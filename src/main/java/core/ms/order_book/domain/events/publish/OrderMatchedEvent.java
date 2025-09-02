package core.ms.order_book.domain.events.publish;

import core.ms.shared.events.BaseEvent;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.util.Objects;

public class OrderMatchedEvent extends BaseEvent {
    private final String buyOrderId;
    private final String sellOrderId;
    private final Symbol symbol;
    private final BigDecimal matchedQuantity;
    private final Money executionPrice;

    public OrderMatchedEvent(String correlationId, String buyOrderId, String sellOrderId,
                             Symbol symbol, BigDecimal matchedQuantity, Money executionPrice) {
        super(correlationId, "ORDER_BOOK_BC");
        this.buyOrderId = Objects.requireNonNull(buyOrderId, "Buy order ID cannot be null");
        this.sellOrderId = Objects.requireNonNull(sellOrderId, "Sell order ID cannot be null");
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");
        this.matchedQuantity = Objects.requireNonNull(matchedQuantity, "Matched quantity cannot be null");
        this.executionPrice = Objects.requireNonNull(executionPrice, "Execution price cannot be null");
    }

    // Getters
    public String getBuyOrderId() { return buyOrderId; }
    public String getSellOrderId() { return sellOrderId; }
    public Symbol getSymbol() { return symbol; }
    public BigDecimal getMatchedQuantity() { return matchedQuantity; }
    public Money getExecutionPrice() { return executionPrice; }

    public Money getTotalValue() {
        return executionPrice.multiply(matchedQuantity);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OrderMatchedEvent that = (OrderMatchedEvent) obj;
        return Objects.equals(buyOrderId, that.buyOrderId) &&
                Objects.equals(sellOrderId, that.sellOrderId) &&
                Objects.equals(symbol, that.symbol) &&
                Objects.equals(matchedQuantity, that.matchedQuantity) &&
                Objects.equals(executionPrice, that.executionPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(buyOrderId, sellOrderId, symbol, matchedQuantity, executionPrice);
    }

    @Override
    public String toString() {
        return String.format("OrderMatchedEvent{correlationId='%s', buyOrderId='%s', sellOrderId='%s', " +
                        "symbol='%s', matchedQuantity=%s, executionPrice=%s, totalValue=%s}",
                getCorrelationId(), buyOrderId, sellOrderId, symbol, matchedQuantity,
                executionPrice, getTotalValue());
    }
}