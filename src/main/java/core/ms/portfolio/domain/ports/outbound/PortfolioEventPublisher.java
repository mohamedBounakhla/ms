package core.ms.portfolio.domain.ports.outbound;

import core.ms.shared.events.DomainEvent;

import java.util.List;

public interface PortfolioEventPublisher {
    void publishEvent(DomainEvent event);
    void publishEvents(List<DomainEvent> events);
}