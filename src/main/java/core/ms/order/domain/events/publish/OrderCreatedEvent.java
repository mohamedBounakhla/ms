package core.ms.order.domain.events.publish;

import core.ms.shared.events.DomainEvent;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderCreatedEvent implements DomainEvent {
    private final String orderId;
    private final String portfolioId;
    private final String reservationId;
    private final String orderType; // "BUY" or "SELL"
    private final Symbol symbol;
    private final Money price;
    private final BigDecimal quantity;
    private final String status;
    private final LocalDateTime occurredAt;

    public OrderCreatedEvent(String orderId, String portfolioId, String reservationId,
                             String orderType, Symbol symbol, Money price,
                             BigDecimal quantity, String status) {
        this.orderId = orderId;
        this.portfolioId = portfolioId;
        this.reservationId = reservationId;
        this.orderType = orderType;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.status = status;
        this.occurredAt = LocalDateTime.now();
    }

    // All getters
    public String getOrderId() { return orderId; }
    public String getPortfolioId() { return portfolioId; }
    public String getReservationId() { return reservationId; }
    public String getOrderType() { return orderType; }
    public Symbol getSymbol() { return symbol; }
    public Money getPrice() { return price; }
    public BigDecimal getQuantity() { return quantity; }
    public String getStatus() { return status; }
    @Override
    public LocalDateTime getOccurredAt() { return occurredAt; }
}