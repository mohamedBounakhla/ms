package core.ms.order_book.application.event_handlers;

import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.ports.inbound.OrderService;
import core.ms.order_book.application.services.OrderBookApplicationService;
import core.ms.order_book.domain.events.subscribe.OrderCreatedEvent;
import core.ms.order_book.domain.events.subscribe.TransactionCreatedEvent;
import core.ms.shared.events.CorrelationAwareEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class OrderBookSagaEventHandler extends CorrelationAwareEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderBookSagaEventHandler.class);

    @Autowired
    private OrderBookApplicationService orderBookService;

    @Autowired
    private OrderService orderService;

    // Cache to reduce service calls
    private final Map<String, CachedOrder> orderCache = new ConcurrentHashMap<>();

    // Cache wrapper class
    private static class CachedOrder {
        final IOrder order;
        final LocalDateTime fetchTime;

        CachedOrder(IOrder order) {
            this.order = order;
            this.fetchTime = LocalDateTime.now();
        }

        boolean isStale() {
            return fetchTime.isBefore(LocalDateTime.now().minusSeconds(10));
        }
    }

    /**
     * Handles OrderCreatedEvent from Order BC.
     * Simply adds the order to the book - matches are handled internally.
     */
    @EventListener
    @Async
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        handleEvent(event, () -> {
            logger.info("📥 [SAGA: {}] OrderCreatedEvent received - Order: {}, Symbol: {}, Type: {}",
                    event.getCorrelationId(), event.getOrderId(),
                    event.getSymbol().getCode(), event.getOrderType());

            try {
                // Fetch order through Order BC's service layer
                IOrder order = orderService.findOrderById(event.getOrderId())
                        .orElseThrow(() -> new IllegalStateException(
                                "Order not found: " + event.getOrderId()));

                // Cache the order for potential future use
                orderCache.put(event.getOrderId(), new CachedOrder(order));

                // Add to order book - black box operation
                var result = orderBookService.addOrderToBook(order);

                if (result.isSuccess()) {
                    logger.info("✅ [SAGA: {}] Order {} added to book",
                            event.getCorrelationId(), event.getOrderId());
                } else {
                    logger.error("❌ [SAGA: {}] Failed to add order {} to book: {}",
                            event.getCorrelationId(), event.getOrderId(), result.getMessage());
                }

            } catch (Exception e) {
                logger.error("💥 [SAGA: {}] Error processing OrderCreatedEvent for order {}",
                        event.getCorrelationId(), event.getOrderId(), e);
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
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        handleEvent(event, () -> {
            logger.info("📥 [SAGA: {}] TransactionCreatedEvent received - TX: {}, Buy: {}, Sell: {}",
                    event.getCorrelationId(), event.getTransactionId(),
                    event.getBuyOrderId(), event.getSellOrderId());

            try {
                // Handle buy order
                if (event.isBuyOrderFullyExecuted()) {
                    logger.info("🗑️ [SAGA: {}] Removing fully executed buy order: {}",
                            event.getCorrelationId(), event.getBuyOrderId());

                    orderBookService.removeOrderFromBook(
                            event.getBuyOrderId(), event.getSymbol());

                    // Remove from cache since it's fully executed
                    orderCache.remove(event.getBuyOrderId());
                } else {
                    logger.debug("[SAGA: {}] Updating buy order {}: {} remaining",
                            event.getCorrelationId(), event.getBuyOrderId(),
                            event.getBuyOrderRemainingQuantity());

                    updateOrderInBook(event.getBuyOrderId(), event.getCorrelationId());
                }

                // Handle sell order
                if (event.isSellOrderFullyExecuted()) {
                    logger.info("🗑️ [SAGA: {}] Removing fully executed sell order: {}",
                            event.getCorrelationId(), event.getSellOrderId());

                    orderBookService.removeOrderFromBook(
                            event.getSellOrderId(), event.getSymbol());

                    // Remove from cache since it's fully executed
                    orderCache.remove(event.getSellOrderId());
                } else {
                    logger.debug("[SAGA: {}] Updating sell order {}: {} remaining",
                            event.getCorrelationId(), event.getSellOrderId(),
                            event.getSellOrderRemainingQuantity());

                    updateOrderInBook(event.getSellOrderId(), event.getCorrelationId());
                }

                // Trigger match check - fire and forget
                orderBookService.processPendingMatches(event.getSymbol());

                logger.debug("[SAGA: {}] Transaction processing complete for TX: {}",
                        event.getCorrelationId(), event.getTransactionId());

            } catch (Exception e) {
                logger.error("💥 [SAGA: {}] Error processing TransactionCreatedEvent",
                        event.getCorrelationId(), e);
            }
        });
    }

    /**
     * Updates an order in the book by removing and re-adding it.
     * Uses caching to reduce service calls.
     */
    private void updateOrderInBook(String orderId, String correlationId) {
        try {
            // Try cache first, refresh if stale or missing
            CachedOrder cached = orderCache.get(orderId);
            IOrder order;

            if (cached != null && !cached.isStale()) {
                order = cached.order;
                logger.debug("[SAGA: {}] Using cached order {}", correlationId, orderId);
            } else {
                // Fetch fresh order from service
                order = orderService.findOrderById(orderId)
                        .orElseThrow(() -> new IllegalStateException(
                                "Order not found for update: " + orderId));

                // Update cache
                orderCache.put(orderId, new CachedOrder(order));
                logger.debug("[SAGA: {}] Fetched fresh order {}", correlationId, orderId);
            }

            // Remove old version
            orderBookService.removeOrderFromBook(orderId, order.getSymbol());

            // Add updated version if still active
            if (order.isActive() && order.getRemainingQuantity().signum() > 0) {
                var result = orderBookService.addOrderToBook(order);

                if (!result.isSuccess()) {
                    logger.warn("[SAGA: {}] Failed to re-add order {} after update",
                            correlationId, orderId);
                }
            } else {
                // Order is done, remove from cache
                orderCache.remove(orderId);
            }
        } catch (Exception e) {
            logger.error("[SAGA: {}] Failed to update order {} in book",
                    correlationId, orderId, e);
            // Clear from cache on error
            orderCache.remove(orderId);
        }
    }

    /**
     * Periodic cleanup of stale cache entries to prevent memory leaks.
     * Runs every minute.
     */
    @Scheduled(fixedDelay = 60000)
    public void cleanupCache() {
        int sizeBefore = orderCache.size();
        orderCache.entrySet().removeIf(entry -> entry.getValue().isStale());
        int sizeAfter = orderCache.size();

        if (sizeBefore != sizeAfter) {
            logger.debug("Cache cleanup: removed {} stale entries, {} orders remaining",
                    sizeBefore - sizeAfter, sizeAfter);
        }
    }
}