package core.ms.order_book.infrastructure.events.publishers;

import core.ms.order_book.domain.events.publish.OrderMatchedEvent;
import core.ms.order_book.domain.ports.outbound.OrderMatchEventPublisher;
import core.ms.order_book.infrastructure.events.mappers.OrderMatchedEventMapper;
import core.ms.shared.events.EventBus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class OrderMatchEventPublisherImpl implements OrderMatchEventPublisher {

    private final EventBus eventBus;

    public OrderMatchEventPublisherImpl(EventBus eventBus) {
        this.eventBus = Objects.requireNonNull(eventBus, "EventBus cannot be null");
    }

    @Override
    public void publishOrderMatchedEvents(List<OrderMatchedEvent> events) {
        Objects.requireNonNull(events, "Events list cannot be null");

        // Convert domain events to infrastructure messages
        var eventMessages = OrderMatchedEventMapper.toEventMessages(events);
        eventBus.publishAll(eventMessages);
    }

    @Override
    public void publishOrderMatchedEvent(OrderMatchedEvent event) {
        Objects.requireNonNull(event, "Event cannot be null");

        // Convert domain event to infrastructure message
        var eventMessage = OrderMatchedEventMapper.toEventMessage(event);
        eventBus.publish(eventMessage);
    }
}