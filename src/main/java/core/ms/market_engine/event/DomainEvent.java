package core.ms.market_engine.event;

import core.ms.shared.utils.IdGenerator;

import java.time.LocalDateTime;
import java.util.Objects;

public abstract class DomainEvent {
    protected final String eventId;
    protected final LocalDateTime timestamp;
    protected final String engineId;

    protected DomainEvent(String engineId) {
        this.eventId = new IdGenerator().generateEventId();
        this.timestamp = LocalDateTime.now();
        this.engineId = Objects.requireNonNull(engineId, "Engine ID cannot be null");
    }

    public String getEventId() {
        return eventId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getEngineId() {
        return engineId;
    }
}
