package core.ms.order_book.application.event_handlers;

import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.ports.inbound.OrderService;
import core.ms.order_book.application.services.OrderBookApplicationService;
import core.ms.shared.events.CorrelationAwareEventListener;
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
    @EventListener
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
    }

    /**
     * Handles TransactionCreatedEvent from Order BC.
     * Updates or removes orders based on execution status.
     */
    @EventListener
    @Async
    @Transactional
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
    private void updateOrderInBook(String orderId, core.ms.shared.money.Symbol symbol, String correlationId) {
        logger.info("🔄 ORDERBOOK BC: Updating order {} in book", orderId);

        try {
            // Fetch fresh order from service
            logger.info("   - Fetching updated order from Order Service");

            IOrder order = orderService.findOrderById(orderId)
                    .orElseThrow(() -> {
                        logger.error("   ❌ Order {} not found for update", orderId);
                        return new IllegalStateException("Order not found for update: " + orderId);
                    });

            logger.info("   - Order fetched successfully");
            logger.info("   - Is Active: {}", order.isActive());
            logger.info("   - Remaining Quantity: {}", order.getRemainingQuantity());

            // Remove old version
            logger.info("   - Removing old version from book");
            orderBookService.removeOrderFromBook(orderId, symbol);

            // Add updated version if still active
            if (order.isActive() && order.getRemainingQuantity().signum() > 0) {
                logger.info("   - Adding updated version to book");

                var result = orderBookService.addOrderToBook(order);

                if (result.isSuccess()) {
                    logger.info("   ✅ Order {} successfully updated in book", orderId);
                } else {
                    logger.warn("   ⚠️ Failed to re-add order {} after update: {}",
                            orderId, result.getMessage());
                }
            } else {
                logger.info("   - Order {} is no longer active/has no remaining quantity - not re-adding",
                        orderId);
            }

        } catch (Exception e) {
            logger.error("💥 Failed to update order {} in book", orderId, e);
        }
    }
}