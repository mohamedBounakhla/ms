package core.ms.order_book.domain.value_object;

import core.ms.order.domain.IBuyOrder;
import core.ms.shared.domain.Money;

import java.math.BigDecimal;
import java.util.*;

public class BidPriceLevel extends AbstractPriceLevel {
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

    @Override
    public boolean isEmpty() {
        return buyOrders.isEmpty();
    }

    public Optional<IBuyOrder> getFirstOrder() {
        return buyOrders.isEmpty() ? Optional.empty() : Optional.of(buyOrders.getFirst());
    }

    @Override
    protected void recalculateTotals() {
        totalQuantity = buyOrders.stream()
                .map(IBuyOrder::getRemainingQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        orderCount = buyOrders.size();
    }
}