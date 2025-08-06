package core.ms.order_book.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_books")
public class OrderBookEntity {
    @Id
    @Column(name = "symbol_code", length = 20)
    private String symbolCode;

    @Column(name = "symbol_name", length = 100)
    private String symbolName;

    @Column(name = "base_currency", length = 10)
    private String baseCurrency;

    @Column(name = "quote_currency", length = 10)
    private String quoteCurrency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_activity")
    private Instant lastActivity;

    @Column(name = "is_active")
    private boolean active;

    @Column(name = "total_orders")
    private Integer totalOrders;

    @Column(name = "total_volume", precision = 19, scale = 8)
    private BigDecimal totalVolume;

    // Constructors
    public OrderBookEntity() {}

    public OrderBookEntity(String symbolCode, String symbolName,
                           String baseCurrency, String quoteCurrency) {
        this.symbolCode = symbolCode;
        this.symbolName = symbolName;
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.createdAt = Instant.now();
        this.lastActivity = Instant.now();
        this.active = true;
        this.totalOrders = 0;
        this.totalVolume = BigDecimal.ZERO;
    }

    // Getters and Setters
    public String getSymbolCode() { return symbolCode; }
    public void setSymbolCode(String symbolCode) { this.symbolCode = symbolCode; }
    public String getSymbolName() { return symbolName; }
    public void setSymbolName(String symbolName) { this.symbolName = symbolName; }
    public String getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency; }
    public String getQuoteCurrency() { return quoteCurrency; }
    public void setQuoteCurrency(String quoteCurrency) { this.quoteCurrency = quoteCurrency; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getLastActivity() { return lastActivity; }
    public void setLastActivity(Instant lastActivity) { this.lastActivity = lastActivity; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Integer getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Integer totalOrders) { this.totalOrders = totalOrders; }
    public BigDecimal getTotalVolume() { return totalVolume; }
    public void setTotalVolume(BigDecimal totalVolume) { this.totalVolume = totalVolume; }
}