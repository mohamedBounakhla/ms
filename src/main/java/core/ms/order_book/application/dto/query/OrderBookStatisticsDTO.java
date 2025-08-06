package core.ms.order_book.application.dto.query;

import core.ms.shared.money.Currency;

import java.math.BigDecimal;

public class OrderBookStatisticsDTO {
    private int totalBuyOrders;
    private int totalSellOrders;
    private BigDecimal totalBuyVolume;
    private BigDecimal totalSellVolume;
    private BigDecimal bestBidPrice;
    private BigDecimal bestAskPrice;
    private Currency priceCurrency;
    private BigDecimal spread;

    // Constructors
    public OrderBookStatisticsDTO() {}

    // Getters and Setters
    public int getTotalBuyOrders() { return totalBuyOrders; }
    public void setTotalBuyOrders(int totalBuyOrders) { this.totalBuyOrders = totalBuyOrders; }
    public int getTotalSellOrders() { return totalSellOrders; }
    public void setTotalSellOrders(int totalSellOrders) { this.totalSellOrders = totalSellOrders; }
    public BigDecimal getTotalBuyVolume() { return totalBuyVolume; }
    public void setTotalBuyVolume(BigDecimal totalBuyVolume) { this.totalBuyVolume = totalBuyVolume; }
    public BigDecimal getTotalSellVolume() { return totalSellVolume; }
    public void setTotalSellVolume(BigDecimal totalSellVolume) { this.totalSellVolume = totalSellVolume; }
    public BigDecimal getBestBidPrice() { return bestBidPrice; }
    public void setBestBidPrice(BigDecimal bestBidPrice) { this.bestBidPrice = bestBidPrice; }
    public BigDecimal getBestAskPrice() { return bestAskPrice; }
    public void setBestAskPrice(BigDecimal bestAskPrice) { this.bestAskPrice = bestAskPrice; }
    public Currency getPriceCurrency() { return priceCurrency; }
    public void setPriceCurrency(Currency priceCurrency) { this.priceCurrency = priceCurrency; }
    public BigDecimal getSpread() { return spread; }
    public void setSpread(BigDecimal spread) { this.spread = spread; }
}
