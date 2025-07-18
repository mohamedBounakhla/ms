package core.ms.order.application.dto.query;

import core.ms.shared.domain.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionStatisticsDTO {
    private String symbolCode;
    private long totalTransactions;
    private BigDecimal totalVolume;
    private BigDecimal averagePrice;
    private Currency currency;
    private BigDecimal highestPrice;
    private BigDecimal lowestPrice;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    public TransactionStatisticsDTO() {}

    public TransactionStatisticsDTO(String symbolCode, long totalTransactions, BigDecimal totalVolume,
                                    BigDecimal averagePrice, Currency currency, BigDecimal highestPrice,
                                    BigDecimal lowestPrice, LocalDateTime periodStart, LocalDateTime periodEnd) {
        this.symbolCode = symbolCode;
        this.totalTransactions = totalTransactions;
        this.totalVolume = totalVolume;
        this.averagePrice = averagePrice;
        this.currency = currency;
        this.highestPrice = highestPrice;
        this.lowestPrice = lowestPrice;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    // Getters and Setters
    public String getSymbolCode() { return symbolCode; }
    public void setSymbolCode(String symbolCode) { this.symbolCode = symbolCode; }
    public long getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(long totalTransactions) { this.totalTransactions = totalTransactions; }
    public BigDecimal getTotalVolume() { return totalVolume; }
    public void setTotalVolume(BigDecimal totalVolume) { this.totalVolume = totalVolume; }
    public BigDecimal getAveragePrice() { return averagePrice; }
    public void setAveragePrice(BigDecimal averagePrice) { this.averagePrice = averagePrice; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    public BigDecimal getHighestPrice() { return highestPrice; }
    public void setHighestPrice(BigDecimal highestPrice) { this.highestPrice = highestPrice; }
    public BigDecimal getLowestPrice() { return lowestPrice; }
    public void setLowestPrice(BigDecimal lowestPrice) { this.lowestPrice = lowestPrice; }
    public LocalDateTime getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDateTime periodStart) { this.periodStart = periodStart; }
    public LocalDateTime getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDateTime periodEnd) { this.periodEnd = periodEnd; }
}
