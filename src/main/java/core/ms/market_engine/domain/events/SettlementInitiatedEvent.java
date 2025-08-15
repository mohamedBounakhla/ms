package core.ms.market_engine.domain.events;

import core.ms.shared.events.DomainEvent;

import java.time.LocalDateTime;

public class SettlementInitiatedEvent implements DomainEvent {
    private final String buyOrderId;
    private final String sellOrderId;
    private final String matchId;
    private final LocalDateTime occurredAt;

    public SettlementInitiatedEvent(String buyOrderId, String sellOrderId, String matchId) {
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.matchId = matchId;
        this.occurredAt = LocalDateTime.now();
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getBuyOrderId() { return buyOrderId; }
    public String getSellOrderId() { return sellOrderId; }
    public String getMatchId() { return matchId; }
}