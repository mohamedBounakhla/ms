package core.ms.market_engine.domain.workflow;

import core.ms.market_engine.domain.events.OrderValidatedEvent;
import core.ms.market_engine.domain.events.ReservationCreatedEvent;
import core.ms.shared.events.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OrderProcessingWorkflow {
    private static final Logger log = LoggerFactory.getLogger(OrderProcessingWorkflow.class);

    private final EventBus eventBus;

    public OrderProcessingWorkflow(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public boolean processOrder(String orderId, String portfolioId) {
        log.debug("Starting order processing workflow for order: {}", orderId);

        // Step 1: Validate order exists and is active
        // This would typically call the Order BC
        if (!validateOrder(orderId)) {
            return false;
        }

        // Step 2: Create reservation in Portfolio BC
        // This is handled via events
        boolean reservationCreated = createReservation(orderId, portfolioId);

        if (!reservationCreated) {
            log.error("Failed to create reservation for order: {}", orderId);
            return false;
        }

        // Step 3: Publish order validated event
        eventBus.publish(new OrderValidatedEvent(orderId, portfolioId));

        return true;
    }

    private boolean validateOrder(String orderId) {
        // Implementation would check order status, etc.
        return true;
    }

    private boolean createReservation(String orderId, String portfolioId) {
        // Implementation would interact with Portfolio BC
        eventBus.publish(new ReservationCreatedEvent(orderId, portfolioId));
        return true;
    }
}