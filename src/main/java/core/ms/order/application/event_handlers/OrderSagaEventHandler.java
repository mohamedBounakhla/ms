package core.ms.order.application.event_handlers;

import core.ms.order.application.services.OrderSagaService;
import core.ms.order.domain.events.subscribe.OrderMatchedEvent;
import core.ms.order.domain.events.subscribe.OrderRequestedEvent;
import core.ms.shared.events.EventContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class OrderSagaEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(OrderSagaEventHandler.class);

    @Autowired
    private OrderSagaService orderSagaService;

    /**
     * Handles OrderRequestedEvent from Portfolio BC.
     * Creates an order with reservation reference.
     */
    @EventListener
    @Async
    public void handleOrderRequested(OrderRequestedEvent event) {
        logger.info("📥 [SAGA: {}] Received OrderRequestedEvent from {} - Reservation: {}, Portfolio: {}, Type: {}, Symbol: {}",
                event.getCorrelationId(), event.getSourceBC(), event.getReservationId(),
                event.getPortfolioId(), event.getOrderType(), event.getSymbolCode());

        try {
            // Set correlation ID for the current thread
            EventContext.setCorrelationId(event.getCorrelationId());

            // Process the order request
            orderSagaService.processOrderRequest(event);

            logger.info("✅ [SAGA: {}] Successfully processed order request for reservation: {}",
                    event.getCorrelationId(), event.getReservationId());

        } catch (Exception e) {
            logger.error("❌ [SAGA: {}] Failed to process order request - Reservation: {}, Error: {}",
                    event.getCorrelationId(), event.getReservationId(), e.getMessage(), e);

            // Publish failure event
            orderSagaService.publishOrderCreationFailed(
                    event.getCorrelationId(),
                    event.getReservationId(),
                    event.getPortfolioId(),
                    e.getMessage()
            );

        } finally {
            EventContext.clear();
        }
    }

    /**
     * Handles OrderMatchedEvent from OrderBook BC.
     * Creates a transaction and updates both orders.
     */
    @EventListener
    @Async
    public void handleOrderMatched(OrderMatchedEvent event) {
        logger.info("📥 [SAGA: {}] Received OrderMatchedEvent from {} - Buy: {}, Sell: {}, Quantity: {}, Price: {}",
                event.getCorrelationId(), event.getSourceBC(), event.getBuyOrderId(),
                event.getSellOrderId(), event.getMatchedQuantity(), event.getExecutionPrice());

        try {
            // Set correlation ID for the current thread
            EventContext.setCorrelationId(event.getCorrelationId());

            // Process the matched orders
            orderSagaService.processOrderMatch(event);

            logger.info("✅ [SAGA: {}] Successfully processed order match - Buy: {}, Sell: {}",
                    event.getCorrelationId(), event.getBuyOrderId(), event.getSellOrderId());

        } catch (Exception e) {
            logger.error("❌ [SAGA: {}] Failed to process order match - Buy: {}, Sell: {}, Error: {}",
                    event.getCorrelationId(), event.getBuyOrderId(), event.getSellOrderId(), e.getMessage(), e);

            orderSagaService.publishTransactionCreationFailed(
                    event.getCorrelationId(),
                    event.getBuyOrderId(),
                    event.getSellOrderId(),
                    e.getMessage()
            );

        } finally {
            EventContext.clear();
        }
    }
}