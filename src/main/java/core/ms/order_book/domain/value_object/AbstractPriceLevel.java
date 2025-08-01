package core.ms.order_book.domain.value_object;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.IOrder;
import core.ms.shared.domain.Money;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractPriceLevel<T extends IOrder> implements IPriceLevel {
    protected final LinkedList<T> orders; // Time-ordered queue
    protected final Money price;
    protected int orderCount;

    protected AbstractPriceLevel(Money price) {
        this.price = Objects.requireNonNull(price, "Price cannot be null");
        this.orders = new LinkedList<>();
        this.orderCount = 0;
    }

    // ============ COMMON OPERATIONS ============

    public void addOrder(T order) {
        Objects.requireNonNull(order, "Order cannot be null");
        validateOrderPrice(order);

        orders.addLast(order); // Time priority: first in, first out
        recalculateTotals();
    }

    public boolean removeOrder(T order) {
        Objects.requireNonNull(order, "Order cannot be null");
        boolean removed = orders.remove(order);
        if (removed) {
            recalculateTotals();
        }
        return removed;
    }

    public void removeInactiveOrders() {
        orders.removeIf(order -> !order.isActive());
        recalculateTotals();
    }

    // ============ QUERY METHODS ============

    public List<T> getOrders() {
        return new ArrayList<>(orders); // Defensive copy
    }

    public List<T> getActiveOrders() {
        return orders.stream()
                .filter(order -> order.isActive() && order.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
    }

    public Optional<T> getFirstOrder() {
        return orders.isEmpty() ? Optional.empty() : Optional.of(orders.getFirst());
    }

    public Optional<T> getFirstActiveOrder() {
        return getOrdersStream()
                .filter(order -> order.isActive() && order.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0)
                .findFirst();
    }

    @Override
    public boolean isEmpty() {
        return orders.isEmpty();
    }

    protected Stream<T> getOrdersStream() {
        return orders.stream();
    }

    // ============ INTERFACE IMPLEMENTATIONS ============

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

    // ============ PROTECTED METHODS ============

    protected void validateOrderPrice(IOrder order) {
        if (!price.equals(order.getPrice())) {
            throw new IllegalArgumentException(
                    "Order price " + order.getPrice() + " does not match level price " + price);
        }
    }

    protected BigDecimal calculateCurrentTotal() {
        return getOrdersStream()
                .filter(IOrder::isActive)
                .map(IOrder::getRemainingQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    protected void recalculateTotals() {
        orderCount = orders.size();
    }

    // ============ EQUALS/HASHCODE ============

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
}