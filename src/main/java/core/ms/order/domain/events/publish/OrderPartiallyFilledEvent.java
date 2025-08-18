package core.ms.order.domain.events.publish;

import core.ms.shared.events.DomainEvent;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderPartiallyFilledEvent implements DomainEvent {
    private final String orderId;
    private final String portfolioId;
    private final String orderType;
    private final Symbol symbol;
    private final BigDecimal filledQuantity;
    private final BigDecimal totalFilledQuantity;
    private final BigDecimal remainingQuantity;
    private final Money executionPrice;
    private final LocalDateTime occurredAt;

    public OrderPartiallyFilledEvent(String orderId, String portfolioId, String orderType,
                                     Symbol symbol, BigDecimal filledQuantity,
                                     BigDecimal totalFilledQuantity, BigDecimal remainingQuantity,
                                     Money executionPrice) {
        this.orderId = orderId;
        this.portfolioId = portfolioId;
        this.orderType = orderType;
        this.symbol = symbol;
        this.filledQuantity = filledQuantity;
        this.totalFilledQuantity = totalFilledQuantity;
        this.remainingQuantity = remainingQuantity;
        this.executionPrice = executionPrice;
        this.occurredAt = LocalDateTime.now();
    }

    // All getters
    public String getOrderId() { return orderId; }
    public String getPortfolioId() { return portfolioId; }
    public String getOrderType() { return orderType; }
    public Symbol getSymbol() { return symbol; }
    public BigDecimal getFilledQuantity() { return filledQuantity; }
    public BigDecimal getTotalFilledQuantity() { return totalFilledQuantity; }
    public BigDecimal getRemainingQuantity() { return remainingQuantity; }
    public Money getExecutionPrice() { return executionPrice; }
    @Override
    public LocalDateTime getOccurredAt() { return occurredAt; }
}