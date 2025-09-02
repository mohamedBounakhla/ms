package core.ms.order_book.infrastructure.events.publishers;

import core.ms.order_book.domain.events.publish.OrderMatchedEvent;
import core.ms.order_book.domain.ports.outbound.OrderMatchEventPublisher;
import core.ms.shared.events.EventBus;
import core.ms.shared.events.EventContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class OrderMatchEventPublisherImpl implements OrderMatchEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OrderMatchEventPublisherImpl.class);

    private final EventBus eventBus;

    public OrderMatchEventPublisherImpl(EventBus eventBus) {
        this.eventBus = Objects.requireNonNull(eventBus, "EventBus cannot be null");
    }

    @Override
    public void publishOrderMatchedEvents(List<OrderMatchedEvent> events) {
        Objects.requireNonNull(events, "Events list cannot be null");

        for (OrderMatchedEvent event : events) {
            publishOrderMatchedEvent(event);
        }
    }

    @Override
    public void publishOrderMatchedEvent(OrderMatchedEvent event) {
        Objects.requireNonNull(event, "Event cannot be null");

        // Ensure correlation ID is set in context before publishing
        if (event.getCorrelationId() != null) {
            EventContext.setCorrelationId(event.getCorrelationId());
        }

        logger.info("📤 [SAGA: {}] Publishing OrderMatchedEvent - Buy: {}, Sell: {}, Quantity: {}",
                event.getCorrelationId(), event.getBuyOrderId(),
                event.getSellOrderId(), event.getMatchedQuantity());

        eventBus.publish(event);
    }
}