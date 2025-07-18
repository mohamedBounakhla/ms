package core.ms.order.domain.ports.inbound;

import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Value object for transaction statistics
 */
public class TransactionStatistics {
    private final Symbol symbol;
    private final long totalTransactions;
    private final BigDecimal totalVolume;
    private final Money averagePrice;
    private final Money highestPrice;
    private final Money lowestPrice;
    private final LocalDateTime periodStart;
    private final LocalDateTime periodEnd;

    public TransactionStatistics(Symbol symbol, long totalTransactions, BigDecimal totalVolume,
                                 Money averagePrice, Money highestPrice, Money lowestPrice,
                                 LocalDateTime periodStart, LocalDateTime periodEnd) {
        this.symbol = symbol;
        this.totalTransactions = totalTransactions;
        this.totalVolume = totalVolume;
        this.averagePrice = averagePrice;
        this.highestPrice = highestPrice;
        this.lowestPrice = lowestPrice;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    // Getters
    public Symbol getSymbol() { return symbol; }
    public long getTotalTransactions() { return totalTransactions; }
    public BigDecimal getTotalVolume() { return totalVolume; }
    public Money getAveragePrice() { return averagePrice; }
    public Money getHighestPrice() { return highestPrice; }
    public Money getLowestPrice() { return lowestPrice; }
    public LocalDateTime getPeriodStart() { return periodStart; }
    public LocalDateTime getPeriodEnd() { return periodEnd; }
}