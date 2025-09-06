package core.ms.shared.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Monitors all events flowing through the event bus for debugging
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // Execute before other listeners
public class EventBusMonitor {

    private static final Logger logger = LoggerFactory.getLogger(EventBusMonitor.class);

    @EventListener
    public void monitorAllEvents(Object event) {
        if (event instanceof DomainEvent domainEvent) {
            logger.info("🔍 EVENT BUS MONITOR - Event Published:");
            logger.info("  📌 Event Type: {}", event.getClass().getSimpleName());
            logger.info("  📌 Correlation ID: {}", domainEvent.getCorrelationId());
            logger.info("  📌 Source BC: {}", domainEvent.getSourceBC());
            logger.info("  📌 Event Class: {}", event.getClass().getName());

            // Log specific event details
            if (event.getClass().getName().contains("OrderRequestedEvent")) {
                logger.info("  ⚡ This is an OrderRequestedEvent - Should trigger Order BC!");
                logOrderRequestedDetails(event);
            } else if (event.getClass().getName().contains("OrderCreatedEvent")) {
                logger.info("  ⚡ This is an OrderCreatedEvent - Should trigger Portfolio & OrderBook BC!");
            } else if (event.getClass().getName().contains("TransactionCreatedEvent")) {
                logger.info("  ⚡ This is a TransactionCreatedEvent - Should trigger Portfolio BC!");
            }

            logger.info("  📌 Event published successfully to Spring ApplicationContext");
        }
    }

    private void logOrderRequestedDetails(Object event) {
        try {
            // Use reflection to log details without importing specific event class
            var clazz = event.getClass();

            var portfolioMethod = clazz.getMethod("getPortfolioId");
            var reservationMethod = clazz.getMethod("getReservationId");
            var orderTypeMethod = clazz.getMethod("getOrderType");

            if (portfolioMethod != null && reservationMethod != null && orderTypeMethod != null) {
                var portfolioId = portfolioMethod.invoke(event);
                var reservationId = reservationMethod.invoke(event);
                var orderType = orderTypeMethod.invoke(event);

                logger.info("  📋 OrderRequestedEvent Details:");
                logger.info("    - Portfolio ID: {}", portfolioId);
                logger.info("    - Reservation ID: {}", reservationId);
                logger.info("    - Order Type: {}", orderType);
            }
        } catch (Exception e) {
            logger.debug("Could not extract OrderRequestedEvent details: {}", e.getMessage());
        }
    }
}