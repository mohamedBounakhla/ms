package core.ms.order.domain.events.publish;

import core.ms.shared.events.BaseEvent;
import core.ms.shared.events.DomainEvent;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderCreatedEvent extends BaseEvent {
    private final String orderId;
    private final String portfolioId;
    private final String reservationId;
    private final String orderType; // "BUY" or "SELL"
    private final String symbolCode;
    private final BigDecimal price;
    private final Currency currency;
    private final BigDecimal quantity;
    private final String status;

    public OrderCreatedEvent(String correlationId, String orderId, String portfolioId,
                             String reservationId, String orderType, String symbolCode,
                             BigDecimal price, Currency currency, BigDecimal quantity,
                             String status) {
        super(correlationId, "ORDER_BC");
        this.orderId = orderId;
        this.portfolioId = portfolioId;
        this.reservationId = reservationId;
        this.orderType = orderType;
        this.symbolCode = symbolCode;
        this.price = price;
        this.currency = currency;
        this.quantity = quantity;
        this.status = status;
    }

    // Getters
    public String getOrderId() { return orderId; }
    public String getPortfolioId() { return portfolioId; }
    public String getReservationId() { return reservationId; }
    public String getOrderType() { return orderType; }
    public String getSymbolCode() { return symbolCode; }
    public BigDecimal getPrice() { return price; }
    public Currency getCurrency() { return currency; }
    public BigDecimal getQuantity() { return quantity; }
    public String getStatus() { return status; }
}