package core.ms.order_book.domain.events.publish;

import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class OrderMatchedEvent {
    private final String buyOrderId;
    private final String sellOrderId;
    private final Symbol symbol;
    private final BigDecimal quantity;
    private final Money executionPrice;
    private final LocalDateTime occurredAt;

    public OrderMatchedEvent(String buyOrderId, String sellOrderId, Symbol symbol,
                             BigDecimal quantity, Money executionPrice, LocalDateTime occurredAt) {
        this.buyOrderId = Objects.requireNonNull(buyOrderId, "Buy order ID cannot be null");
        this.sellOrderId = Objects.requireNonNull(sellOrderId, "Sell order ID cannot be null");
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");  // ✅ Symbol validation
        this.quantity = Objects.requireNonNull(quantity, "Quantity cannot be null");
        this.executionPrice = Objects.requireNonNull(executionPrice, "Execution price cannot be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurred at cannot be null");
    }

    public String getBuyOrderId() { return buyOrderId; }
    public String getSellOrderId() { return sellOrderId; }
    public Symbol getSymbol() { return symbol; }  // ✅ Return Symbol object
    public BigDecimal getQuantity() { return quantity; }
    public Money getExecutionPrice() { return executionPrice; }
    public LocalDateTime getOccurredAt() { return occurredAt; }

    public Money getTotalValue() {
        return executionPrice.multiply(quantity);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OrderMatchedEvent that = (OrderMatchedEvent) obj;
        return Objects.equals(buyOrderId, that.buyOrderId) &&
                Objects.equals(sellOrderId, that.sellOrderId) &&
                Objects.equals(symbol, that.symbol) &&
                Objects.equals(quantity, that.quantity) &&
                Objects.equals(executionPrice, that.executionPrice) &&
                Objects.equals(occurredAt, that.occurredAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(buyOrderId, sellOrderId, symbol, quantity, executionPrice, occurredAt);
    }

    @Override
    public String toString() {
        return String.format("OrderMatchedEvent{buyOrderId='%s', sellOrderId='%s', symbol='%s', " +
                        "quantity=%s, executionPrice=%s, totalValue=%s, occurredAt=%s}",
                buyOrderId, sellOrderId, symbol, quantity, executionPrice, getTotalValue(), occurredAt);
    }
}