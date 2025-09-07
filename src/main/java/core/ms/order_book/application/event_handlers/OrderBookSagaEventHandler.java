package core.ms.order_book.application.event_handlers;

import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.ports.inbound.OrderService;
import core.ms.order_book.application.services.OrderBookApplicationService;
import core.ms.shared.events.CorrelationAwareEventListener;
import core.ms.shared.money.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class OrderBookSagaEventHandler extends CorrelationAwareEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderBookSagaEventHandler.class);

    @Autowired
    private OrderBookApplicationService orderBookService;

    @Autowired
    private OrderService orderService;

    /**
     * Handles OrderCreatedEvent DIRECTLY from Order BC.
     * No mapping needed - listen to the actual event from Order BC.
     */
    /*@EventListener
    @Async
    @Transactional
    public void handleOrderCreatedFromOrderBC(core.ms.order.domain.events.publish.OrderCreatedEvent event) {

        // LOG ENTRY POINT
        logger.info("════════════════════════════════════════════════════════════════");
        logger.info("📥 ORDERBOOK BC: RECEIVED OrderCreatedEvent from ORDER BC");
        logger.info("📋 Event Details:");
        logger.info("   - Correlation ID: {}", event.getCorrelationId());
        logger.info("   - Order ID: {}", event.getOrderId());
        logger.info("   - Portfolio ID: {}", event.getPortfolioId());
        logger.info("   - Reservation ID: {}", event.getReservationId());
        logger.info("   - Symbol: {}", event.getSymbol().getCode());
        logger.info("   - Order Type: {}", event.getOrderType());
        logger.info("   - Price: {} {}", event.getPrice().getAmount(), event.getPrice().getCurrency());
        logger.info("   - Quantity: {}", event.getQuantity());
        logger.info("   - Status: {}", event.getStatus());
        logger.info("   - Source BC: {}", event.getSourceBC());
        logger.info("════════════════════════════════════════════════════════════════");

        handleEvent(event, () -> {
            try {
                // STEP 1: FETCH ORDER WITH RETRY (handle race condition)
                logger.info("🔍 ORDERBOOK BC: Fetching order {} from Order Service", event.getOrderId());

                IOrder order = null;
                int retryCount = 0;
                int maxRetries = 3;

                while (order == null && retryCount < maxRetries) {
                    Optional<IOrder> orderOpt = orderService.findOrderById(event.getOrderId());

                    if (orderOpt.isPresent()) {
                        order = orderOpt.get();
                        logger.info("✅ ORDERBOOK BC: Order {} found on attempt {}",
                                event.getOrderId(), retryCount + 1);
                    } else {
                        retryCount++;
                        if (retryCount < maxRetries) {
                            logger.warn("⚠️ ORDERBOOK BC: Order {} not found, retrying in 100ms (attempt {}/{})",
                                    event.getOrderId(), retryCount, maxRetries);
                            try {
                                Thread.sleep(100); // Wait 100ms before retry
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }

                if (order == null) {
                    logger.error("❌ ORDERBOOK BC: Order {} NOT FOUND after {} retries!",
                            event.getOrderId(), maxRetries);
                    throw new IllegalStateException("Order not found after retries: " + event.getOrderId());
                }

                logger.info("✅ ORDERBOOK BC: Order {} successfully fetched", event.getOrderId());
                logger.info("   - Order Class: {}", order.getClass().getSimpleName());
                logger.info("   - Is Active: {}", order.isActive());
                logger.info("   - Remaining Quantity: {}", order.getRemainingQuantity());

                // STEP 2: ADD TO ORDER BOOK
                logger.info("📚 ORDERBOOK BC: Adding order {} to order book", event.getOrderId());

                var result = orderBookService.addOrderToBook(order);

                if (result.isSuccess()) {
                    logger.info("✅ ORDERBOOK BC: Order {} successfully added to book", event.getOrderId());
                    logger.info("   - Result Message: {}", result.getMessage());

                    // LOG MATCH CHECK
                    logger.info("🔄 ORDERBOOK BC: Order book will now check for matches...");
                    logger.info("   Note: Matches are processed internally in OrderBook.addOrder()");

                } else {
                    logger.error("❌ ORDERBOOK BC: Failed to add order {} to book", event.getOrderId());
                    logger.error("   - Failure Reason: {}", result.getMessage());
                }

            } catch (Exception e) {
                logger.error("💥 ORDERBOOK BC: EXCEPTION processing OrderCreatedEvent", e);
                logger.error("   - Order ID: {}", event.getOrderId());
                logger.error("   - Error Type: {}", e.getClass().getSimpleName());
                logger.error("   - Error Message: {}", e.getMessage());
                throw new RuntimeException("Failed to process OrderCreatedEvent", e);
            } finally {
                logger.info("════════════════════════════════════════════════════════════════");
                logger.info("📤 ORDERBOOK BC: Completed processing OrderCreatedEvent for order {}", event.getOrderId());
                logger.info("════════════════════════════════════════════════════════════════");
            }
        });
    }*/
    @EventListener
    @Async
