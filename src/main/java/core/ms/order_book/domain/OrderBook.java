package core.ms.order_book.domain;

import core.ms.order.domain.IBuyOrder;
import core.ms.order.domain.IOrder;
import core.ms.order.domain.ISellOrder;
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
    private final MatchFinder matchFinder;

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
        this.matchFinder = new MatchFinder();
    }

    public void addOrder(IBuyOrder order) {
        Objects.requireNonNull(order, "Buy order cannot be null");
        validateOrder(order);

        orderIndex.put(order.getId(), order);

        Money price = order.getPrice();
        BidPriceLevel level = bidLevels.computeIfAbsent(price, BidPriceLevel::new);
        level.addOrder(order);

        updateVolumeMetrics();
        lastUpdate = LocalDateTime.now();
    }

    public void addOrder(ISellOrder order) {
        Objects.requireNonNull(order, "Sell order cannot be null");
        validateOrder(order);

        orderIndex.put(order.getId(), order);

        Money price = order.getPrice();
        AskPriceLevel level = askLevels.computeIfAbsent(price, AskPriceLevel::new);
        level.addOrder(order);

        updateVolumeMetrics();
        lastUpdate = LocalDateTime.now();
    }

    public boolean removeOrder(IBuyOrder order) {
        Objects.requireNonNull(order, "Buy order cannot be null");

        if (orderIndex.remove(order.getId()) != null) {
            Money price = order.getPrice();
            BidPriceLevel level = bidLevels.get(price);
            if (level != null) {
                level.removeOrder(order);
                if (level.isEmpty()) {
                    bidLevels.remove(price);
                }
            }
            updateVolumeMetrics();
            lastUpdate = LocalDateTime.now();
            return true;
        }
        return false;
    }

    public boolean removeOrder(ISellOrder order) {
        Objects.requireNonNull(order, "Sell order cannot be null");

        if (orderIndex.remove(order.getId()) != null) {
            Money price = order.getPrice();
            AskPriceLevel level = askLevels.get(price);
            if (level != null) {
                level.removeOrder(order);
                if (level.isEmpty()) {
                    askLevels.remove(price);
                }
            }
            updateVolumeMetrics();
            lastUpdate = LocalDateTime.now();
            return true;
        }
        return false;
    }

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
        if (bidLevels.isEmpty()) return Optional.empty();

        BidPriceLevel bestBidLevel = bidLevels.firstEntry().getValue();
        return bestBidLevel.getFirstOrder();
    }

    public Optional<ISellOrder> getBestSellOrder() {
        if (askLevels.isEmpty()) return Optional.empty();

        AskPriceLevel bestAskLevel = askLevels.firstEntry().getValue();
        return bestAskLevel.getFirstOrder();
    }

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

    public List<OrderMatch> findMatches() {
        return matchFinder.findMatches(this);
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

    private void validateOrder(IOrder order) {
        if (!symbol.equals(order.getSymbol())) {
            throw new IllegalArgumentException(
                    "Order symbol " + order.getSymbol() + " does not match book symbol " + symbol);
        }

        if (!order.isActive()) {
            throw new IllegalArgumentException("Only active orders can be added to order book");
        }

        if (orderIndex.containsKey(order.getId())) {
            throw new IllegalArgumentException("Order " + order.getId() + " already exists in book");
        }
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