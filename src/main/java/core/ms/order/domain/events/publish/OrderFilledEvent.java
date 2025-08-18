package core.ms.order.domain.events.publish;

import core.ms.shared.events.DomainEvent;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderFilledEvent implements DomainEvent {
    private final String orderId;
    private final String portfolioId;
    private final String orderType;
    private final Symbol symbol;
    private final BigDecimal filledQuantity;
    private final Money averageExecutionPrice;
    private final LocalDateTime occurredAt;

    public OrderFilledEvent(String orderId, String portfolioId, String orderType,
                            Symbol symbol, BigDecimal filledQuantity,
                            Money averageExecutionPrice) {
        this.orderId = orderId;
        this.portfolioId = portfolioId;
        this.orderType = orderType;
        this.symbol = symbol;
        this.filledQuantity = filledQuantity;
        this.averageExecutionPrice = averageExecutionPrice;
        this.occurredAt = LocalDateTime.now();
    }

    // All getters
    public String getOrderId() { return orderId; }
    public String getPortfolioId() { return portfolioId; }
    public String getOrderType() { return orderType; }
    public Symbol getSymbol() { return symbol; }
    public BigDecimal getFilledQuantity() { return filledQuantity; }
    public Money getAverageExecutionPrice() { return averageExecutionPrice; }
    @Override
    public LocalDateTime getOccurredAt() { return occurredAt; }
}