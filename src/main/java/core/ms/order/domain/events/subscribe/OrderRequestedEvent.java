package core.ms.order.domain.events.subscribe;

import core.ms.shared.events.BaseEvent;
import core.ms.shared.money.Currency;

import java.math.BigDecimal;

public class OrderRequestedEvent extends BaseEvent {
    private final String reservationId;
    private final String portfolioId;
    private final String orderType; // "BUY" or "SELL"
    private final String symbolCode;
    private final BigDecimal price;
    private final Currency currency;
    private final BigDecimal quantity;

    public OrderRequestedEvent(String correlationId, String sourceBC,
                               String reservationId, String portfolioId,
                               String orderType, String symbolCode,
                               BigDecimal price, Currency currency,
                               BigDecimal quantity) {
        super(correlationId, sourceBC);
        this.reservationId = reservationId;
        this.portfolioId = portfolioId;
        this.orderType = orderType;
        this.symbolCode = symbolCode;
        this.price = price;
        this.currency = currency;
        this.quantity = quantity;
    }

    // Getters
    public String getReservationId() { return reservationId; }
    public String getPortfolioId() { return portfolioId; }
    public String getOrderType() { return orderType; }
    public String getSymbolCode() { return symbolCode; }
    public BigDecimal getPrice() { return price; }
    public Currency getCurrency() { return currency; }
    public BigDecimal getQuantity() { return quantity; }
}