package core.ms.order_book.domain.value_object;

import core.ms.order.domain.IBuyOrder;
import core.ms.order.domain.IOrder;
import core.ms.shared.domain.Money;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BidPriceLevel extends AbstractPriceLevel<IBuyOrder> {
    private final LinkedList<IBuyOrder> buyOrders; // Time-ordered queue

    public BidPriceLevel(Money price) {
        super(price);
        this.buyOrders = new LinkedList<>();
    }

    public void addOrder(IBuyOrder order) {
        Objects.requireNonNull(order, "Buy order cannot be null");
        validateOrderPrice(order);

        buyOrders.addLast(order); // Time priority: first in, first out
        recalculateTotals();
    }

    public boolean removeOrder(IBuyOrder order) {
        Objects.requireNonNull(order, "Buy order cannot be null");
        boolean removed = buyOrders.remove(order);
        if (removed) {
            recalculateTotals();
        }
        return removed;
    }

    public List<IBuyOrder> getOrders() {
        return new ArrayList<>(buyOrders); // Defensive copy
    }

    /**
     * Returns only active orders with remaining quantity > 0.
     * This ensures that inactive or fully filled orders don't appear in order lists.
     */
    public List<IBuyOrder> getActiveOrders() {
        return buyOrders.stream()
                .filter(order -> order.isActive() && order.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
    }
    @Override
    protected Stream<IBuyOrder> getOrdersStream() {
        return buyOrders.stream();
    }
    @Override
    public boolean isEmpty() {
        return buyOrders.isEmpty();
    }

    public Optional<IBuyOrder> getFirstOrder() {
        return buyOrders.isEmpty() ? Optional.empty() : Optional.of(buyOrders.getFirst());
    }

    @Override
    protected BigDecimal calculateCurrentTotal() {
        return buyOrders.stream()
                .filter(IOrder::isActive)
                .map(IBuyOrder::getRemainingQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    protected void recalculateTotals() {
        orderCount = buyOrders.size();
    }

    /**
     * Removes all inactive orders from this price level.
     * Should be called periodically to keep the order book clean.
     */
    public void removeInactiveOrders() {
        buyOrders.removeIf(order -> !order.isActive());
        recalculateTotals();
    }
}