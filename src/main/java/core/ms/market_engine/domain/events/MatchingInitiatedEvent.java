package core.ms.market_engine.domain.events;

import core.ms.shared.events.DomainEvent;
import core.ms.shared.money.Symbol;

import java.time.LocalDateTime;

public class MatchingInitiatedEvent implements DomainEvent {
    private final String orderId;
    private final Symbol symbol;
    private final LocalDateTime occurredAt;

    public MatchingInitiatedEvent(String orderId, Symbol symbol) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.occurredAt = LocalDateTime.now();
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getOrderId() { return orderId; }
    public Symbol getSymbol() { return symbol; }
}