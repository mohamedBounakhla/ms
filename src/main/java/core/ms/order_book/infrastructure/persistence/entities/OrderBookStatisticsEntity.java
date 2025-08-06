package core.ms.order_book.infrastructure.persistence.entities;

import core.ms.shared.money.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;

@Embeddable
public class OrderBookStatisticsEntity {
    @Column(name = "total_buy_orders")
    private Integer totalBuyOrders;

    @Column(name = "total_sell_orders")
    private Integer totalSellOrders;

    @Column(name = "total_buy_volume", precision = 19, scale = 8)
    private BigDecimal totalBuyVolume;

    @Column(name = "total_sell_volume", precision = 19, scale = 8)
    private BigDecimal totalSellVolume;

    @Column(name = "best_bid_price", precision = 19, scale = 8)
    private BigDecimal bestBidPrice;

    @Column(name = "best_ask_price", precision = 19, scale = 8)
    private BigDecimal bestAskPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_currency", length = 10)
    private Currency priceCurrency;

    @Column(name = "spread", precision = 19, scale = 8)
    private BigDecimal spread;

    // Constructors
    public OrderBookStatisticsEntity() {}

    // Getters and Setters
    public Integer getTotalBuyOrders() { return totalBuyOrders; }
    public void setTotalBuyOrders(Integer totalBuyOrders) { this.totalBuyOrders = totalBuyOrders; }
    public Integer getTotalSellOrders() { return totalSellOrders; }
    public void setTotalSellOrders(Integer totalSellOrders) { this.totalSellOrders = totalSellOrders; }
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