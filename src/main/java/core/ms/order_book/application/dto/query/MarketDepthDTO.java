package core.ms.order_book.application.dto.query;

import core.ms.shared.money.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class MarketDepthDTO {
    private String symbolCode;
    private List<PriceLevelDTO> bidLevels;
    private List<PriceLevelDTO> askLevels;
    private BigDecimal spread;
    private Currency spreadCurrency;
    private BigDecimal totalBidVolume;
    private BigDecimal totalAskVolume;
    private LocalDateTime timestamp;

    public MarketDepthDTO() {}

    // Constructor with all fields
    public MarketDepthDTO(String symbolCode, List<PriceLevelDTO> bidLevels, List<PriceLevelDTO> askLevels,
                          BigDecimal spread, Currency spreadCurrency, BigDecimal totalBidVolume,
                          BigDecimal totalAskVolume, LocalDateTime timestamp) {
        this.symbolCode = symbolCode;
        this.bidLevels = bidLevels;
        this.askLevels = askLevels;
        this.spread = spread;
        this.spreadCurrency = spreadCurrency;
        this.totalBidVolume = totalBidVolume;
        this.totalAskVolume = totalAskVolume;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getSymbolCode() { return symbolCode; }
    public void setSymbolCode(String symbolCode) { this.symbolCode = symbolCode; }
    public List<PriceLevelDTO> getBidLevels() { return bidLevels; }
    public void setBidLevels(List<PriceLevelDTO> bidLevels) { this.bidLevels = bidLevels; }
    public List<PriceLevelDTO> getAskLevels() { return askLevels; }
    public void setAskLevels(List<PriceLevelDTO> askLevels) { this.askLevels = askLevels; }
    public BigDecimal getSpread() { return spread; }
    public void setSpread(BigDecimal spread) { this.spread = spread; }
    public Currency getSpreadCurrency() { return spreadCurrency; }
    public void setSpreadCurrency(Currency spreadCurrency) { this.spreadCurrency = spreadCurrency; }
    public BigDecimal getTotalBidVolume() { return totalBidVolume; }
    public void setTotalBidVolume(BigDecimal totalBidVolume) { this.totalBidVolume = totalBidVolume; }
    public BigDecimal getTotalAskVolume() { return totalAskVolume; }
    public void setTotalAskVolume(BigDecimal totalAskVolume) { this.totalAskVolume = totalAskVolume; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}