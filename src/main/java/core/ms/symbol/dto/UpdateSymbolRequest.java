package core.ms.symbol.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class UpdateSymbolRequest {

    @Size(max = 100, message = "Symbol name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

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

    private Boolean active;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getMinOrderSize() { return minOrderSize; }
    public void setMinOrderSize(BigDecimal minOrderSize) { this.minOrderSize = minOrderSize; }

    public BigDecimal getMaxOrderSize() { return maxOrderSize; }
    public void setMaxOrderSize(BigDecimal maxOrderSize) { this.maxOrderSize = maxOrderSize; }

    public BigDecimal getTickSize() { return tickSize; }
    public void setTickSize(BigDecimal tickSize) { this.tickSize = tickSize; }

    public BigDecimal getLotSize() { return lotSize; }
    public void setLotSize(BigDecimal lotSize) { this.lotSize = lotSize; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}