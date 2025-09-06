package core.ms.order.application.event_handlers;

import core.ms.order.application.services.OrderSagaService;
import core.ms.order_book.domain.events.publish.OrderMatchedEvent;
import core.ms.portfolio.domain.events.publish.OrderRequestedEvent;
import core.ms.shared.events.CorrelationAwareEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderSagaEventHandler extends CorrelationAwareEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderSagaEventHandler.class);

    @Autowired
    private OrderSagaService orderSagaService;

    /**
     * Handles OrderRequestedEvent from Portfolio BC.
     */
    @EventListener
    @Transactional
    public void handleOrderRequested(OrderRequestedEvent event) {
        logger.info("════════════════════════════════════════════════════════════════");
        logger.info("🎯 ORDER BC: RECEIVED OrderRequestedEvent from Portfolio BC");
        logger.info("📋 Event details:");
        logger.info("   - Correlation ID: {}", event.getCorrelationId());
        logger.info("   - Source: {}", event.getSourceBC());
        logger.info("   - Reservation: {}", event.getReservationId());
        logger.info("   - Portfolio: {}", event.getPortfolioId());
        logger.info("   - Order Type: {}", event.getOrderType());
        logger.info("   - Symbol: {}", event.getSymbol().getCode());
        logger.info("   - Price: {}", event.getPrice().toDisplayString());
        logger.info("   - Quantity: {}", event.getQuantity());
        logger.info("════════════════════════════════════════════════════════════════");

        handleEvent(event, () -> {
            try {
                logger.info("🔄 ORDER BC: Starting order creation process");

                // Convert Portfolio's OrderRequestedEvent to internal format
                core.ms.order.domain.events.subscribe.OrderRequestedEvent internalEvent =
                        new core.ms.order.domain.events.subscribe.OrderRequestedEvent(
                                event.getCorrelationId(),
                                event.getSourceBC(),
                                event.getReservationId(),
                                event.getPortfolioId(),
                                event.getOrderType().name(),
                                event.getSymbol().getCode(),
                                event.getPrice().getAmount(),
                                event.getPrice().getCurrency(),
                                event.getQuantity()
                        );

                logger.info("📦 ORDER BC: Calling OrderSagaService.processOrderRequest");

                // Process the order request
                orderSagaService.processOrderRequest(internalEvent);

                logger.info("✅ ORDER BC: Successfully processed order request for reservation: {}",
                        event.getReservationId());

            } catch (Exception e) {
                logger.error("❌ ORDER BC: Failed to process order request", e);
                logger.error("   - Reservation: {}", event.getReservationId());
                logger.error("   - Error: {}", e.getMessage());

                // Publish failure event
                orderSagaService.publishOrderCreationFailed(
                        event.getCorrelationId(),
                        event.getReservationId(),
                        event.getPortfolioId(),
                        e.getMessage()
                );

                throw new RuntimeException("Order creation failed", e);
            }
        });
    }

    /**
     * Handles OrderMatchedEvent from OrderBook BC.
     * Creates a transaction and updates both orders.
     */
    @EventListener
    @Transactional
    public void handleOrderMatched(OrderMatchedEvent event) {
        logger.info("════════════════════════════════════════════════════════════════");
        logger.info("🎯 ORDER BC: RECEIVED OrderMatchedEvent from OrderBook BC");
        logger.info("📋 Match details:");
        logger.info("   - Correlation ID: {}", event.getCorrelationId());
        logger.info("   - Source BC: {}", event.getSourceBC());
        logger.info("   - Buy Order: {}", event.getBuyOrderId());
        logger.info("   - Sell Order: {}", event.getSellOrderId());
        logger.info("   - Symbol: {}", event.getSymbol().getCode());
        logger.info("   - Matched Quantity: {}", event.getMatchedQuantity());
        logger.info("   - Execution Price: {} {}",
                event.getExecutionPrice().getAmount(),
                event.getExecutionPrice().getCurrency());
        logger.info("   - Total Value: {}", event.getTotalValue());
        logger.info("════════════════════════════════════════════════════════════════");

        handleEvent(event, () -> {
            try {
                logger.info("🔄 ORDER BC: Starting transaction creation process");

                // Convert to internal event format if needed
                core.ms.order.domain.events.subscribe.OrderMatchedEvent internalEvent =
                        new core.ms.order.domain.events.subscribe.OrderMatchedEvent(
                                event.getCorrelationId(),
                                event.getSourceBC(),
                                event.getBuyOrderId(),
                                event.getSellOrderId(),
                                event.getMatchedQuantity(),
                                event.getExecutionPrice().getAmount(),
                                event.getExecutionPrice().getCurrency()
                        );

                // Process the matched orders
                logger.info("📦 ORDER BC: Calling OrderSagaService.processOrderMatch");
                orderSagaService.processOrderMatch(internalEvent);

                logger.info("✅ ORDER BC: Successfully processed order match");
                logger.info("   - Transaction created for Buy: {}, Sell: {}",
                        event.getBuyOrderId(), event.getSellOrderId());

            } catch (Exception e) {
                logger.error("❌ ORDER BC: Failed to process order match", e);
                logger.error("   - Buy Order: {}", event.getBuyOrderId());
                logger.error("   - Sell Order: {}", event.getSellOrderId());
                logger.error("   - Error: {}", e.getMessage());

                orderSagaService.publishTransactionCreationFailed(
                        event.getCorrelationId(),
                        event.getBuyOrderId(),
                        event.getSellOrderId(),
                        e.getMessage()
                );

                throw new RuntimeException("Transaction creation failed", e);
            }
        });
    }
}