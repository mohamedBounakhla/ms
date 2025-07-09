package core.ms.order_book.domain.value_object;

import core.ms.order.domain.ISellOrder;
import core.ms.shared.domain.Money;

import java.math.BigDecimal;
import java.util.*;

public class AskPriceLevel extends AbstractPriceLevel {
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

    public List<ISellOrder> getOrders() {
        return new ArrayList<>(sellOrders); // Defensive copy
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
        totalQuantity = sellOrders.stream()
                .map(ISellOrder::getRemainingQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        orderCount = sellOrders.size();
    }
}