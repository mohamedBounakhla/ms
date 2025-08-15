package core.ms.market_engine.domain.events;

import core.ms.shared.events.DomainEvent;

import java.time.LocalDateTime;

public class ReservationCreatedEvent implements DomainEvent {
    private final String orderId;
    private final String portfolioId;
    private final LocalDateTime occurredAt;

    public ReservationCreatedEvent(String orderId, String portfolioId) {
        this.orderId = orderId;
        this.portfolioId = portfolioId;
        this.occurredAt = LocalDateTime.now();
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getOrderId() { return orderId; }
    public String getPortfolioId() { return portfolioId; }
}