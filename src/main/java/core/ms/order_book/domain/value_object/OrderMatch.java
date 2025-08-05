package core.ms.order_book.domain.value_object;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.shared.money.Money;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class OrderMatch {
    private final IBuyOrder buyOrder;
    private final ISellOrder sellOrder;
    private final BigDecimal quantity;
    private final Money executionPrice;
    private final LocalDateTime timestamp;

    public OrderMatch(IBuyOrder buyOrder, ISellOrder sellOrder, BigDecimal quantity, Money executionPrice) {
        this.buyOrder = Objects.requireNonNull(buyOrder, "Buy order cannot be null");
        this.sellOrder = Objects.requireNonNull(sellOrder, "Sell order cannot be null");
        this.quantity = Objects.requireNonNull(quantity, "Quantity cannot be null");
        this.executionPrice = Objects.requireNonNull(executionPrice, "Execution price cannot be null");
        this.timestamp = LocalDateTime.now();
    }

    // ============ GETTERS (PURE DATA ACCESS) ============

    public IBuyOrder getBuyOrder() {
        return buyOrder;
    }

    public ISellOrder getSellOrder() {
        return sellOrder;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public Money getExecutionPrice() {
        return executionPrice;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Money getTotalValue() {
        return executionPrice.multiply(quantity);
    }

    public boolean isValid() {
        return quantity.compareTo(BigDecimal.ZERO) > 0 &&
                buyOrder.isActive() &&
                sellOrder.isActive();
    }

    @Override
    public String toString() {
        return String.format("OrderMatch{buy=%s, sell=%s, quantity=%s, price=%s, value=%s, timestamp=%s}",
                buyOrder.getId(), sellOrder.getId(), quantity, executionPrice, getTotalValue(), timestamp);
    }
}