package core.ms.symbol.domain;

import core.ms.shared.money.AssetType;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Symbol;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "symbols",
        indexes = {
                @Index(name = "idx_symbol_active", columnList = "active"),
                @Index(name = "idx_symbol_type", columnList = "asset_type")
        })
public class SymbolEntity {

    @Id
    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private AssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_currency", nullable = false, length = 10)
    private Currency baseCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "quote_currency", nullable = false, length = 10)
    private Currency quoteCurrency;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "min_order_size", precision = 19, scale = 8)
    private BigDecimal minOrderSize;

    @Column(name = "max_order_size", precision = 19, scale = 8)
    private BigDecimal maxOrderSize;

    @Column(name = "tick_size", precision = 19, scale = 8)
    private BigDecimal tickSize;

    @Column(name = "lot_size", precision = 19, scale = 8)
    private BigDecimal lotSize;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public SymbolEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public SymbolEntity(String code, String name, AssetType assetType,
                        Currency baseCurrency, Currency quoteCurrency) {
        this.code = code.toUpperCase();
        this.name = name;
        this.assetType = assetType;
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Full constructor
    public SymbolEntity(String code, String name, String description,
                        AssetType assetType, Currency baseCurrency, Currency quoteCurrency,
                        BigDecimal minOrderSize, BigDecimal maxOrderSize,
                        BigDecimal tickSize, BigDecimal lotSize) {
        this(code, name, assetType, baseCurrency, quoteCurrency);
        this.description = description;
        this.minOrderSize = minOrderSize;
        this.maxOrderSize = maxOrderSize;
        this.tickSize = tickSize;
        this.lotSize = lotSize;
    }

    // Convert to domain Symbol
    public Symbol toDomainSymbol() {
        return new Symbol(code, name, assetType, baseCurrency, quoteCurrency);
    }

    // Getters and Setters
    public String getCode() { return code; }
    public void setCode(String code) {
        this.code = code != null ? code.toUpperCase() : null;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public AssetType getAssetType() { return assetType; }
    public void setAssetType(AssetType assetType) { this.assetType = assetType; }

    public Currency getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(Currency baseCurrency) { this.baseCurrency = baseCurrency; }

    public Currency getQuoteCurrency() { return quoteCurrency; }
    public void setQuoteCurrency(Currency quoteCurrency) { this.quoteCurrency = quoteCurrency; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = LocalDateTime.now();
    }

    public BigDecimal getMinOrderSize() { return minOrderSize; }
    public void setMinOrderSize(BigDecimal minOrderSize) { this.minOrderSize = minOrderSize; }

    public BigDecimal getMaxOrderSize() { return maxOrderSize; }
    public void setMaxOrderSize(BigDecimal maxOrderSize) { this.maxOrderSize = maxOrderSize; }

    public BigDecimal getTickSize() { return tickSize; }
    public void setTickSize(BigDecimal tickSize) { this.tickSize = tickSize; }

    public BigDecimal getLotSize() { return lotSize; }
    public void setLotSize(BigDecimal lotSize) { this.lotSize = lotSize; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}