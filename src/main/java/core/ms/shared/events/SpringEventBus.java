package core.ms.shared.events;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SpringEventBus implements EventBus {

    private final ApplicationEventPublisher eventPublisher;

    public SpringEventBus(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publishAll(List<? extends DomainEvent> events) {
        events.forEach(this::publish);
    }
}