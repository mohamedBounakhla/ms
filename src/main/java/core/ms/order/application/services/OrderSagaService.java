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
import core.ms.shared.OrderType;
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
        logger.info("🚀 [SAGA: {}] BEGIN processOrderRequest", event.getCorrelationId());
        logger.info("📋 Processing details - Type: {}, Symbol: {}, Portfolio: {}, Reservation: {}",
                event.getOrderType(), event.getSymbolCode(),
                event.getPortfolioId(), event.getReservationId());

        try {
            // Validate input
            logger.info("🔍 Validating input parameters");
            if (event.getPortfolioId() == null || event.getPortfolioId().trim().isEmpty()) {
                throw new IllegalArgumentException("Portfolio ID is required");
            }
            if (event.getReservationId() == null || event.getReservationId().trim().isEmpty()) {
                throw new IllegalArgumentException("Reservation ID is required");
            }

            // Create domain objects
            logger.info("🏗️ Creating domain objects - Symbol: {}, Price: {} {}",
                    event.getSymbolCode(), event.getPrice(), event.getCurrency());

            Symbol symbol = Symbol.createFromCode(event.getSymbolCode());
            Money price = Money.of(event.getPrice(), event.getCurrency());

            logger.info("✅ Domain objects created successfully");

            // Create order based on type with reservation reference
            IOrder order;
            if ("BUY".equalsIgnoreCase(event.getOrderType())) {
                logger.info("💰 Creating BUY order with factory");
                order = OrderFactory.createBuyOrder(
                        event.getPortfolioId(),
                        event.getReservationId(),
                        symbol,
                        price,
                        event.getQuantity()
                );
                logger.info("✅ BUY order created - ID: {}", order.getId());

            } else if ("SELL".equalsIgnoreCase(event.getOrderType())) {
                logger.info("💸 Creating SELL order with factory");
                order = OrderFactory.createSellOrder(
                        event.getPortfolioId(),
                        event.getReservationId(),
                        symbol,
                        price,
                        event.getQuantity()
                );
                logger.info("✅ SELL order created - ID: {}", order.getId());

            } else {
                throw new IllegalArgumentException("Invalid order type: " + event.getOrderType());
            }

            // Log order details before saving
            logger.info("📝 Order details before save:");
            logger.info("  - ID: {}", order.getId());
            logger.info("  - Portfolio ID: {}", order.getPortfolioId());
            logger.info("  - Reservation ID: {}", order.getReservationId());
            logger.info("  - Symbol: {}", order.getSymbol().getCode());
            logger.info("  - Price: {}", order.getPrice().toDisplayString());
            logger.info("  - Quantity: {}", order.getQuantity());
            logger.info("  - Status: {}", order.getStatus().getStatus());

            // Save order
            logger.info("💾 Saving order to repository");
            IOrder savedOrder = orderRepository.save(order);
            logger.info("✅ Order saved successfully - ID: {}, Type: {}",
                    savedOrder.getId(), savedOrder.getClass().getSimpleName());

            // Verify save
            logger.info("🔍 Verifying order was saved");
            Optional<IOrder> verification = orderRepository.findById(savedOrder.getId());
            if (verification.isPresent()) {
                logger.info("✅ Order verified in repository - ID: {}", verification.get().getId());
            } else {
                logger.error("❌ Order NOT found in repository after save!");
                throw new IllegalStateException("Order save verification failed");
            }

            // Publish OrderCreatedEvent
            logger.info("📤 Publishing OrderCreatedEvent");
            publishOrderCreated(event.getCorrelationId(), savedOrder, event.getOrderType());

            logger.info("🎉 [SAGA: {}] COMPLETED processOrderRequest - Order ID: {}",
                    event.getCorrelationId(), savedOrder.getId());

        } catch (Exception e) {
            logger.error("💥 [SAGA: {}] FAILED processOrderRequest - Error: {}",
                    event.getCorrelationId(), e.getMessage(), e);
            throw new OrderCreationException("Failed to create order: " + e.getMessage(), e);
        }
    }

    /**
     * Processes an order match from OrderBook BC.
     * Creates a transaction and updates both orders.
     */
    public void processOrderMatch(OrderMatchedEvent event) {
        logger.info("🚀 [SAGA: {}] BEGIN processOrderMatch", event.getCorrelationId());
        logger.info("📋 Match details - Buy: {}, Sell: {}, Quantity: {}",
                event.getBuyOrderId(), event.getSellOrderId(), event.getMatchedQuantity());

        try {
            // Fetch orders
            logger.info("🔍 Fetching buy order: {}", event.getBuyOrderId());
            Optional<IOrder> buyOrderOpt = orderRepository.findById(event.getBuyOrderId());

            logger.info("🔍 Fetching sell order: {}", event.getSellOrderId());
            Optional<IOrder> sellOrderOpt = orderRepository.findById(event.getSellOrderId());

            if (buyOrderOpt.isEmpty()) {
                logger.error("❌ Buy order not found: {}", event.getBuyOrderId());
                throw new IllegalStateException("Buy order not found: " + event.getBuyOrderId());
            }
            if (sellOrderOpt.isEmpty()) {
                logger.error("❌ Sell order not found: {}", event.getSellOrderId());
                throw new IllegalStateException("Sell order not found: " + event.getSellOrderId());
            }

            logger.info("✅ Both orders found");

            // Validate order types
            if (!(buyOrderOpt.get() instanceof IBuyOrder)) {
                logger.error("❌ Invalid buy order type: {}", buyOrderOpt.get().getClass());
                throw new IllegalStateException("Invalid buy order type: " + event.getBuyOrderId());
            }
            if (!(sellOrderOpt.get() instanceof ISellOrder)) {
                logger.error("❌ Invalid sell order type: {}", sellOrderOpt.get().getClass());
                throw new IllegalStateException("Invalid sell order type: " + event.getSellOrderId());
            }

            IBuyOrder buyOrder = (IBuyOrder) buyOrderOpt.get();
            ISellOrder sellOrder = (ISellOrder) sellOrderOpt.get();

            logger.info("📝 Order states before transaction:");
            logger.info("  Buy - Remaining: {}, Executed: {}",
                    buyOrder.getRemainingQuantity(), buyOrder.getExecutedQuantity());
            logger.info("  Sell - Remaining: {}, Executed: {}",
                    sellOrder.getRemainingQuantity(), sellOrder.getExecutedQuantity());

            // Store pre-transaction state
            String buyOrderPortfolioId = buyOrder.getPortfolioId();
            String sellOrderPortfolioId = sellOrder.getPortfolioId();
            String buyOrderReservationId = buyOrder.getReservationId();
            String sellOrderReservationId = sellOrder.getReservationId();

            // Create transaction
            logger.info("🏗️ Creating transaction with factory");
            Transaction transaction = TransactionFactory.create(buyOrder, sellOrder, event.getMatchedQuantity());
            logger.info("✅ Transaction created - ID: {}", transaction.getId());

            // Save transaction
            logger.info("💾 Saving transaction");
            ITransaction savedTransaction = transactionRepository.save(transaction);
            logger.info("✅ Transaction saved - ID: {}", savedTransaction.getId());

            // Save updated orders
            logger.info("💾 Saving updated orders");
            orderRepository.save(buyOrder);
            orderRepository.save(sellOrder);

            logger.info("📝 Order states after transaction:");
            logger.info("  Buy - Remaining: {}, Executed: {}",
                    buyOrder.getRemainingQuantity(), buyOrder.getExecutedQuantity());
            logger.info("  Sell - Remaining: {}, Executed: {}",
                    sellOrder.getRemainingQuantity(), sellOrder.getExecutedQuantity());

            // Publish TransactionCreatedEvent
            logger.info("📤 Publishing TransactionCreatedEvent");
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

            logger.info("🎉 [SAGA: {}] COMPLETED processOrderMatch - Transaction ID: {}",
                    event.getCorrelationId(), savedTransaction.getId());

        } catch (Exception e) {
            logger.error("💥 [SAGA: {}] FAILED processOrderMatch - Error: {}",
                    event.getCorrelationId(), e.getMessage(), e);
            throw new TransactionCreationException("Failed to create transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Publishes OrderCreatedEvent to Portfolio BC and OrderBook BC.
     */
    private void publishOrderCreated(String correlationId, IOrder order, String orderType) {
        logger.info("📤 Creating OrderCreatedEvent for order: {}", order.getId());

        OrderCreatedEvent event = new OrderCreatedEvent(
                correlationId,
                order.getId(),
                order.getPortfolioId(),
                order.getReservationId(),
                order.getSymbol(),
                order.getPrice(),
                order.getQuantity(),
                "BUY".equalsIgnoreCase(orderType) ? OrderType.BUY : OrderType.SELL,
                order.getStatus().getStatus().name()
        );

        logger.info("📨 Publishing to event bus - Correlation: {}", correlationId);
        eventBus.publish(event);
        logger.info("✅ OrderCreatedEvent published - Order: {}, Type: {}, Reservation: {}",
                order.getId(), orderType, order.getReservationId());
    }

    /**
     * Publishes OrderCreationFailedEvent to Portfolio BC.
     */
    public void publishOrderCreationFailed(String correlationId, String reservationId,
                                           String portfolioId, String reason) {
        logger.info("📤 Creating OrderCreationFailedEvent - Reservation: {}", reservationId);

        OrderCreationFailedEvent event = new OrderCreationFailedEvent(
                correlationId,
                reservationId,
                portfolioId,
                "ORDER_CREATION_FAILED",
                reason
        );

        logger.info("📨 Publishing failure event to event bus");
        eventBus.publish(event);
        logger.info("✅ OrderCreationFailedEvent published - Reservation: {}, Reason: {}",
                reservationId, reason);
    }

    /**
     * Publishes TransactionCreatedEvent to Portfolio BC and OrderBook BC.
     */
    private void publishTransactionCreated(String correlationId, ITransaction transaction,
                                           String buyerPortfolioId, String sellerPortfolioId,
                                           String buyerReservationId, String sellerReservationId,
                                           java.math.BigDecimal buyOrderRemaining,
                                           java.math.BigDecimal sellOrderRemaining) {
        logger.info("📤 Creating TransactionCreatedEvent - Transaction: {}", transaction.getId());

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

        logger.info("📨 Publishing to event bus - Correlation: {}", correlationId);
        eventBus.publish(event);
        logger.info("✅ TransactionCreatedEvent published - Transaction: {}, Buy: {}, Sell: {}",
                transaction.getId(), transaction.getBuyOrder().getId(), transaction.getSellOrder().getId());
    }

    /**
     * Publishes TransactionCreationFailedEvent.
     */
    public void publishTransactionCreationFailed(String correlationId, String buyOrderId,
                                                 String sellOrderId, String reason) {
        logger.info("📤 Creating TransactionCreationFailedEvent - Buy: {}, Sell: {}",
                buyOrderId, sellOrderId);

        TransactionCreationFailedEvent event = new TransactionCreationFailedEvent(
                correlationId,
                buyOrderId,
                sellOrderId,
                "TRANSACTION_CREATION_FAILED",
                reason
        );

        logger.info("📨 Publishing failure event to event bus");
        eventBus.publish(event);
        logger.info("✅ TransactionCreationFailedEvent published - Reason: {}", reason);
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