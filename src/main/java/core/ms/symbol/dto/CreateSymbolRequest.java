package core.ms.symbol.dto;

import core.ms.shared.money.AssetType;
import core.ms.shared.money.Currency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class CreateSymbolRequest {

    @NotBlank(message = "Symbol code is required")
    @Size(max = 20, message = "Symbol code must not exceed 20 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Symbol code must contain only uppercase letters and numbers")
    private String code;

    @NotBlank(message = "Symbol name is required")
    @Size(max = 100, message = "Symbol name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Asset type is required")
    private AssetType assetType;

    @NotNull(message = "Base currency is required")
    private Currency baseCurrency;

    @NotNull(message = "Quote currency is required")
    private Currency quoteCurrency;

    @DecimalMin(value = "0.00000001", message = "Min order size must be positive")
    @Digits(integer = 10, fraction = 8, message = "Invalid min order size format")
    private BigDecimal minOrderSize;

    @DecimalMin(value = "0.00000001", message = "Max order size must be positive")
    @Digits(integer = 10, fraction = 8, message = "Invalid max order size format")
    private BigDecimal maxOrderSize;

    @DecimalMin(value = "0.00000001", message = "Tick size must be positive")
    @Digits(integer = 10, fraction = 8, message = "Invalid tick size format")
    private BigDecimal tickSize;

    @DecimalMin(value = "0.00000001", message = "Lot size must be positive")
    @Digits(integer = 10, fraction = 8, message = "Invalid lot size format")
    private BigDecimal lotSize;

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

    public BigDecimal getMinOrderSize() { return minOrderSize; }
    public void setMinOrderSize(BigDecimal minOrderSize) { this.minOrderSize = minOrderSize; }

    public BigDecimal getMaxOrderSize() { return maxOrderSize; }
    public void setMaxOrderSize(BigDecimal maxOrderSize) { this.maxOrderSize = maxOrderSize; }

    public BigDecimal getTickSize() { return tickSize; }
    public void setTickSize(BigDecimal tickSize) { this.tickSize = tickSize; }

    public BigDecimal getLotSize() { return lotSize; }
    public void setLotSize(BigDecimal lotSize) { this.lotSize = lotSize; }
}