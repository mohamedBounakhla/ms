package core.ms.order_book.domain.ports.outbound;

import core.ms.order_book.domain.events.publish.OrderMatchedEvent;

import java.util.List;

public interface OrderMatchEventPublisher {
    /**
     * Publishes a list of order matched events.
     */
    void publishOrderMatchedEvents(List<OrderMatchedEvent> events);

    /**
     * Publishes a single order matched event.
     */
    void publishOrderMatchedEvent(OrderMatchedEvent event);
}