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
import core.ms.shared.events.EventContext;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class OrderSagaService {

    private static final Logger logger = LoggerFactory.getLogger(OrderSagaService.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long LOCK_TIMEOUT_SECONDS = 5;

    // Order-level locks for match processing
    private final Map<String, ReentrantLock> orderLocks = new ConcurrentHashMap<>();

    // Idempotency tracking for order creation
    private final Map<String, String> reservationToOrderMap = new ConcurrentHashMap<>();

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EventBus eventBus;

    /**
     * Processes order request with idempotency and thread safety.
     * Uses REQUIRES_NEW to isolate from calling transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED,
            timeout = 10)
    @Retryable(
            value = {OptimisticLockingFailureException.class},
            maxAttempts = MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public void processOrderRequest(OrderRequestedEvent event) {
        String correlationId = event.getCorrelationId();
        String reservationId = event.getReservationId();

        logger.info("🚀 [SAGA: {}] BEGIN processOrderRequest - Reservation: {}",
                correlationId, reservationId);

        try {
            // Propagate correlation for saga tracking
            EventContext.setCorrelationId(correlationId);

            // Check idempotency - prevent duplicate order creation
            String existingOrderId = reservationToOrderMap.get(reservationId);
            if (existingOrderId != null) {
                logger.warn("⚠️ [SAGA: {}] Duplicate order request for reservation: {}, existing order: {}",
                        correlationId, reservationId, existingOrderId);

                // Check if order actually exists in DB
                Optional<IOrder> existingOrder = orderRepository.findById(existingOrderId);
                if (existingOrder.isPresent()) {
                    logger.info("✅ [SAGA: {}] Order already exists, republishing success event",
                            correlationId);
                    publishOrderCreated(correlationId, existingOrder.get(), event.getOrderType());
                    return;
                } else {
                    // Stale cache entry, remove it
                    reservationToOrderMap.remove(reservationId);
                }
            }

            // Validate input
            validateOrderRequest(event);

            // Create domain objects
            Symbol symbol = Symbol.createFromCode(event.getSymbolCode());
            Money price = Money.of(event.getPrice(), event.getCurrency());

            // Create order with factory (includes validation)
            IOrder order = createOrder(event, symbol, price);

            logger.info("📝 Order created in memory - ID: {}, Type: {}",
                    order.getId(), order.getClass().getSimpleName());

            // Save order with immediate flush
            IOrder savedOrder = orderRepository.save(order);
            orderRepository.flush(); // Force immediate DB write

            // Track idempotency after successful save
            reservationToOrderMap.put(reservationId, savedOrder.getId());

            logger.info("💾 Order persisted - ID: {}, Reservation: {}",
                    savedOrder.getId(), reservationId);

            // Publish success event
            publishOrderCreated(correlationId, savedOrder, event.getOrderType());

            logger.info("🎉 [SAGA: {}] COMPLETED processOrderRequest - Order: {}",
                    correlationId, savedOrder.getId());

        } catch (Exception e) {
            logger.error("💥 [SAGA: {}] FAILED processOrderRequest - Error: {}",
                    correlationId, e.getMessage(), e);

            // Publish failure event for compensation
            publishOrderCreationFailed(correlationId, reservationId,
                    event.getPortfolioId(), e.getMessage());

            throw new OrderCreationException(
                    "Failed to create order for reservation: " + reservationId, e);
        } finally {
            EventContext.clear();
        }
    }

    /**
     * Processes order match with pessimistic locking to prevent race conditions.
     * Critical section for transaction creation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.SERIALIZABLE,
            timeout = 15)
    @Retryable(
            value = {OptimisticLockingFailureException.class},
            maxAttempts = MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(delay = 200, multiplier = 2)
    )
    public void processOrderMatch(OrderMatchedEvent event) {
        String correlationId = event.getCorrelationId();
        String buyOrderId = event.getBuyOrderId();
        String sellOrderId = event.getSellOrderId();

        logger.info("🚀 [SAGA: {}] BEGIN processOrderMatch - Buy: {}, Sell: {}",
                correlationId, buyOrderId, sellOrderId);

        // Acquire locks in consistent order to prevent deadlock
        String firstLockId = buyOrderId.compareTo(sellOrderId) < 0 ? buyOrderId : sellOrderId;
        String secondLockId = firstLockId.equals(buyOrderId) ? sellOrderId : buyOrderId;

        Lock firstLock = getOrderLock(firstLockId);
        Lock secondLock = getOrderLock(secondLockId);

        try {
            // Try to acquire both locks with timeout
            if (!firstLock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new LockAcquisitionException("Failed to acquire lock for order: " + firstLockId);
            }

            try {
                if (!secondLock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    throw new LockAcquisitionException("Failed to acquire lock for order: " + secondLockId);
                }

                try {
                    logger.info("🔒 Acquired locks for both orders");

                    // Propagate correlation
                    EventContext.setCorrelationId(correlationId);

                    // Process the match with locks held
                    processMatchWithLocks(event);

                } finally {
                    secondLock.unlock();
                }
            } finally {
                firstLock.unlock();
            }

            logger.info("🔓 Released locks for both orders");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("⚠️ Interrupted while acquiring locks", e);
            throw new TransactionCreationException(
                    "Interrupted during order match processing", e);
        } catch (Exception e) {
            logger.error("💥 [SAGA: {}] FAILED processOrderMatch", correlationId, e);

            // Publish failure event
            publishTransactionCreationFailed(correlationId, buyOrderId, sellOrderId, e.getMessage());

            throw new TransactionCreationException(
                    "Failed to process order match", e);
        } finally {
            EventContext.clear();
        }
    }

    /**
     * Internal method to process match with locks already acquired.
     */
    private void processMatchWithLocks(OrderMatchedEvent event) {
        String buyOrderId = event.getBuyOrderId();
        String sellOrderId = event.getSellOrderId();

        // Fetch orders with pessimistic write locks
        logger.info("🔍 Fetching orders with pessimistic locks");

        IBuyOrder buyOrder = orderRepository.findByIdWithLock(buyOrderId, LockModeType.PESSIMISTIC_WRITE)
                .filter(o -> o instanceof IBuyOrder)
                .map(o -> (IBuyOrder) o)
                .orElseThrow(() -> new IllegalStateException("Buy order not found: " + buyOrderId));

        ISellOrder sellOrder = orderRepository.findByIdWithLock(sellOrderId, LockModeType.PESSIMISTIC_WRITE)
                .filter(o -> o instanceof ISellOrder)
                .map(o -> (ISellOrder) o)
                .orElseThrow(() -> new IllegalStateException("Sell order not found: " + sellOrderId));

        logger.info("📊 Order states before transaction:");
        logger.info("  Buy - Status: {}, Remaining: {}, Executed: {}",
                buyOrder.getStatus().getStatus(),
                buyOrder.getRemainingQuantity(),
                buyOrder.getExecutedQuantity());
        logger.info("  Sell - Status: {}, Remaining: {}, Executed: {}",
                sellOrder.getStatus().getStatus(),
                sellOrder.getRemainingQuantity(),
                sellOrder.getExecutedQuantity());

        // CHANGE: Check remaining quantity instead of active status
        BigDecimal buyRemaining = buyOrder.getRemainingQuantity();
        BigDecimal sellRemaining = sellOrder.getRemainingQuantity();

        if (buyRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Buy order {} has no remaining quantity", buyOrderId);
            return; // Skip this match
        }
        if (sellRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Sell order {} has no remaining quantity", sellOrderId);
            return; // Skip this match
        }

        // Calculate actual match quantity
        BigDecimal matchQuantity = event.getMatchedQuantity();
        BigDecimal actualQuantity = matchQuantity.min(buyRemaining).min(sellRemaining);

        if (actualQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("No quantity available for matching");
            return; // Skip this match
        }

        // Store portfolio/reservation info before transaction
        String buyPortfolioId = buyOrder.getPortfolioId();
        String sellPortfolioId = sellOrder.getPortfolioId();
        String buyReservationId = buyOrder.getReservationId();
        String sellReservationId = sellOrder.getReservationId();

        // Create transaction (updates order states)
        logger.info("🏗️ Creating transaction for quantity: {}", actualQuantity);
        Transaction transaction = TransactionFactory.create(buyOrder, sellOrder, actualQuantity);

        // Save transaction
        ITransaction savedTransaction = transactionRepository.save(transaction);
        transactionRepository.flush();

        logger.info("💾 Transaction saved - ID: {}", savedTransaction.getId());

        // Save updated orders
        orderRepository.save(buyOrder);
        orderRepository.save(sellOrder);
        orderRepository.flush();

        logger.info("📊 Order states after transaction:");
        logger.info("  Buy - Status: {}, Remaining: {}, Executed: {}",
                buyOrder.getStatus().getStatus(),
                buyOrder.getRemainingQuantity(),
                buyOrder.getExecutedQuantity());
        logger.info("  Sell - Status: {}, Remaining: {}, Executed: {}",
                sellOrder.getStatus().getStatus(),
                sellOrder.getRemainingQuantity(),
                sellOrder.getExecutedQuantity());

        // Publish success event
        publishTransactionCreated(
                event.getCorrelationId(),
                savedTransaction,
                buyPortfolioId,
                sellPortfolioId,
                buyReservationId,
                sellReservationId,
                buyOrder.getRemainingQuantity(),
                sellOrder.getRemainingQuantity()
        );

        logger.info("🎉 Transaction completed - ID: {}", savedTransaction.getId());
    }

    /**
     * Helper method to create order based on type.
     */
    private IOrder createOrder(OrderRequestedEvent event, Symbol symbol, Money price) {
        if ("BUY".equalsIgnoreCase(event.getOrderType())) {
            return OrderFactory.createBuyOrder(
                    event.getPortfolioId(),
                    event.getReservationId(),
                    symbol,
                    price,
                    event.getQuantity()
            );
        } else if ("SELL".equalsIgnoreCase(event.getOrderType())) {
            return OrderFactory.createSellOrder(
                    event.getPortfolioId(),
                    event.getReservationId(),
                    symbol,
                    price,
                    event.getQuantity()
            );
        } else {
            throw new IllegalArgumentException("Invalid order type: " + event.getOrderType());
        }
    }

    /**
     * Validates order request parameters.
     */
    private void validateOrderRequest(OrderRequestedEvent event) {
        if (event.getPortfolioId() == null || event.getPortfolioId().trim().isEmpty()) {
            throw new IllegalArgumentException("Portfolio ID is required");
        }
        if (event.getReservationId() == null || event.getReservationId().trim().isEmpty()) {
            throw new IllegalArgumentException("Reservation ID is required");
        }
        if (event.getPrice() == null || event.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        if (event.getQuantity() == null || event.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }

    /**
     * Gets or creates a lock for an order.
     */
    private Lock getOrderLock(String orderId) {
        return orderLocks.computeIfAbsent(orderId, k -> new ReentrantLock(true));
    }

    /**
     * Publishes OrderCreatedEvent.
     */
    private void publishOrderCreated(String correlationId, IOrder order, String orderType) {
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

        eventBus.publish(event);
        logger.info("📤 Published OrderCreatedEvent - Order: {}, Type: {}",
                order.getId(), orderType);
    }

    /**
     * Publishes OrderCreationFailedEvent.
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
        logger.info("📤 Published OrderCreationFailedEvent - Reservation: {}", reservationId);
    }

    /**
     * Publishes TransactionCreatedEvent.
     */
    private void publishTransactionCreated(String correlationId, ITransaction transaction,
                                           String buyerPortfolioId, String sellerPortfolioId,
                                           String buyerReservationId, String sellerReservationId,
                                           BigDecimal buyOrderRemaining, BigDecimal sellOrderRemaining) {
        logger.info("DEBOUG publishTransactionCreated - Symbol: {}, Quantity: {}, Price: {}",
                transaction.getSymbol().getCode(),
                transaction.getQuantity(),
                transaction.getPrice().toDisplayString());

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
        logger.info("📤 Published TransactionCreatedEvent - Transaction: {}", transaction.getId());
    }

    /**
     * Publishes TransactionCreationFailedEvent.
     */
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
        logger.info("📤 Published TransactionCreationFailedEvent");
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

    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }
    }
}