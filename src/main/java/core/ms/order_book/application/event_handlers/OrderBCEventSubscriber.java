package core.ms.order_book.application.event_handlers;

import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.events.publish.*;
import core.ms.order.domain.ports.outbound.OrderRepository;
import core.ms.order_book.application.services.OrderBookApplicationService;
import core.ms.order_book.application.services.OrderBookWebSocketService;
import core.ms.order_book.application.services.OrderSynchronizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens to Order BC domain events to keep order book synchronized.
 * Uses repository queries to ensure data consistency in monolithic architecture.
 */
@Component
public class OrderBCEventSubscriber {

    private static final Logger logger = LoggerFactory.getLogger(OrderBCEventSubscriber.class);

    @Autowired
    private OrderBookApplicationService orderBookService;

    @Autowired
    private OrderSynchronizationService syncService;

    @Autowired(required = false)
    private OrderBookWebSocketService webSocketService;

    // Cache to track orders we're managing
    private final Map<String, String> orderSymbolCache = new ConcurrentHashMap<>();

    // ===== ORDER LIFECYCLE EVENTS =====

    @EventListener
    @Async
    public void handleOrderCreated(OrderCreatedEvent event) {
        logger.debug("📄 Order created event - ID: {}, Symbol: {}, Status: {}",
                event.getOrderId(), event.getSymbol().getCode(), event.getStatus());

        // Cache the symbol for this order
        orderSymbolCache.put(event.getOrderId(), event.getSymbol().getCode());

        // Note: We don't add to order book here
        // Market Engine will send AddOrderToBookEvent when appropriate
    }

    @EventListener
    @Async
    public void handleOrderCancelled(OrderCancelledEvent event) {
        logger.info("🚫 Order cancelled event - ID: {}, Remaining: {}",
                event.getOrderId(), event.getRemainingQuantity());

        try {
            // Order should be removed from book
            var result = orderBookService.removeOrderFromBook(
                    event.getOrderId(),
                    event.getSymbol()
            );

            if (result.isSuccess()) {
                logger.debug("Order removed from book after cancellation: {}", event.getOrderId());
            }

            // Notify WebSocket clients
            notifyWebSocketClients(event.getSymbol().getCode());

        } catch (Exception e) {
            logger.error("Error handling order cancelled event", e);
        }
    }

    @EventListener
    @Async
    public void handleOrderPartiallyFilled(OrderPartiallyFilledEvent event) {
        logger.info("📊 Order partially filled - ID: {}, Filled: {}, Remaining: {}",
                event.getOrderId(), event.getFilledQuantity(), event.getRemainingQuantity());

        try {
            // Force refresh for quantity changes
            IOrder updatedOrder = syncService.getOrderWithRefresh(event.getOrderId(), true)
                    .orElse(null);

            if (updatedOrder != null && updatedOrder.isActive()) {
                updateOrderInBook(updatedOrder);
                notifyWebSocketClients(event.getSymbol().getCode());
            }

        } catch (Exception e) {
            logger.error("Error handling order partially filled event", e);
        }
    }

    @EventListener
    @Async
    public void handleOrderFilled(OrderFilledEvent event) {
        logger.info("✅ Order filled - ID: {}, Quantity: {}",
                event.getOrderId(), event.getFilledQuantity());

        try {
            // Remove filled order from book
            var result = orderBookService.removeOrderFromBook(
                    event.getOrderId(),
                    event.getSymbol()
            );

            if (result.isSuccess()) {
                logger.debug("Filled order removed from book: {}", event.getOrderId());
            }

            // Notify WebSocket clients
            notifyWebSocketClients(event.getSymbol().getCode());

        } catch (Exception e) {
            logger.error("Error handling order filled event", e);
        }
    }

    @EventListener
    @Async
    public void handleOrderUpdated(OrderUpdatedEvent event) {
        logger.info("🔄 Order updated - ID: {}, Type: {}",
                event.getOrderId(), event.getUpdateType());

        try {
            // Use syncService instead of orderRepository
            IOrder updatedOrder = syncService.getOrderWithRefresh(event.getOrderId(), true)
                    .orElse(null);

            if (updatedOrder != null && updatedOrder.isActive()) {
                updateOrderInBook(updatedOrder);
                notifyWebSocketClients(event.getSymbol().getCode());
            }

        } catch (Exception e) {
            logger.error("Error handling order updated event", e);
        }
    }

    @EventListener
    @Async
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        logger.info("💱 Transaction created - Buy: {}, Sell: {}, Quantity: {}",
                event.getBuyOrderId(), event.getSellOrderId(), event.getExecutedQuantity());

        try {
            // Force refresh both orders after transaction
            IOrder buyOrder = syncService.getOrderWithRefresh(event.getBuyOrderId(), true)
                    .orElse(null);
            IOrder sellOrder = syncService.getOrderWithRefresh(event.getSellOrderId(), true)
                    .orElse(null);

            // Update or remove based on state
            if (buyOrder != null) {
                if (buyOrder.isActive() && buyOrder.getRemainingQuantity().signum() > 0) {
                    updateOrderInBook(buyOrder);
                } else {
                    orderBookService.removeOrderFromBook(buyOrder.getId(), buyOrder.getSymbol());
                }
            }

            if (sellOrder != null) {
                if (sellOrder.isActive() && sellOrder.getRemainingQuantity().signum() > 0) {
                    updateOrderInBook(sellOrder);
                } else {
                    orderBookService.removeOrderFromBook(sellOrder.getId(), sellOrder.getSymbol());
                }
            }

            notifyWebSocketClients(event.getSymbol().getCode());

        } catch (Exception e) {
            logger.error("Error handling transaction created event", e);
        }
    }

    // ===== HELPER METHODS =====

    private void updateOrderInBook(IOrder order) {
        // Remove old version
        orderBookService.removeOrderFromBook(order.getId(), order.getSymbol());

        // Add updated version if still active
        if (order.isActive() && order.getRemainingQuantity().signum() > 0) {
            var result = orderBookService.addOrderToBook(order);

            if (result.isSuccess()) {
                logger.debug("Order updated in book: {}", order.getId());
            }
        }
    }

    private void notifyWebSocketClients(String symbolCode) {
        if (webSocketService != null) {
            try {
                webSocketService.broadcastOrderBookUpdate(symbolCode);
            } catch (Exception e) {
                logger.error("Failed to notify WebSocket clients for symbol: {}", symbolCode, e);
            }
        }
    }
}