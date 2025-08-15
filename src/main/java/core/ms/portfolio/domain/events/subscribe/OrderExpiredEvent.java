package core.ms.portfolio.domain.events.subscribe;

import core.ms.shared.OrderType;
import core.ms.shared.events.DomainEvent;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Symbol;

import java.time.LocalDateTime;

public class OrderExpiredEvent implements DomainEvent {
    private final String orderId;
    private final String portfolioId;
    private final String reservationId;
    private final Symbol symbol;
    private final Currency currency; // For buy orders
    private final OrderType type;
    private final LocalDateTime occurredAt;

    public OrderExpiredEvent(String orderId, String portfolioId, String reservationId, Symbol symbol, Currency currency, OrderType type) {
        this.orderId = orderId;
        this.portfolioId = portfolioId;
        this.reservationId = reservationId;
        this.symbol = symbol;
        this.currency = currency;
        this.type = type;
        this.occurredAt = LocalDateTime.now();
    }

    public String getOrderId() {
        return orderId;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public String getReservationId() {
        return reservationId;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public Currency getCurrency() {
        return currency;
    }

    public OrderType getType() {
        return type;
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}