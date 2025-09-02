package core.ms.order.application.services;

import core.ms.order.domain.entities.*;
import core.ms.order.domain.events.publish.OrderCreatedEvent;
import core.ms.order.domain.events.publish.OrderCreationFailedEvent;
import core.ms.order.domain.events.publish.TransactionCreatedEvent;
import core.ms.order.domain.events.publish.TransactionCreationFailedEvent;
import core.ms.order.domain.events.subscribe.OrderMatchedEvent;
import core.ms.order.domain.events.subscribe.OrderRequestedEvent;
import core.ms.order.domain.factories.OrderFactory;
import core.ms.order.domain.factories.TransactionFactory;
import core.ms.order.domain.ports.outbound.OrderRepository;
import core.ms.order.domain.ports.outbound.TransactionRepository;
import core.ms.shared.events.EventBus;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class OrderSagaService {

    private static final Logger logger = LoggerFactory.getLogger(OrderSagaService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EventBus eventBus;

    /**
     * Processes an order request from Portfolio BC.
     * Creates an order with reservation reference and publishes events.
     */
    public void processOrderRequest(OrderRequestedEvent event) {
        try {
            logger.info("[SAGA: {}] Processing order request - Type: {}, Symbol: {}, Reservation: {}",
                    event.getCorrelationId(), event.getOrderType(), event.getSymbolCode(), event.getReservationId());

            // Create domain objects
            Symbol symbol = Symbol.createFromCode(event.getSymbolCode());
            Money price = Money.of(event.getPrice(), event.getCurrency());

            // Create order based on type with reservation reference
            IOrder order;
            if ("BUY".equalsIgnoreCase(event.getOrderType())) {
                order = OrderFactory.createBuyOrder(
                        event.getPortfolioId(),
                        event.getReservationId(),
                        symbol,
                        price,
                        event.getQuantity()
                );
            } else if ("SELL".equalsIgnoreCase(event.getOrderType())) {
                order = OrderFactory.createSellOrder(
                        event.getPortfolioId(),
                        event.getReservationId(),
                        symbol,
                        price,
                        event.getQuantity()
                );
            } else {
                throw new IllegalArgumentException("Invalid order type: " + event.getOrderType());
            }

            // Save order
            IOrder savedOrder = orderRepository.save(order);

            logger.info("[SAGA: {}] Order created successfully - Order ID: {}, Reservation: {}",
                    event.getCorrelationId(), savedOrder.getId(), savedOrder.getReservationId());

            // Publish OrderCreatedEvent (to Portfolio BC and OrderBook BC)
            publishOrderCreated(event.getCorrelationId(), savedOrder, event.getOrderType());

        } catch (Exception e) {
            logger.error("[SAGA: {}] Failed to create order - Error: {}",
                    event.getCorrelationId(), e.getMessage(), e);
            throw new OrderCreationException("Failed to create order: " + e.getMessage(), e);
        }
    }

    /**
     * Processes an order match from OrderBook BC.
     * Creates a transaction and updates both orders.
     */
    public void processOrderMatch(OrderMatchedEvent event) {
        try {
            logger.info("[SAGA: {}] Processing order match - Buy: {}, Sell: {}, Quantity: {}",
                    event.getCorrelationId(), event.getBuyOrderId(), event.getSellOrderId(), event.getMatchedQuantity());

            // Fetch orders
            Optional<IOrder> buyOrderOpt = orderRepository.findById(event.getBuyOrderId());
            Optional<IOrder> sellOrderOpt = orderRepository.findById(event.getSellOrderId());

            if (buyOrderOpt.isEmpty()) {
                throw new IllegalStateException("Buy order not found: " + event.getBuyOrderId());
            }
            if (sellOrderOpt.isEmpty()) {
                throw new IllegalStateException("Sell order not found: " + event.getSellOrderId());
            }

            // Validate order types
            if (!(buyOrderOpt.get() instanceof IBuyOrder)) {
                throw new IllegalStateException("Invalid buy order type: " + event.getBuyOrderId());
            }
            if (!(sellOrderOpt.get() instanceof ISellOrder)) {
                throw new IllegalStateException("Invalid sell order type: " + event.getSellOrderId());
            }

            IBuyOrder buyOrder = (IBuyOrder) buyOrderOpt.get();
            ISellOrder sellOrder = (ISellOrder) sellOrderOpt.get();

            // Store pre-transaction state for event publishing
            String buyOrderPortfolioId = buyOrder.getPortfolioId();
            String sellOrderPortfolioId = sellOrder.getPortfolioId();
            String buyOrderReservationId = buyOrder.getReservationId();
            String sellOrderReservationId = sellOrder.getReservationId();

            // Create transaction (this updates order execution quantities)
            Transaction transaction = TransactionFactory.create(buyOrder, sellOrder, event.getMatchedQuantity());

            // Save transaction
            ITransaction savedTransaction = transactionRepository.save(transaction);

            // Save updated orders
            orderRepository.save(buyOrder);
            orderRepository.save(sellOrder);

            logger.info("[SAGA: {}] Transaction created successfully - Transaction ID: {}, Buy remaining: {}, Sell remaining: {}",
                    event.getCorrelationId(), savedTransaction.getId(),
                    buyOrder.getRemainingQuantity(), sellOrder.getRemainingQuantity());

            // Publish TransactionCreatedEvent (to Portfolio BC and OrderBook BC)
            publishTransactionCreated(
                    event.getCorrelationId(),
                    savedTransaction,
                    buyOrderPortfolioId,
                    sellOrderPortfolioId,
                    buyOrderReservationId,
                    sellOrderReservationId,
                    buyOrder.getRemainingQuantity(),
                    sellOrder.getRemainingQuantity()
            );

        } catch (Exception e) {
            logger.error("[SAGA: {}] Failed to create transaction - Error: {}",
                    event.getCorrelationId(), e.getMessage(), e);
            throw new TransactionCreationException("Failed to create transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Publishes OrderCreatedEvent to Portfolio BC and OrderBook BC.
     */
    private void publishOrderCreated(String correlationId, IOrder order, String orderType) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                correlationId,
                order.getId(),
                order.getPortfolioId(),
                order.getReservationId(),
                orderType,
                order.getSymbol().getCode(),
                order.getPrice().getAmount(),
                order.getPrice().getCurrency(),
                order.getQuantity(),
                order.getStatus().getStatus().name()
        );

        eventBus.publish(event);

        logger.info("📤 [SAGA: {}] Published OrderCreatedEvent - Order ID: {}, Type: {}, Reservation: {}",
                correlationId, order.getId(), orderType, order.getReservationId());
    }

    /**
     * Publishes OrderCreationFailedEvent to Portfolio BC.
     */
    public void publishOrderCreationFailed(String correlationId, String reservationId,
                                           String portfolioId, String reason) {
        OrderCreationFailedEvent event = new OrderCreationFailedEvent(
                correlationId,
                reservationId,
                portfolioId,
                "ORDER_CREATION_FAILED",
                reason
        );

        eventBus.publish(event);

        logger.info("📤 [SAGA: {}] Published OrderCreationFailedEvent - Reservation: {}, Reason: {}",
                correlationId, reservationId, reason);
    }

    /**
     * Publishes TransactionCreatedEvent to Portfolio BC and OrderBook BC.
     */
    private void publishTransactionCreated(String correlationId, ITransaction transaction,
                                           String buyerPortfolioId, String sellerPortfolioId,
                                           String buyerReservationId, String sellerReservationId,
                                           java.math.BigDecimal buyOrderRemaining,
                                           java.math.BigDecimal sellOrderRemaining) {
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                correlationId,
                transaction.getId(),
                transaction.getBuyOrder().getId(),
                transaction.getSellOrder().getId(),
                buyerPortfolioId,
                sellerPortfolioId,
                buyerReservationId,
                sellerReservationId,
                transaction.getSymbol().getCode(),
                transaction.getQuantity(),
                transaction.getPrice().getAmount(),
                transaction.getPrice().getCurrency(),
                buyOrderRemaining,
                sellOrderRemaining
        );

        eventBus.publish(event);

        logger.info("📤 [SAGA: {}] Published TransactionCreatedEvent - Transaction ID: {}, Buy Order: {}, Sell Order: {}",
                correlationId, transaction.getId(), transaction.getBuyOrder().getId(), transaction.getSellOrder().getId());
    }
    public void publishTransactionCreationFailed(String correlationId, String buyOrderId,
                                                 String sellOrderId, String reason) {
        TransactionCreationFailedEvent event = new TransactionCreationFailedEvent(
                correlationId,
                buyOrderId,
                sellOrderId,
                "TRANSACTION_CREATION_FAILED",
                reason
        );

        eventBus.publish(event);

        logger.info("📤 [SAGA: {}] Published TransactionCreationFailedEvent - Buy: {}, Sell: {}, Reason: {}",
                correlationId, buyOrderId, sellOrderId, reason);
    }


    // Exception classes
    public static class OrderCreationException extends RuntimeException {
        public OrderCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class TransactionCreationException extends RuntimeException {
        public TransactionCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}