package core.ms.order.domain.entities;

import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Pure transaction entity - NO business rules, NO validation.
 * All validation is handled by the factory/builder pattern.
 */
public abstract class AbstractTransaction implements ITransaction {
    protected final String id;
    protected final Symbol symbol;
    protected final IBuyOrder buyOrder;
    protected final ISellOrder sellOrder;
    protected final Money price;
    protected final BigDecimal quantity;
    protected final LocalDateTime createdAt;

    protected AbstractTransaction(String id, Symbol symbol, IBuyOrder buyOrder,
                                  ISellOrder sellOrder, BigDecimal quantity) {
        this.id = id;
        this.symbol = symbol;
        this.buyOrder = buyOrder;
        this.sellOrder = sellOrder;
        this.price = sellOrder.getPrice();
        this.quantity = quantity;
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public String getId() { return id; }

    @Override
    public Symbol getSymbol() { return symbol; }

    @Override
    public IBuyOrder getBuyOrder() { return buyOrder; }

    @Override
    public ISellOrder getSellOrder() { return sellOrder; }

    @Override
    public Money getPrice() { return price; }

    @Override
    public BigDecimal getQuantity() { return quantity; }

    @Override
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public Money getTotalValue() {
        return price.multiply(quantity);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AbstractTransaction that = (AbstractTransaction) obj;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("%s[%s, %s, %s @ %s]",
                getClass().getSimpleName(), id, symbol.getFullSymbol(),
                quantity, price);
    }
}