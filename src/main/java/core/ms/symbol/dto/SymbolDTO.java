package core.ms.symbol.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import core.ms.shared.money.AssetType;
import core.ms.shared.money.Currency;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SymbolDTO {

    private String code;
    private String name;
    private String description;
    private AssetType assetType;
    private Currency baseCurrency;
    private Currency quoteCurrency;
    private Boolean active;
    private BigDecimal minOrderSize;
    private BigDecimal maxOrderSize;
    private BigDecimal tickSize;
    private BigDecimal lotSize;

    // Constructors
    public SymbolDTO() {}

    public SymbolDTO(String code, String name, String description,
                     AssetType assetType, Currency baseCurrency, Currency quoteCurrency,
                     Boolean active, BigDecimal minOrderSize, BigDecimal maxOrderSize,
                     BigDecimal tickSize, BigDecimal lotSize) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.assetType = assetType;
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.active = active;
        this.minOrderSize = minOrderSize;
        this.maxOrderSize = maxOrderSize;
        this.tickSize = tickSize;
        this.lotSize = lotSize;
    }

    // Getters and Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

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

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public BigDecimal getMinOrderSize() { return minOrderSize; }
    public void setMinOrderSize(BigDecimal minOrderSize) { this.minOrderSize = minOrderSize; }

    public BigDecimal getMaxOrderSize() { return maxOrderSize; }
    public void setMaxOrderSize(BigDecimal maxOrderSize) { this.maxOrderSize = maxOrderSize; }

    public BigDecimal getTickSize() { return tickSize; }
    public void setTickSize(BigDecimal tickSize) { this.tickSize = tickSize; }

    public BigDecimal getLotSize() { return lotSize; }
    public void setLotSize(BigDecimal lotSize) { this.lotSize = lotSize; }
}