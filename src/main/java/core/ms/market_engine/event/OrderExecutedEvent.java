package core.ms.market_engine.event;

import core.ms.shared.money.Money;

import java.math.BigDecimal;
import java.util.Objects;

public class OrderExecutedEvent extends DomainEvent {
    private final String orderId;
    private final BigDecimal executedQuantity;
    private final BigDecimal remainingQuantity;
    private final Money executionPrice;

    public OrderExecutedEvent(String orderId, BigDecimal executedQuantity,
                              BigDecimal remainingQuantity, Money executionPrice, String engineId) {
        super(engineId);
        this.orderId = Objects.requireNonNull(orderId, "Order ID cannot be null");
        this.executedQuantity = Objects.requireNonNull(executedQuantity, "Executed quantity cannot be null");
        this.remainingQuantity = Objects.requireNonNull(remainingQuantity, "Remaining quantity cannot be null");
        this.executionPrice = Objects.requireNonNull(executionPrice, "Execution price cannot be null");
    }

    public String getOrderId() {
        return orderId;
    }

    public BigDecimal getExecutedQuantity() {
        return executedQuantity;
    }

    public BigDecimal getRemainingQuantity() {
        return remainingQuantity;
    }

    public Money getExecutionPrice() {
        return executionPrice;
    }

    public boolean isFullyExecuted() {
        return remainingQuantity.compareTo(BigDecimal.ZERO) == 0;
    }

    @Override
    public String toString() {
        return "OrderExecutedEvent{" +
                "orderId='" + orderId + '\'' +
                ", executedQuantity=" + executedQuantity +
                ", remainingQuantity=" + remainingQuantity +
                ", executionPrice=" + executionPrice.toPlainString() +
                ", fullyExecuted=" + isFullyExecuted() +
                '}';
    }
}