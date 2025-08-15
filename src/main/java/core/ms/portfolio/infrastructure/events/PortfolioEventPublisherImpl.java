package core.ms.portfolio.infrastructure.events;

import core.ms.portfolio.domain.ports.outbound.PortfolioEventPublisher;
import core.ms.shared.events.DomainEvent;
import core.ms.shared.events.EventBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PortfolioEventPublisherImpl implements PortfolioEventPublisher {

    @Autowired
    private EventBus eventBus;

    @Override
    public void publishEvent(DomainEvent event) {
        eventBus.publish(event);
    }

    @Override
    public void publishEvents(List<DomainEvent> events) {
        eventBus.publishAll(events);
    }
}