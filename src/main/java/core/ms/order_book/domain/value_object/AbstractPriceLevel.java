package core.ms.order_book.domain.value_object;

import core.ms.order.domain.entities.IOrder;
import core.ms.shared.money.Money;

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

        // Check if order is valid to add
        if (!order.isActive() || order.getRemainingQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("DEBUG AbstractPriceLevel: Rejecting inactive/fully executed order: " +
                    order.getId());
            return;
        }

        // Check if order already exists (avoid duplicates)
        if (orders.stream().anyMatch(o -> o.getId().equals(order.getId()))) {
            System.out.println("DEBUG AbstractPriceLevel: Order already exists in level: " + order.getId());
            return;
        }

        orders.addLast(order); // Time priority: first in, first out
        recalculateTotals();

        System.out.println("DEBUG AbstractPriceLevel: Added order " + order.getId() +
                " to price level " + price + ". Level now has " + orders.size() + " orders");
    }

    public boolean removeOrder(T order) {
        Objects.requireNonNull(order, "Order cannot be null");
        boolean removed = orders.removeIf(o -> o.getId().equals(order.getId()));
        if (removed) {
            recalculateTotals();
            System.out.println("DEBUG AbstractPriceLevel: Removed order " + order.getId() +
                    " from price level " + price);
        }
        return removed;
    }

    public void removeInactiveOrders() {
        int beforeSize = orders.size();
        orders.removeIf(order -> !order.isActive() ||
                order.getRemainingQuantity().compareTo(BigDecimal.ZERO) <= 0);
        int afterSize = orders.size();

        if (beforeSize != afterSize) {
            System.out.println("DEBUG AbstractPriceLevel: Removed " + (beforeSize - afterSize) +
                    " inactive orders from level " + price);
            recalculateTotals();
        }
    }

    // ============ QUERY METHODS ============

    public List<T> getOrders() {
        return new ArrayList<>(orders); // Defensive copy
    }

    public List<T> getActiveOrders() {
        List<T> activeOrders = orders.stream()
                .filter(order -> order.isActive() &&
                        order.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        System.out.println("DEBUG AbstractPriceLevel: Price " + price +
                " has " + activeOrders.size() + " active orders out of " +
                orders.size() + " total");

        return activeOrders;
    }

    public Optional<T> getFirstOrder() {
        return orders.isEmpty() ? Optional.empty() : Optional.of(orders.getFirst());
    }

    public Optional<T> getFirstActiveOrder() {
        return getOrdersStream()
                .filter(order -> order.isActive() &&
                        order.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0)
                .findFirst();
    }

    @Override
    public boolean isEmpty() {
        // Consider level empty if no active orders with remaining quantity
        return getActiveOrders().isEmpty();
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
        BigDecimal total = getOrdersStream()
                .filter(IOrder::isActive)
                .map(IOrder::getRemainingQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total;
    }

    protected void recalculateTotals() {
        orderCount = (int) orders.stream()
                .filter(order -> order.isActive() &&
                        order.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0)
                .count();
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

    @Override
    public String toString() {
        return String.format("PriceLevel[%s, orders=%d, total=%s]",
                price, orders.size(), getTotalQuantity());
    }
}