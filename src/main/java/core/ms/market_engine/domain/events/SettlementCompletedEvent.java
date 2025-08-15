package core.ms.market_engine.domain.events;

import core.ms.shared.events.DomainEvent;

import java.time.LocalDateTime;

public class SettlementCompletedEvent implements DomainEvent {
    private final String matchId;
    private final LocalDateTime occurredAt;

    public SettlementCompletedEvent(String matchId) {
        this.matchId = matchId;
        this.occurredAt = LocalDateTime.now();
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getMatchId() { return matchId; }
}