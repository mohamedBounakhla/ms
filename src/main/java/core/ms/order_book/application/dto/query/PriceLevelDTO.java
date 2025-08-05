package core.ms.order_book.application.dto.query;

import core.ms.shared.money.Currency;

import java.math.BigDecimal;

public class PriceLevelDTO {
    private BigDecimal price;
    private Currency currency;
    private BigDecimal totalQuantity;
    private int orderCount;

    public PriceLevelDTO() {}

    public PriceLevelDTO(BigDecimal price, Currency currency, BigDecimal totalQuantity, int orderCount) {
        this.price = price;
        this.currency = currency;
        this.totalQuantity = totalQuantity;
        this.orderCount = orderCount;
    }

    // Getters and Setters
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal totalQuantity) { this.totalQuantity = totalQuantity; }
    public int getOrderCount() { return orderCount; }
    public void setOrderCount(int orderCount) { this.orderCount = orderCount; }
}