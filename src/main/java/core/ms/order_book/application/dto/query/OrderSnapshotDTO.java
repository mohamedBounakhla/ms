package core.ms.order_book.application.dto.query;

import core.ms.shared.money.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderSnapshotDTO {
    private String orderId;
    private BigDecimal price;
    private Currency currency;
    private BigDecimal quantity;
    private BigDecimal remainingQuantity;
    private LocalDateTime createdAt;

    // Constructors
    public OrderSnapshotDTO() {}

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(BigDecimal remainingQuantity) { this.remainingQuantity = remainingQuantity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}