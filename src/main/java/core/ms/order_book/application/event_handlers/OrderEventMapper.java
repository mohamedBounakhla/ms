package core.ms.order_book.application.event_handlers;

import core.ms.shared.events.EventBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventMapper {
    @Autowired
    private EventBus eventBus;

    @EventListener
    public void mapOrderCreatedEvent(core.ms.order.domain.events.publish.OrderCreatedEvent sourceEvent) {
        // Map to OrderBook's expected event structure
        core.ms.order_book.domain.events.subscribe.OrderCreatedEvent mappedEvent =
                new core.ms.order_book.domain.events.subscribe.OrderCreatedEvent(
                        sourceEvent.getCorrelationId(),
                        sourceEvent.getOrderId(),
                        sourceEvent.getPortfolioId(),
                        sourceEvent.getReservationId(),
                        sourceEvent.getSymbol(),
                        sourceEvent.getPrice(),
                        sourceEvent.getQuantity(),
                        sourceEvent.getOrderType(),
                        sourceEvent.getStatus()
                );

        // Re-publish for OrderBook handlers
        eventBus.publish(mappedEvent);
    }
}