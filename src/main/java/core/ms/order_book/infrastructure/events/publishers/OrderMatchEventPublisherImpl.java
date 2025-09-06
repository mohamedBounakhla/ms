package core.ms.order_book.infrastructure.events.publishers;

import core.ms.order_book.domain.events.publish.OrderMatchedEvent;
import core.ms.order_book.domain.ports.outbound.OrderMatchEventPublisher;
import core.ms.shared.events.EventBus;
import core.ms.shared.events.EventContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class OrderMatchEventPublisherImpl implements OrderMatchEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OrderMatchEventPublisherImpl.class);

    @Autowired
    private EventBus eventBus;

    @Override
    public void publishOrderMatchedEvents(List<OrderMatchedEvent> events) {
        Objects.requireNonNull(events, "Events list cannot be null");

        logger.info("════════════════════════════════════════════════════════════════");
        logger.info("📤 MATCH PUBLISHER: Publishing {} OrderMatchedEvents", events.size());
        logger.info("════════════════════════════════════════════════════════════════");

        for (OrderMatchedEvent event : events) {
            publishOrderMatchedEvent(event);
        }

        logger.info("════════════════════════════════════════════════════════════════");
        logger.info("✅ MATCH PUBLISHER: All {} events published", events.size());
        logger.info("════════════════════════════════════════════════════════════════");
    }

    @Override
    public void publishOrderMatchedEvent(OrderMatchedEvent event) {
        Objects.requireNonNull(event, "Event cannot be null");

        // Ensure correlation ID is set in context before publishing
        if (event.getCorrelationId() != null) {
            EventContext.setCorrelationId(event.getCorrelationId());
        }

        logger.info("📤 MATCH PUBLISHER: Publishing OrderMatchedEvent");
        logger.info("   - Correlation ID: {}", event.getCorrelationId());
        logger.info("   - Buy Order: {}", event.getBuyOrderId());
        logger.info("   - Sell Order: {}", event.getSellOrderId());
        logger.info("   - Symbol: {}", event.getSymbol().getCode());
        logger.info("   - Matched Quantity: {}", event.getMatchedQuantity());
        logger.info("   - Execution Price: {} {}",
                event.getExecutionPrice().getAmount(),
                event.getExecutionPrice().getCurrency());
        logger.info("   - Total Value: {}", event.getTotalValue());

        try {
            eventBus.publish(event);
            logger.info("✅ MATCH PUBLISHER: OrderMatchedEvent published successfully");
        } catch (Exception e) {
            logger.error("💥 MATCH PUBLISHER: Failed to publish OrderMatchedEvent", e);
            throw new RuntimeException("Failed to publish OrderMatchedEvent", e);
        }
    }
}