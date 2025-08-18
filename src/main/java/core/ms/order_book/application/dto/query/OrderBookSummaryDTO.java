package core.ms.order_book.application.dto.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderBookSummaryDTO {
    private String symbol;



    private int bidLevels;


    private int askLevels;
    private BigDecimal totalBidVolume;
    private BigDecimal totalAskVolume;
    private BigDecimal imbalance; // (bidVolume - askVolume) / (bidVolume + askVolume)
    private LocalDateTime timestamp;
    public OrderBookSummaryDTO(){}

    public OrderBookSummaryDTO(String symbol, int bidLevels, int askLevels, BigDecimal totalBidVolume, BigDecimal totalAskVolume, BigDecimal imbalance) {
        this.symbol = symbol;
        this.bidLevels = bidLevels;
        this.askLevels = askLevels;
        this.totalBidVolume = totalBidVolume;
        this.totalAskVolume = totalAskVolume;
        this.imbalance = imbalance;
        this.timestamp = LocalDateTime.now();
    }
    public String getSymbol() {
        return symbol;
    }

    public int getBidLevels() {
        return bidLevels;
    }

    public int getAskLevels() {
        return askLevels;
    }

    public BigDecimal getTotalBidVolume() {
        return totalBidVolume;
    }

    public BigDecimal getTotalAskVolume() {
        return totalAskVolume;
    }

    public BigDecimal getImbalance() {
        return imbalance;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setBidLevels(int bidLevels) {
        this.bidLevels = bidLevels;
    }

    public void setAskLevels(int askLevels) {
        this.askLevels = askLevels;
    }

    public void setTotalBidVolume(BigDecimal totalBidVolume) {
        this.totalBidVolume = totalBidVolume;
    }

    public void setTotalAskVolume(BigDecimal totalAskVolume) {
        this.totalAskVolume = totalAskVolume;
    }

    public void setImbalance(BigDecimal imbalance) {
        this.imbalance = imbalance;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}