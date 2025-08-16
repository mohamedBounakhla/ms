package core.ms.order.domain.events;

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
    private final LocalDateTime occurredAt;

    public OrderCreatedEvent(String orderId, String portfolioId, String reservationId,
                             String orderType, Symbol symbol, Money price, BigDecimal quantity) {
        this.orderId = orderId;
        this.portfolioId = portfolioId;
        this.reservationId = reservationId;
        this.orderType = orderType;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
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
    @Override
    public LocalDateTime getOccurredAt() { return occurredAt; }
} 