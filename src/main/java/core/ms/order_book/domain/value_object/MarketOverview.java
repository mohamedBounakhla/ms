package core.ms.order_book.domain.value_object;

import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class MarketOverview {
    private final Set<Symbol> activeSymbols;
    private final int totalOrderBooks;
    private final int totalOrders;
    private final Map<Symbol, BigDecimal> totalVolume;
    private final LocalDateTime timestamp;

    public MarketOverview(Set<Symbol> activeSymbols, int totalOrderBooks,
                          int totalOrders, Map<Symbol, BigDecimal> totalVolume) {
        this.activeSymbols = new HashSet<>(Objects.requireNonNull(activeSymbols, "Active symbols cannot be null"));
        this.totalOrderBooks = totalOrderBooks;
        this.totalOrders = totalOrders;
        this.totalVolume = new HashMap<>(Objects.requireNonNull(totalVolume, "Total volume cannot be null"));
        this.timestamp = LocalDateTime.now();
    }

    public Set<Symbol> getActiveSymbols() {
        return new HashSet<>(activeSymbols);
    }

    public int getTotalOrderBooks() {
        return totalOrderBooks;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public Map<Symbol, BigDecimal> getTotalVolume() {
        return new HashMap<>(totalVolume);
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public BigDecimal getVolumeForSymbol(Symbol symbol) {
        return totalVolume.getOrDefault(symbol, BigDecimal.ZERO);
    }
}
