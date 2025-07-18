package core.ms.order_book.domain.value_object;

import core.ms.order.domain.entities.ISellOrder;
import core.ms.shared.domain.Money;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AskPriceLevel extends AbstractPriceLevel<ISellOrder> {
    private final LinkedList<ISellOrder> sellOrders; // Time-ordered queue

    public AskPriceLevel(Money price) {
        super(price);
        this.sellOrders = new LinkedList<>();
    }

    public void addOrder(ISellOrder order) {
        Objects.requireNonNull(order, "Sell order cannot be null");
        validateOrderPrice(order);

        sellOrders.addLast(order); // Time priority: first in, first out
        recalculateTotals();
    }

    public boolean removeOrder(ISellOrder order) {
        Objects.requireNonNull(order, "Sell order cannot be null");
        boolean removed = sellOrders.remove(order);
        if (removed) {
            recalculateTotals();
        }
        return removed;
    }

    @Override
    protected Stream<ISellOrder> getOrdersStream() {
        return sellOrders.stream();
    }
    public List<ISellOrder> getOrders() {
        return new ArrayList<>(sellOrders); // Defensive copy
    }

    /**
     * Returns only active orders with remaining quantity > 0.
     * This ensures that inactive or fully filled orders don't appear in order lists.
     */
    public List<ISellOrder> getActiveOrders() {
        return sellOrders.stream()
                .filter(order -> order.isActive() && order.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isEmpty() {
        return sellOrders.isEmpty();
    }

    public Optional<ISellOrder> getFirstOrder() {
        return sellOrders.isEmpty() ? Optional.empty() : Optional.of(sellOrders.getFirst());
    }


    @Override
    protected void recalculateTotals() {
        orderCount = sellOrders.size();
    }

    /**
     * Removes all inactive orders from this price level.
     * Should be called periodically to keep the order book clean.
     */
    public void removeInactiveOrders() {
        sellOrders.removeIf(order -> !order.isActive());
        recalculateTotals();
    }
}