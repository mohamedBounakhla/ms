package core.ms.order_book.domain.entities;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order_book.domain.events.publish.OrderMatchedEvent;
import core.ms.order_book.domain.factory.OrderMatchEventFactory;
import core.ms.order_book.domain.value_object.*;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class OrderBook {
    private final Symbol symbol;
    private final BidSideManager bidSide;
    private final AskSideManager askSide;
    private final Map<String, IOrder> orderIndex;
    private LocalDateTime lastUpdate;
    private final List<OrderMatchedEvent> recentMatchEvents = new ArrayList<>();

    public OrderBook(Symbol symbol) {
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");
        this.bidSide = new BidSideManager();
        this.askSide = new AskSideManager();
        this.orderIndex = new HashMap<>();
        this.lastUpdate = LocalDateTime.now();
    }

    // ============ ORDER OPERATIONS WITH MATCHING ============

    public void addOrder(IBuyOrder order) {
        orderIndex.put(order.getId(), order);
        bidSide.addOrder(order);
        lastUpdate = LocalDateTime.now();

        checkForMatches();
    }

    public void addOrder(ISellOrder order) {

        orderIndex.put(order.getId(), order);
        askSide.addOrder(order);
        lastUpdate = LocalDateTime.now();

        checkForMatches();
    }
    public boolean removeOrderById(String orderId) {
        IOrder order = orderIndex.remove(orderId);
        if (order == null) return false;

        if (order instanceof IBuyOrder) {
            return bidSide.removeOrder((IBuyOrder) order);
        } else if (order instanceof ISellOrder) {
            return askSide.removeOrder((ISellOrder) order);
        }
        return false;
    }
    // ============ MATCHING LOGIC ============

    private void checkForMatches() {
        if (hasSpreadCrossed()) {
            List<OrderMatchedEvent> events = OrderMatchEventFactory.createMatchEvents(bidSide, askSide);
            recentMatchEvents.addAll(events);
        }
    }

    private boolean hasSpreadCrossed() {
        Optional<Money> bestBid = getBestBid();
        Optional<Money> bestAsk = getBestAsk();

        return bestBid.isPresent() && bestAsk.isPresent() &&
                bestBid.get().isGreaterThanOrEqual(bestAsk.get());
    }

    // ============ REMOVE METHODS ============

    public boolean removeOrder(IBuyOrder order) {
        Objects.requireNonNull(order, "Buy order cannot be null");

        if (orderIndex.remove(order.getId()) != null) {
            boolean removed = bidSide.removeOrder(order);
            lastUpdate = LocalDateTime.now();
            return removed;
        }
        return false;
    }

    public boolean removeOrder(ISellOrder order) {
        Objects.requireNonNull(order, "Sell order cannot be null");

        if (orderIndex.remove(order.getId()) != null) {
            boolean removed = askSide.removeOrder(order);
            lastUpdate = LocalDateTime.now();
            return removed;
        }
        return false;
    }

    // ============ QUERY METHODS ============
    // ... (all other methods remain the same)

    public Optional<Money> getBestBid() {
        return bidSide.getBestPrice();
    }

    public Optional<Money> getBestAsk() {
        return askSide.getBestPrice();
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
        return bidSide.getBestOrder();
    }

    public Optional<ISellOrder> getBestSellOrder() {
        return askSide.getBestOrder();
    }

    public BigDecimal getTotalBidVolume() {
        return bidSide.getTotalVolume();
    }

    public BigDecimal getTotalAskVolume() {
        return askSide.getTotalVolume();
    }

    public boolean isEmpty() {
        return bidSide.isEmpty() && askSide.isEmpty();
    }

    public int getOrderCount() {
        return orderIndex.size();
    }

    // ============ DATA ACCESS METHODS ============

    public MarketDepth getMarketDepth(int levels) {
        if (levels <= 0) {
            throw new IllegalArgumentException("Levels must be positive");
        }

        List<BidPriceLevel> topBids = bidSide.getTopLevels(levels);
        List<AskPriceLevel> topAsks = askSide.getTopLevels(levels);

        return new MarketDepth(symbol, topBids, topAsks);
    }

    public Collection<BidPriceLevel> getBidLevels() {
        return bidSide.getLevels();
    }

    public Collection<AskPriceLevel> getAskLevels() {
        return askSide.getLevels();
    }

    // ============ CLEANUP METHODS ============

    public void removeInactiveOrders() {
        List<IOrder> inactiveOrders = orderIndex.values().stream()
                .filter(order -> !order.isActive())
                .collect(Collectors.toList());

        for (IOrder inactiveOrder : inactiveOrders) {
            orderIndex.remove(inactiveOrder.getId());
        }

        bidSide.removeInactiveOrders();
        askSide.removeInactiveOrders();
        lastUpdate = LocalDateTime.now();
    }
    public List<OrderMatchedEvent> getRecentMatchEvents() {
        return new ArrayList<>(recentMatchEvents);
    }

    public boolean hasRecentMatches() {
        return !recentMatchEvents.isEmpty();
    }

    public List<OrderMatchedEvent> consumeRecentMatchEvents() {
        List<OrderMatchedEvent> events = new ArrayList<>(recentMatchEvents);
        recentMatchEvents.clear();
        return events;
    }
    // ============ GETTERS ============

    public Symbol getSymbol() {
        return symbol;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }
}