// NO @Transactional annotation - handle everything in memory
    public void handleOrderCreatedFromOrderBC(core.ms.order.domain.events.publish.OrderCreatedEvent event) {
        logger.info("📥 ORDERBOOK BC: RECEIVED OrderCreatedEvent");

        handleEvent(event, () -> {
            // Fetch order with retries
            IOrder order = fetchOrderWithRetries(event.getOrderId(), 3);

            if (order == null) {
                logger.warn("Order {} not found after retries - skipping", event.getOrderId());
                return;
            }

            // Add to in-memory order book (no database transaction)
            try {
                var result = orderBookService.addOrderToBook(order);
                if (result.isSuccess()) {
                    logger.info("✅ Order {} added to book", event.getOrderId());
                } else {
                    logger.warn("Failed to add order: {}", result.getMessage());
                }
            } catch (Exception e) {
                logger.error("Error adding order to book", e);
            }
        });
    }

    private IOrder fetchOrderWithRetries(String orderId, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                Optional<IOrder> orderOpt = orderService.findOrderById(orderId);
                if (orderOpt.isPresent()) {
                    return orderOpt.get();
                }

                if (i < maxRetries - 1) {
                    Thread.sleep(100 * (i + 1));
                }
            } catch (Exception e) {
                logger.debug("Attempt {} failed: {}", i + 1, e.getMessage());
            }
        }
        return null;
    }
    /**
     * Handles TransactionCreatedEvent from Order BC.
     * Updates or removes orders based on execution status.
     */
    @EventListener
    @Async
    public void handleTransactionCreatedFromOrderBC(core.ms.order.domain.events.publish.TransactionCreatedEvent event) {

        // LOG ENTRY POINT
        logger.info("════════════════════════════════════════════════════════════════");
        logger.info("📥 ORDERBOOK BC: RECEIVED TransactionCreatedEvent from ORDER BC");
        logger.info("📋 Event Details:");
        logger.info("   - Correlation ID: {}", event.getCorrelationId());
        logger.info("   - Transaction ID: {}", event.getTransactionId());
        logger.info("   - Buy Order ID: {}", event.getBuyOrderId());
        logger.info("   - Sell Order ID: {}", event.getSellOrderId());
        logger.info("   - Symbol: {}", event.getSymbolCode());
        logger.info("   - Executed Quantity: {}", event.getExecutedQuantity());
        logger.info("   - Execution Price: {} {}", event.getExecutionPrice(), event.getCurrency());
        logger.info("   - Buy Order Remaining: {}", event.getBuyOrderRemainingQuantity());
        logger.info("   - Sell Order Remaining: {}", event.getSellOrderRemainingQuantity());
        logger.info("════════════════════════════════════════════════════════════════");

        handleEvent(event, () -> {
            try {
                // Convert symbol code to Symbol object
                var symbol = core.ms.shared.money.Symbol.createFromCode(event.getSymbolCode());

                // HANDLE BUY ORDER
                logger.info("🔄 ORDERBOOK BC: Processing buy order {}", event.getBuyOrderId());

                if (event.getBuyOrderRemainingQuantity().signum() == 0) {
                    logger.info("🗑️ ORDERBOOK BC: Buy order {} is FULLY EXECUTED - removing from book",
                            event.getBuyOrderId());

                    var removeResult = orderBookService.removeOrderFromBook(
                            event.getBuyOrderId(), symbol);

                    if (removeResult.isSuccess()) {
                        logger.info("✅ ORDERBOOK BC: Buy order {} removed from book", event.getBuyOrderId());
                    } else {
                        logger.warn("⚠️ ORDERBOOK BC: Could not remove buy order {} - {}",
                                event.getBuyOrderId(), removeResult.getMessage());
                    }
                } else {
                    logger.info("📝 ORDERBOOK BC: Buy order {} is PARTIALLY EXECUTED - updating in book",
                            event.getBuyOrderId());
                    logger.info("   - Remaining Quantity: {}", event.getBuyOrderRemainingQuantity());

                    updateOrderInBook(event.getBuyOrderId(), symbol, event.getCorrelationId());
                }

                // HANDLE SELL ORDER
                logger.info("🔄 ORDERBOOK BC: Processing sell order {}", event.getSellOrderId());

                if (event.getSellOrderRemainingQuantity().signum() == 0) {
                    logger.info("🗑️ ORDERBOOK BC: Sell order {} is FULLY EXECUTED - removing from book",
                            event.getSellOrderId());

                    var removeResult = orderBookService.removeOrderFromBook(
                            event.getSellOrderId(), symbol);

                    if (removeResult.isSuccess()) {
                        logger.info("✅ ORDERBOOK BC: Sell order {} removed from book", event.getSellOrderId());
                    } else {
                        logger.warn("⚠️ ORDERBOOK BC: Could not remove sell order {} - {}",
                                event.getSellOrderId(), removeResult.getMessage());
                    }
                } else {
                    logger.info("📝 ORDERBOOK BC: Sell order {} is PARTIALLY EXECUTED - updating in book",
                            event.getSellOrderId());
                    logger.info("   - Remaining Quantity: {}", event.getSellOrderRemainingQuantity());

                    updateOrderInBook(event.getSellOrderId(), symbol, event.getCorrelationId());
                }

                // TRIGGER MATCH CHECK
                logger.info("🔍 ORDERBOOK BC: Triggering match check for symbol {}", symbol.getCode());
                orderBookService.processPendingMatches(symbol);
                logger.info("✅ ORDERBOOK BC: Match check completed");

            } catch (Exception e) {
                logger.error("💥 ORDERBOOK BC: EXCEPTION processing TransactionCreatedEvent", e);
                logger.error("   - Transaction ID: {}", event.getTransactionId());
                logger.error("   - Error Type: {}", e.getClass().getSimpleName());
                logger.error("   - Error Message: {}", e.getMessage());
                throw new RuntimeException("Failed to process TransactionCreatedEvent", e);
            } finally {
                logger.info("════════════════════════════════════════════════════════════════");
                logger.info("📤 ORDERBOOK BC: Completed processing TransactionCreatedEvent {}",
                        event.getTransactionId());
                logger.info("════════════════════════════════════════════════════════════════");
            }
        });
    }

    /**
     * Updates an order in the book by removing and re-adding it.
     */
    private void updateOrderInBook(String orderId, Symbol symbol, String correlationId) {
        logger.info("🔄 ORDERBOOK BC: Updating order {} in book", orderId);

        // Use retries with exponential backoff for lock timeouts
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                // Fetch fresh order from service
                IOrder order = orderService.findOrderById(orderId)
                        .orElse(null);

                if (order == null) {
                    logger.warn("Order {} not found, skipping update", orderId);
                    return;
                }

                // Quick check - should it be in the book?
                if (!order.isActive() || order.getRemainingQuantity().signum() == 0) {
                    logger.info("Order {} is inactive/complete - removing from book", orderId);
                    orderBookService.removeOrderFromBook(orderId, symbol);
                    return;
                }

                // Try to update
                var removeResult = orderBookService.removeOrderFromBook(orderId, symbol);
                if (removeResult.isSuccess() || removeResult.getMessage().contains("not found")) {
                    // Either removed successfully or wasn't there
                    var addResult = orderBookService.addOrderToBook(order);
                    if (addResult.isSuccess()) {
                        logger.info("✅ Order {} successfully updated in book", orderId);
                    }
                }

                return; // Success, exit

            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("busy")) {
                    retryCount++;
                    if (retryCount < maxRetries) {
                        try {
                            Thread.sleep(100 * retryCount); // Exponential backoff
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        continue;
                    }
                }
                logger.error("Failed to update order {} after {} retries", orderId, retryCount, e);
                return;
            }
        }
    }
}