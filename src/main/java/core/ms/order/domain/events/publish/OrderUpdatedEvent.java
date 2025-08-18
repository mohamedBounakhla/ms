package core.ms.order.domain.events.publish;

import core.ms.shared.events.DomainEvent;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.time.LocalDateTime;

public class OrderUpdatedEvent implements DomainEvent {
    private final String orderId;
    private final String portfolioId;
    private final String updateType; // "PRICE_UPDATED"
    private final Symbol symbol;
    private final Money oldPrice;
    private final Money newPrice;
    private final LocalDateTime occurredAt;

    public OrderUpdatedEvent(String orderId, String portfolioId, String updateType,
                             Symbol symbol, Money oldPrice, Money newPrice) {
        this.orderId = orderId;
        this.portfolioId = portfolioId;
        this.updateType = updateType;
        this.symbol = symbol;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.occurredAt = LocalDateTime.now();
    }

    // All getters
    public String getOrderId() { return orderId; }
    public String getPortfolioId() { return portfolioId; }
    public String getUpdateType() { return updateType; }
    public Symbol getSymbol() { return symbol; }
    public Money getOldPrice() { return oldPrice; }
    public Money getNewPrice() { return newPrice; }
    @Override
    public LocalDateTime getOccurredAt() { return occurredAt; }
}