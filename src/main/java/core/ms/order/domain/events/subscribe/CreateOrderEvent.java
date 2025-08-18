package core.ms.order.domain.events.subscribe;

import core.ms.shared.events.DomainEvent;
import core.ms.shared.money.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateOrderEvent implements DomainEvent {
    private final String commandId;
    private final String portfolioId;
    private final String reservationId;
    private final String orderType; // "BUY" or "SELL"
    private final String symbolCode;
    private final BigDecimal price;
    private final Currency currency;
    private final BigDecimal quantity;
    private final LocalDateTime occurredAt;

    public CreateOrderEvent(String commandId, String portfolioId, String reservationId,
                            String orderType, String symbolCode, BigDecimal price,
                            Currency currency, BigDecimal quantity) {
        this.commandId = commandId;
        this.portfolioId = portfolioId;
        this.reservationId = reservationId;
        this.orderType = orderType;
        this.symbolCode = symbolCode;
        this.price = price;
        this.currency = currency;
        this.quantity = quantity;
        this.occurredAt = LocalDateTime.now();
    }

    // Getters
    public String getCommandId() { return commandId; }
    public String getPortfolioId() { return portfolioId; }
    public String getReservationId() { return reservationId; }
    public String getOrderType() { return orderType; }
    public String getSymbolCode() { return symbolCode; }
    public BigDecimal getPrice() { return price; }
    public Currency getCurrency() { return currency; }
    public BigDecimal getQuantity() { return quantity; }
    @Override
    public LocalDateTime getOccurredAt() { return occurredAt; }
}