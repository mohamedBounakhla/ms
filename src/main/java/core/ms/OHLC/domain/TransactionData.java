package core.ms.OHLC.domain;

import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Simple data container for transaction information used by factory
 * This is NOT a domain entity, just a data transfer object
 */
public class TransactionData {
    private final Instant timestamp;
    private final Money price;
    private final BigDecimal quantity;
    private final Symbol symbol;

    public TransactionData(Instant timestamp, Money price, BigDecimal quantity, Symbol symbol) {
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.price = Objects.requireNonNull(price, "Price cannot be null");
        this.quantity = Objects.requireNonNull(quantity, "Quantity cannot be null");
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }

    public Instant getTimestamp() { return timestamp; }
    public Money getPrice() { return price; }
    public BigDecimal getQuantity() { return quantity; }
    public Symbol getSymbol() { return symbol; }

    public Money getTotalValue() {
        return price.multiply(quantity);
    }

    @Override
    public String toString() {
        return String.format("TransactionData[%s: %s %s @ %s]",
                timestamp, quantity.toPlainString(), symbol.getCode(), price.toDisplayString());
    }
}
