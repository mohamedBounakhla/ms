package core.ms.order_book.domain.value_object;

import core.ms.order.domain.entities.IOrder;
import core.ms.shared.money.Money;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractPriceLevelManager<
        T extends IOrder,
        L extends AbstractPriceLevel<T>
        > {
    protected final TreeMap<Money, L> levels;
    protected final IPriorityCalculator<T> priorityCalculator;

    protected AbstractPriceLevelManager(IPriorityCalculator<T> priorityCalculator) {
        this.priorityCalculator = Objects.requireNonNull(priorityCalculator, "Priority calculator cannot be null");

        // Create TreeMap with custom comparator based on priority calculator
        this.levels = new TreeMap<>((price1, price2) -> {
            if (priorityCalculator.isPriceBetter(price2, price1)) return 1;
            if (priorityCalculator.isPriceBetter(price1, price2)) return -1;
            return 0;
        });
    }

    // ============ CORE OPERATIONS ============
    protected abstract L createPriceLevel(Money price);

    public void addOrder(T order) {
        Objects.requireNonNull(order, "Order cannot be null");

        Money price = order.getPrice();
        L level = levels.computeIfAbsent(price, this::createPriceLevel);
        level.addOrder(order);
    }

    public boolean removeOrder(T order) {
        Objects.requireNonNull(order, "Order cannot be null");

        Money price = order.getPrice();
        L level = levels.get(price);

        if (level != null && level.removeOrder(order)) {
            if (level.isEmpty()) {
                levels.remove(price);
            }
            return true;
        }
        return false;
    }



    public void removeInactiveOrders() {
        Iterator<Map.Entry<Money, L>> iterator = levels.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Money, L> entry = iterator.next();
            L level = entry.getValue();
            level.removeInactiveOrders();
            if (level.isEmpty()) {
                iterator.remove();
            }
        }
    }

    // ============ QUERY METHODS ============

    public Optional<Money> getBestPrice() {
        return levels.isEmpty() ? Optional.empty() : Optional.of(levels.firstKey());
    }

    public Optional<T> getBestOrder() {
        return levels.values().stream()
                .map(AbstractPriceLevel::getFirstActiveOrder)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    public BigDecimal getTotalVolume() {
        return levels.values().stream()
                .map(AbstractPriceLevel::getTotalQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Collection<L> getLevels() {
        return new ArrayList<>(levels.values());
    }

    public List<L> getTopLevels(int count) {
        return levels.values().stream()
                .limit(count)
                .collect(Collectors.toList());
    }

    public boolean isEmpty() {
        return levels.isEmpty();
    }

    public int getLevelCount() {
        return levels.size();
    }

    // ============ TREEMAP ACCESS ============

    public TreeMap<Money, L> getTreeMap() {
        return levels;
    }
}

