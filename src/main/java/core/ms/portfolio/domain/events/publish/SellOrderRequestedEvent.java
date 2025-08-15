package core.ms.portfolio.domain.events.publish;

import core.ms.shared.events.DomainEvent;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SellOrderRequestedEvent implements DomainEvent {
    private final String portfolioId;
    private final String reservationId;
    private final Symbol symbol;
    private final BigDecimal quantity;
    private final Money price;
    private final LocalDateTime occurredAt;

    public SellOrderRequestedEvent(String portfolioId, String reservationId,
                                   Symbol symbol, BigDecimal quantity, Money price) {
        this.portfolioId = portfolioId;
        this.reservationId = reservationId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.occurredAt = LocalDateTime.now();
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    // Getters
    public String getPortfolioId() { return portfolioId; }
    public String getReservationId() { return reservationId; }
    public Symbol getSymbol() { return symbol; }
    public BigDecimal getQuantity() { return quantity; }
    public Money getPrice() { return price; }
}