package core.ms.order_book.application.event_handlers;

import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.ports.outbound.OrderRepository;
import core.ms.order_book.application.services.OrderBookApplicationService;
import core.ms.order_book.application.services.OrderSynchronizationService;
import core.ms.order_book.domain.events.subscribe.AddOrderToBookEvent;
import core.ms.order_book.domain.events.subscribe.ProcessMatchesEvent;
import core.ms.order_book.domain.events.subscribe.RemoveOrderFromBookEvent;
import core.ms.order_book.domain.events.subscribe.UpdateOrderInBookEvent;
import core.ms.shared.money.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class MarketEngineOrderBookEventSubscriber {

    private static final Logger logger = LoggerFactory.getLogger(MarketEngineOrderBookEventSubscriber.class);

    @Autowired
    private OrderBookApplicationService orderBookService;

    @Autowired
    private OrderSynchronizationService syncService;

    // ===== COMMAND EVENTS FROM MARKET ENGINE =====

    @EventListener
    @Async
    public void handleAddOrderToBook(AddOrderToBookEvent event) {
        logger.info("📥 Received AddOrderToBook command - Order: {}, Symbol: {}",
                event.getOrderId(), event.getSymbolCode());

        try {
            // Use sync service for efficient fetching
            IOrder order = syncService.getOrder(event.getOrderId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Order not found: " + event.getOrderId()));

            // Add to order book
            var result = orderBookService.addOrderToBook(order);

            if (result.isSuccess()) {
                logger.info("✅ Order added to book - Order: {}, Matches: {}",
                        event.getOrderId(), result.getMatchCount());
            } else {
                logger.error("❌ Failed to add order - Order: {}, Error: {}",
                        event.getOrderId(), result.getMessage());
            }

        } catch (Exception e) {
            logger.error("💥 Error processing AddOrderToBook event", e);
        }
    }

    @EventListener
    @Async
    public void handleRemoveOrderFromBook(RemoveOrderFromBookEvent event) {
        logger.info("📥 Received RemoveOrderFromBook command from Market Engine - Order: {}, Reason: {}",
                event.getOrderId(), event.getReason());

        try {
            Symbol symbol = createSymbol(event.getSymbolCode());

            var result = orderBookService.removeOrderFromBook(event.getOrderId(), symbol);

            if (result.isSuccess()) {
                logger.info("✅ Order removed from book - Order: {}", event.getOrderId());
            } else {
                logger.warn("⚠️ Could not remove order from book - Order: {}, Message: {}",
                        event.getOrderId(), result.getMessage());
            }

        } catch (Exception e) {
            logger.error("💥 Error processing RemoveOrderFromBook event", e);
        }
    }

    @EventListener
    @Async
    public void handleUpdateOrderInBook(UpdateOrderInBookEvent event) {
        logger.info("📥 Received UpdateOrderInBook command - Order: {}, Type: {}",
                event.getOrderId(), event.getUpdateType());

        try {
            // Force refresh for updates
            boolean forceRefresh = "QUANTITY_CHANGE".equals(event.getUpdateType()) ||
                    "STATUS_CHANGE".equals(event.getUpdateType());

            IOrder updatedOrder = syncService.getOrderWithRefresh(event.getOrderId(), forceRefresh)
                    .orElseThrow(() -> new IllegalStateException(
                            "Order not found: " + event.getOrderId()));

            // Check if update is significant
            IOrder cachedOrder = syncService.getOrder(event.getOrderId()).orElse(null);

            if (syncService.isSignificantUpdate(cachedOrder, updatedOrder)) {
                // Update in order book
                Symbol symbol = updatedOrder.getSymbol();
                orderBookService.removeOrderFromBook(event.getOrderId(), symbol);

                if (updatedOrder.isActive()) {
                    orderBookService.addOrderToBook(updatedOrder);
                    logger.info("✅ Order updated in book - Order: {}", event.getOrderId());
                } else {
                    logger.info("✅ Order removed (inactive) - Order: {}", event.getOrderId());
                }
            } else {
                logger.debug("Update not significant, skipping - Order: {}", event.getOrderId());
            }

        } catch (Exception e) {
            logger.error("💥 Error processing UpdateOrderInBook event", e);
        }
    }

    @EventListener
    @Async
    public void handleProcessMatches(ProcessMatchesEvent event) {
        logger.info("📥 Received ProcessMatches command from Market Engine - Symbol: {}",
                event.getSymbolCode());

        try {
            Symbol symbol = createSymbol(event.getSymbolCode());

            var matchEvents = orderBookService.processPendingMatches(symbol);

            logger.info("✅ Processed {} matches for symbol: {}",
                    matchEvents.size(), event.getSymbolCode());

        } catch (Exception e) {
            logger.error("💥 Error processing ProcessMatches event", e);
        }
    }

    // ===== HELPER METHODS =====

    private Symbol createSymbol(String symbolCode) {
        return switch (symbolCode.toUpperCase()) {
            case "BTC" -> Symbol.btcUsd();
            case "ETH" -> Symbol.ethUsd();
            case "EURUSD" -> Symbol.eurUsd();
            case "GBPUSD" -> Symbol.gbpUsd();
            default -> throw new IllegalArgumentException("Unsupported symbol: " + symbolCode);
        };
    }
}