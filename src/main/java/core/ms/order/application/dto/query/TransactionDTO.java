package core.ms.order.application.dto.query;

import core.ms.shared.domain.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionDTO {
    private String id;
    private String symbolCode;
    private String symbolName;
    private String buyOrderId;
    private String sellOrderId;
    private BigDecimal price;
    private Currency currency;
    private BigDecimal quantity;
    private BigDecimal totalValue;
    private LocalDateTime createdAt;

    public TransactionDTO() {}

    public TransactionDTO(String id, String symbolCode, String symbolName, String buyOrderId, String sellOrderId,
                          BigDecimal price, Currency currency, BigDecimal quantity, BigDecimal totalValue,
                          LocalDateTime createdAt) {
        this.id = id;
        this.symbolCode = symbolCode;
        this.symbolName = symbolName;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.price = price;
        this.currency = currency;
        this.quantity = quantity;
        this.totalValue = totalValue;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSymbolCode() { return symbolCode; }
    public void setSymbolCode(String symbolCode) { this.symbolCode = symbolCode; }
    public String getSymbolName() { return symbolName; }
    public void setSymbolName(String symbolName) { this.symbolName = symbolName; }
    public String getBuyOrderId() { return buyOrderId; }
    public void setBuyOrderId(String buyOrderId) { this.buyOrderId = buyOrderId; }
    public String getSellOrderId() { return sellOrderId; }
    public void setSellOrderId(String sellOrderId) { this.sellOrderId = sellOrderId; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
