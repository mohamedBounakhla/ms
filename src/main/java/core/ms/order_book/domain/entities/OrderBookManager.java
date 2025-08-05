package core.ms.order_book.domain.entities;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order_book.domain.value_object.MarketOverview;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OrderBookManager {
    private final Map<Symbol, OrderBook> orderBooks;


    public OrderBookManager() {
        this.orderBooks = new ConcurrentHashMap<>();
    }
    // ============ ORDER BOOK MANAGEMENT ============

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

    // ============ ORDER MANAGEMENT WITH TYPE INTROSPECTION ============

    public synchronized void addOrderToBook(IOrder order) {
        Objects.requireNonNull(order, "Order cannot be null");

        OrderBook orderBook = getOrderBook(order.getSymbol());

        // Handle type introspection and casting here
        if (order instanceof IBuyOrder) {
            orderBook.addOrder((IBuyOrder) order);
        } else if (order instanceof ISellOrder) {
            orderBook.addOrder((ISellOrder) order);
        } else {
            throw new IllegalArgumentException("Unknown order type: " + order.getClass());
        }
    }

    public synchronized boolean removeOrderFromBook(IOrder order, Symbol symbol) {
        Objects.requireNonNull(order, "Order cannot be null");
        Objects.requireNonNull(symbol, "Symbol cannot be null");

        OrderBook orderBook = orderBooks.get(symbol);
        if (orderBook == null) {
            return false;
        }

        // Handle type introspection and casting here
        if (order instanceof IBuyOrder) {
            return orderBook.removeOrder((IBuyOrder) order);
        } else if (order instanceof ISellOrder) {
            return orderBook.removeOrder((ISellOrder) order);
        } else {
            throw new IllegalArgumentException("Unknown order type: " + order.getClass());
        }
    }

    // ============ QUERY METHODS ============

    public Collection<OrderBook> getAllOrderBooks() {
        return new ArrayList<>(orderBooks.values());
    }

    public Set<Symbol> getActiveSymbols() {
        return new HashSet<>(orderBooks.keySet());
    }

    public int getTotalOrderBooks() {
        return orderBooks.size();
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