package core.ms.shared.events;

import java.time.LocalDateTime;

public abstract class BaseEvent implements DomainEvent {
    private final String correlationId;
    private final String sourceBC;
    private final LocalDateTime occurredAt;

    protected BaseEvent(String correlationId, String sourceBC) {
        this.correlationId = correlationId;
        this.sourceBC = sourceBC;
        this.occurredAt = LocalDateTime.now();
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getSourceBC() {
        return sourceBC;
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}