package core.ms.order_book.domain.value_object;

import core.ms.order.domain.IOrder;
import core.ms.shared.domain.Money;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public abstract class AbstractPriceLevel<T extends IOrder> implements IPriceLevel {
    protected final Money price;
    protected int orderCount;

    protected AbstractPriceLevel(Money price) {
        this.price = Objects.requireNonNull(price, "Price cannot be null");
        this.orderCount = 0;
    }

    @Override
    public Money getPrice() {
        return price;
    }

    @Override
    public BigDecimal getTotalQuantity() {
        return calculateCurrentTotal();
    }

    @Override
    public int getOrderCount() {
        return orderCount;
    }

    @Override
    public boolean hasQuantity(BigDecimal quantity) {
        return getTotalQuantity().compareTo(quantity) >= 0;
    }

    protected void validateOrderPrice(IOrder order) {
        if (!price.equals(order.getPrice())) {
            throw new IllegalArgumentException(
                    "Order price " + order.getPrice() + " does not match level price " + price);
        }
    }

    /**
     * Calculates the current total quantity from all orders in this level.
     * This method is called every time getTotalQuantity() is invoked to ensure
     * the total always reflects the current state of orders, including any
     * quantity changes due to transactions.
     */
    protected BigDecimal calculateCurrentTotal() {
        // Basic business rule: price level shows quantity of all orders at this price
        return getOrdersStream()
                .map(IOrder::getRemainingQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Updates cached values that don't change during transactions.
     * Currently only updates order count since total quantity is calculated dynamically.
     */
    protected abstract void recalculateTotals();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AbstractPriceLevel<?> that = (AbstractPriceLevel<?>) obj;
        return Objects.equals(price, that.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(price);
    }
    protected abstract Stream<T> getOrdersStream();

    public Optional<T> getFirstActiveOrder() {
        return getOrdersStream()
                .filter(order -> order.isActive() && order.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0)
                .findFirst();
    }
}