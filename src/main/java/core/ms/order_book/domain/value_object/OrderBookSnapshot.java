package core.ms.order_book.domain.value_object;

import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class OrderBookSnapshot {
    private final String id;
    private final Symbol symbol;
    private final List<OrderSnapshot> buyOrders;
    private final List<OrderSnapshot> sellOrders;
    private final Instant timestamp;
    private final OrderBookStatistics statistics;

    public OrderBookSnapshot(String id, Symbol symbol,
                             List<OrderSnapshot> buyOrders,
                             List<OrderSnapshot> sellOrders,
                             OrderBookStatistics statistics) {
        this.id = Objects.requireNonNull(id);
        this.symbol = Objects.requireNonNull(symbol);
        this.buyOrders = List.copyOf(buyOrders);
        this.sellOrders = List.copyOf(sellOrders);
        this.timestamp = Instant.now();
        this.statistics = Objects.requireNonNull(statistics);
    }

    // Getters
    public String getId() { return id; }
    public Symbol getSymbol() { return symbol; }
    public List<OrderSnapshot> getBuyOrders() { return List.copyOf(buyOrders); }
    public List<OrderSnapshot> getSellOrders() { return List.copyOf(sellOrders); }
    public Instant getTimestamp() { return timestamp; }
    public OrderBookStatistics getStatistics() { return statistics; }

    public static class OrderSnapshot {
        private final String orderId;
        private final Money price;
        private final BigDecimal quantity;
        private final BigDecimal remainingQuantity;
        private final Instant createdAt;

        public OrderSnapshot(String orderId, Money price, BigDecimal quantity,
                             BigDecimal remainingQuantity, Instant createdAt) {
            this.orderId = Objects.requireNonNull(orderId);
            this.price = Objects.requireNonNull(price);
            this.quantity = Objects.requireNonNull(quantity);
            this.remainingQuantity = Objects.requireNonNull(remainingQuantity);
            this.createdAt = Objects.requireNonNull(createdAt);
        }

        // Getters
        public String getOrderId() { return orderId; }
        public Money getPrice() { return price; }
        public BigDecimal getQuantity() { return quantity; }
        public BigDecimal getRemainingQuantity() { return remainingQuantity; }
        public Instant getCreatedAt() { return createdAt; }
    }

    public static class OrderBookStatistics {
        private final int totalBuyOrders;
        private final int totalSellOrders;
        private final BigDecimal totalBuyVolume;
        private final BigDecimal totalSellVolume;
        private final Money bestBid;
        private final Money bestAsk;
        private final Money spread;

        public OrderBookStatistics(int totalBuyOrders, int totalSellOrders,
                                   BigDecimal totalBuyVolume, BigDecimal totalSellVolume,
                                   Money bestBid, Money bestAsk, Money spread) {
            this.totalBuyOrders = totalBuyOrders;
            this.totalSellOrders = totalSellOrders;
            this.totalBuyVolume = totalBuyVolume;
            this.totalSellVolume = totalSellVolume;
            this.bestBid = bestBid;
            this.bestAsk = bestAsk;
            this.spread = spread;
        }

        // Getters
        public int getTotalBuyOrders() { return totalBuyOrders; }
        public int getTotalSellOrders() { return totalSellOrders; }
        public BigDecimal getTotalBuyVolume() { return totalBuyVolume; }
        public BigDecimal getTotalSellVolume() { return totalSellVolume; }
        public Money getBestBid() { return bestBid; }
        public Money getBestAsk() { return bestAsk; }
        public Money getSpread() { return spread; }
    }
}