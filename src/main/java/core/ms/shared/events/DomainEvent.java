package core.ms.shared.events;

import java.time.LocalDateTime;

public interface DomainEvent {
    /**
     * Gets the correlation ID for saga pattern tracking
     */
    String getCorrelationId();

    /**
     * Gets the source bounded context that emitted this event
     */
    String getSourceBC();

    /**
     * Gets the timestamp when the event occurred
     */
    LocalDateTime getOccurredAt();
}