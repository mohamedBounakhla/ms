package core.ms.order.application.dto.query;

import core.ms.shared.money.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderDTO {
    private String id;
    private String symbolCode;
    private String symbolName;
    private BigDecimal price;
    private Currency currency;
    private BigDecimal quantity;
    private String status;
    private BigDecimal executedQuantity;
    private BigDecimal remainingQuantity;
    private String orderType; // "BUY" or "SELL"
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OrderDTO() {}

    public OrderDTO(String id, String symbolCode, String symbolName, BigDecimal price, Currency currency,
                    BigDecimal quantity, String status, BigDecimal executedQuantity, BigDecimal remainingQuantity,
                    String orderType, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.symbolCode = symbolCode;
        this.symbolName = symbolName;
        this.price = price;
        this.currency = currency;
        this.quantity = quantity;
        this.status = status;
        this.executedQuantity = executedQuantity;
        this.remainingQuantity = remainingQuantity;
        this.orderType = orderType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSymbolCode() { return symbolCode; }
    public void setSymbolCode(String symbolCode) { this.symbolCode = symbolCode; }
    public String getSymbolName() { return symbolName; }
    public void setSymbolName(String symbolName) { this.symbolName = symbolName; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getExecutedQuantity() { return executedQuantity; }
    public void setExecutedQuantity(BigDecimal executedQuantity) { this.executedQuantity = executedQuantity; }
    public BigDecimal getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(BigDecimal remainingQuantity) { this.remainingQuantity = remainingQuantity; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}