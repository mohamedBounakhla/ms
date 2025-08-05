package core.ms.shared.events;

import java.util.List;

public interface EventBus {
    void publish(DomainEvent event);
    void publishAll(List<? extends DomainEvent> events);
}