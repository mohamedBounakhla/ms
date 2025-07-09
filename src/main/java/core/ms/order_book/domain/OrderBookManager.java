package core.ms.order_book.domain;

import core.ms.order.domain.IOrder;
import core.ms.order_book.domain.value_object.MarketOverview;
import core.ms.order_book.domain.value_object.OrderMatch;
import core.ms.shared.domain.Symbol;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OrderBookManager {
    private final Map<Symbol, OrderBook> orderBooks;

    public OrderBookManager() {
        this.orderBooks = new ConcurrentHashMap<>();
    }

    public OrderBook getOrderBook(Symbol symbol) {
        Objects.requireNonNull(symbol, "Symbol cannot be null");
        return orderBooks.computeIfAbsent(symbol, OrderBook::new);
    }

    public OrderBook createOrderBook(Symbol symbol) {
        Objects.requireNonNull(symbol, "Symbol cannot be null");

        if (orderBooks.containsKey(symbol)) {
            throw new IllegalArgumentException("OrderBook for symbol " + symbol + " already exists");
        }

        OrderBook orderBook = new OrderBook(symbol);
        orderBooks.put(symbol, orderBook);
        return orderBook;
    }

    public boolean removeOrderBook(Symbol symbol) {
        Objects.requireNonNull(symbol, "Symbol cannot be null");

        OrderBook removed = orderBooks.remove(symbol);
        return removed != null;
    }

    public Collection<OrderBook> getAllOrderBooks() {
        return new ArrayList<>(orderBooks.values());
    }

    public Set<Symbol> getActiveSymbols() {
        return new HashSet<>(orderBooks.keySet());
    }

    public int getTotalOrderBooks() {
        return orderBooks.size();
    }

    public void addOrderToBook(IOrder order) {
        Objects.requireNonNull(order, "Order cannot be null");
        OrderBook orderBook = getOrderBook(order.getSymbol());
        orderBook.addOrder(order);
    }

    public boolean removeOrderFromBook(IOrder order, Symbol symbol) {
        Objects.requireNonNull(order, "Order ID cannot be null");
        Objects.requireNonNull(symbol, "Symbol cannot be null");

        OrderBook orderBook = orderBooks.get(symbol);
        return orderBook != null && orderBook.removeOrder(order);
    }

    public List<OrderMatch> findAllMatches() {
        return orderBooks.values().stream()
                .flatMap(orderBook -> orderBook.findMatches().stream())
                .collect(Collectors.toList());
    }

    public MarketOverview getMarketOverview() {
        Map<Symbol, BigDecimal> totalVolumes = new HashMap<>();
        int totalOrders = 0;

        for (OrderBook orderBook : orderBooks.values()) {
            Symbol symbol = orderBook.getSymbol();
            BigDecimal totalVolume = orderBook.getTotalBidVolume().add(orderBook.getTotalAskVolume());
            totalVolumes.put(symbol, totalVolume);
            totalOrders += orderBook.getOrderCount();
        }

        return new MarketOverview(
                new HashSet<>(orderBooks.keySet()),
                orderBooks.size(),
                totalOrders,
                totalVolumes
        );
    }


}
