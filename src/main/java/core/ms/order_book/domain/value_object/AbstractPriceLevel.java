package core.ms.order_book.domain.value_object;

import core.ms.order.domain.IOrder;
import core.ms.shared.domain.Money;

import java.math.BigDecimal;
import java.util.Objects;

public abstract class AbstractPriceLevel implements IPriceLevel {
    protected final Money price;
    protected BigDecimal totalQuantity;
    protected int orderCount;

    protected AbstractPriceLevel(Money price) {
        this.price = Objects.requireNonNull(price, "Price cannot be null");
        this.totalQuantity = BigDecimal.ZERO;
        this.orderCount = 0;
    }

    @Override
    public Money getPrice() {
        return price;
    }

    @Override
    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    @Override
    public int getOrderCount() {
        return orderCount;
    }

    @Override
    public boolean hasQuantity(BigDecimal quantity) {
        return totalQuantity.compareTo(quantity) >= 0;
    }

    protected void validateOrderPrice(IOrder order) {
        if (!price.equals(order.getPrice())) {
            throw new IllegalArgumentException(
                    "Order price " + order.getPrice() + " does not match level price " + price);
        }
    }

    protected abstract void recalculateTotals();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AbstractPriceLevel that = (AbstractPriceLevel) obj;
        return Objects.equals(price, that.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(price);
    }
}
