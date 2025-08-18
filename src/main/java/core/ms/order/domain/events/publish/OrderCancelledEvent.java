package core.ms.order.domain.events.publish;

import core.ms.shared.events.DomainEvent;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderCancelledEvent implements DomainEvent {
    private final String orderId;
    private final String portfolioId;
    private final String reservationId;
    private final String orderType;
    private final Symbol symbol;
    private final BigDecimal cancelledQuantity;
    private final BigDecimal remainingQuantity;
    private final String reason;
    private final LocalDateTime occurredAt;

    public OrderCancelledEvent(String orderId, String portfolioId, String reservationId,
                               String orderType, Symbol symbol, BigDecimal cancelledQuantity,
                               BigDecimal remainingQuantity, String reason) {
        this.orderId = orderId;
        this.portfolioId = portfolioId;
        this.reservationId = reservationId;
        this.orderType = orderType;
        this.symbol = symbol;
        this.cancelledQuantity = cancelledQuantity;
        this.remainingQuantity = remainingQuantity;
        this.reason = reason;
        this.occurredAt = LocalDateTime.now();
    }

    // All getters
    public String getOrderId() { return orderId; }
    public String getPortfolioId() { return portfolioId; }
    public String getReservationId() { return reservationId; }
    public String getOrderType() { return orderType; }
    public Symbol getSymbol() { return symbol; }
    public BigDecimal getCancelledQuantity() { return cancelledQuantity; }
    public BigDecimal getRemainingQuantity() { return remainingQuantity; }
    public String getReason() { return reason; }
    @Override
    public LocalDateTime getOccurredAt() { return occurredAt; }
}