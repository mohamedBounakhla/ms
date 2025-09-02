package core.ms.order_book.application.event_handlers;

import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.ports.outbound.OrderRepository;
import core.ms.order_book.application.services.OrderBookApplicationService;
import core.ms.order_book.domain.events.subscribe.OrderCreatedEvent;
import core.ms.order_book.domain.events.subscribe.TransactionCreatedEvent;
import core.ms.shared.events.CorrelationAwareEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
public class OrderBookSagaEventHandler extends CorrelationAwareEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderBookSagaEventHandler.class);

    @Autowired
    private OrderBookApplicationService orderBookService;

    @Autowired
    private OrderRepository orderRepository;

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
                // Fetch the actual order from the repository
                IOrder order = orderRepository.findById(event.getOrderId())
                        .orElseThrow(() -> new IllegalStateException(
                                "Order not found: " + event.getOrderId()));

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
     * This is a black box operation - we don't care about matches.
     */
    private void updateOrderInBook(String orderId, String correlationId) {
        try {
            IOrder order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Order not found for update: " + orderId));

            // Remove old version
            orderBookService.removeOrderFromBook(orderId, order.getSymbol());

            // Add updated version if still active
            if (order.isActive() && order.getRemainingQuantity().signum() > 0) {
                var result = orderBookService.addOrderToBook(order);

                if (!result.isSuccess()) {
                    logger.warn("[SAGA: {}] Failed to re-add order {} after update",
                            correlationId, orderId);
                }
            }
        } catch (Exception e) {
            logger.error("[SAGA: {}] Failed to update order {} in book",
                    correlationId, orderId, e);
        }
    }
}