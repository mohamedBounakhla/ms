package core.ms.shared.events;

import java.time.LocalDateTime;

public interface DomainEvent {
    LocalDateTime getOccurredAt();
}