package core.ms.order.infrastructure.persistence.entities;

import core.ms.shared.money.Currency;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "symbol_code", nullable = false, length = 20)
    private String symbolCode;

    @Column(name = "symbol_name", nullable = false, length = 100)
    private String symbolName;

    @Column(name = "buy_order_id", nullable = false, length = 50)
    private String buyOrderId;

    @Column(name = "sell_order_id", nullable = false, length = 50)
    private String sellOrderId;

    @Column(name = "price", nullable = false, precision = 19, scale = 8)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private Currency currency;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(name = "total_value", nullable = false, precision = 19, scale = 8)
    private BigDecimal totalValue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Constructors
    public TransactionEntity() {}

    public TransactionEntity(String id, String symbolCode, String symbolName,
                             String buyOrderId, String sellOrderId,
                             BigDecimal price, Currency currency, BigDecimal quantity,
                             BigDecimal totalValue, LocalDateTime createdAt) {
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TransactionEntity that = (TransactionEntity) obj;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}