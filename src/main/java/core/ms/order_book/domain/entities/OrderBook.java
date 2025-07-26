package core.ms.order_book.domain.entities;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order_book.domain.factory.OrderMatchFactory;
import core.ms.order_book.domain.value_object.*;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class OrderBook {
    private final Symbol symbol;
    private final TreeMap<Money, BidPriceLevel> bidLevels;
    private final TreeMap<Money, AskPriceLevel> askLevels;
    private final Map<String, IOrder> orderIndex;
    private LocalDateTime lastUpdate;
    private BigDecimal totalBidVolume;
    private BigDecimal totalAskVolume;

    private final BuyOrderPriorityCalculator buyOrderCalculator;
    private final SellOrderPriorityCalculator sellOrderCalculator;

    public OrderBook(Symbol symbol) {
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");

        this.buyOrderCalculator = new BuyOrderPriorityCalculator();
        this.sellOrderCalculator = new SellOrderPriorityCalculator();

        this.bidLevels = new TreeMap<>((price1, price2) -> {
            if (buyOrderCalculator.isPriceBetter(price2, price1)) return 1;
            if (buyOrderCalculator.isPriceBetter(price1, price2)) return -1;
            return 0;
        });

        this.askLevels = new TreeMap<>((price1, price2) -> {
            if (sellOrderCalculator.isPriceBetter(price1, price2)) return -1;
            if (sellOrderCalculator.isPriceBetter(price2, price1)) return 1;
            return 0;
        });

        this.orderIndex = new HashMap<>();
        this.lastUpdate = LocalDateTime.now();
        this.totalBidVolume = BigDecimal.ZERO;
        this.totalAskVolume = BigDecimal.ZERO;
    }

    public void addOrder(IOrder order) {
        orderIndex.put(order.getId(), order);

        Money price = order.getPrice();

        if (order instanceof IBuyOrder) {
            BidPriceLevel level = bidLevels.computeIfAbsent(price, BidPriceLevel::new);
            level.addOrder((IBuyOrder) order);
        } else if (order instanceof ISellOrder) {
            AskPriceLevel level = askLevels.computeIfAbsent(price, AskPriceLevel::new);
            level.addOrder((ISellOrder) order);
        } else {
            throw new IllegalArgumentException("Unknown order type: " + order.getClass());
        }

        updateVolumeMetrics();
        lastUpdate = LocalDateTime.now();
    }

    public boolean removeOrder(IOrder order) {
        Objects.requireNonNull(order, "Order cannot be null");

        if (orderIndex.remove(order.getId()) != null) {
            Money price = order.getPrice();

            if (order instanceof IBuyOrder) {
                BidPriceLevel level = bidLevels.get(price);
                if (level != null) {
                    level.removeOrder((IBuyOrder) order);
                    if (level.isEmpty()) {
                        bidLevels.remove(price);
                    }
                }
            } else if (order instanceof ISellOrder) {
                AskPriceLevel level = askLevels.get(price);
                if (level != null) {
                    level.removeOrder((ISellOrder) order);
                    if (level.isEmpty()) {
                        askLevels.remove(price);
                    }
                }
            } else {
                throw new IllegalArgumentException("Unknown order type: " + order.getClass());
            }

            updateVolumeMetrics();
            lastUpdate = LocalDateTime.now();
            return true;
        }
        return false;
    }

    /**
     * Removes all inactive (filled or cancelled) orders from the order book.
     * This method should be called after transactions to maintain a clean order book
     * containing only tradeable orders.
     */
    public void removeInactiveOrders() {
        // Collect inactive orders to avoid concurrent modification
        List<IOrder> inactiveOrders = orderIndex.values().stream()
                .filter(order -> !order.isActive())
                .collect(Collectors.toList());

        // Remove each inactive order
        for (IOrder inactiveOrder : inactiveOrders) {
            removeOrder(inactiveOrder);
        }
    }

    // ============ PURE DATA ACCESS METHODS ============
    // The OrderBook now focuses solely on providing data

    public Optional<Money> getBestBid() {
        return bidLevels.isEmpty() ? Optional.empty() : Optional.of(bidLevels.firstKey());
    }

    public Optional<Money> getBestAsk() {
        return askLevels.isEmpty() ? Optional.empty() : Optional.of(askLevels.firstKey());
    }

    public Optional<Money> getSpread() {
        Optional<Money> bestBid = getBestBid();
        Optional<Money> bestAsk = getBestAsk();

        if (bestBid.isPresent() && bestAsk.isPresent()) {
            return Optional.of(bestAsk.get().subtract(bestBid.get()));
        }
        return Optional.empty();
    }

    public Optional<IBuyOrder> getBestBuyOrder() {
        return bidLevels.values().stream()
                .map(BidPriceLevel::getFirstActiveOrder)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    public Optional<ISellOrder> getBestSellOrder() {
        return askLevels.values().stream()
                .map(AskPriceLevel::getFirstActiveOrder)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    // ============ MATCHING DELEGATION ============
    // OrderBook delegates ALL matching logic to the factory

    /**
     * Finds matches using the default strategy.
     * The OrderBook simply passes itself to the factory and doesn't care about the details.
     */
    public List<OrderMatch> findMatches() {
        // Clean up inactive orders before finding matches
        removeInactiveOrders();
        return OrderMatchFactory.findMatches(this);
    }

    /**
     * Finds matches using a specific strategy.
     * Still completely delegated to the factory.
     */
    public List<OrderMatch> findMatches(MatchingStrategy strategy) {
        removeInactiveOrders();
        return OrderMatchFactory.findMatches(this, strategy);
    }

    /**
     * Convenience method to check if there are any potential matches.
     * Uses the factory to determine this.
     */
    public boolean hasMatches() {
        return !findMatches().isEmpty();
    }

    // ============ DATA METHODS ============

    public MarketDepth getMarketDepth(int levels) {
        if (levels <= 0) {
            throw new IllegalArgumentException("Levels must be positive");
        }

        List<BidPriceLevel> topBids = bidLevels.values().stream()
                .limit(levels)
                .collect(Collectors.toList());

        List<AskPriceLevel> topAsks = askLevels.values().stream()
                .limit(levels)
                .collect(Collectors.toList());

        return new MarketDepth(symbol, topBids, topAsks);
    }

    public Collection<BidPriceLevel> getBidLevels() {
        return new ArrayList<>(bidLevels.values());
    }

    public Collection<AskPriceLevel> getAskLevels() {
        return new ArrayList<>(askLevels.values());
    }

    public BigDecimal getTotalBidVolume() {
        return totalBidVolume;
    }

    public BigDecimal getTotalAskVolume() {
        return totalAskVolume;
    }

    public boolean isEmpty() {
        return bidLevels.isEmpty() && askLevels.isEmpty();
    }

    public boolean hasOrders() {
        return !isEmpty();
    }

    public int getOrderCount() {
        return orderIndex.size();
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    private void updateVolumeMetrics() {
        totalBidVolume = bidLevels.values().stream()
                .map(BidPriceLevel::getTotalQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalAskVolume = askLevels.values().stream()
                .map(AskPriceLevel::getTotalQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}