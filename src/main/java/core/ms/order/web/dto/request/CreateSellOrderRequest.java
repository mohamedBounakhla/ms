package core.ms.order.web.dto.request;

import core.ms.shared.domain.Currency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class CreateSellOrderRequest {

    @NotBlank(message = "User ID cannot be blank")
    @Size(min = 1, max = 50, message = "User ID must be between 1 and 50 characters")
    private String userId;

    @NotBlank(message = "Symbol code cannot be blank")
    @Size(min = 1, max = 20, message = "Symbol code must be between 1 and 20 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Symbol code must contain only uppercase letters and numbers")
    private String symbolCode;

    @NotNull(message = "Price cannot be null")
    @DecimalMin(value = "0.00000001", message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 8, message = "Price must have at most 10 integer digits and 8 decimal places")
    private BigDecimal price;

    @NotNull(message = "Currency cannot be null")
    private Currency currency;

    @NotNull(message = "Quantity cannot be null")
    @DecimalMin(value = "0.00000001", message = "Quantity must be greater than 0")
    @Digits(integer = 10, fraction = 8, message = "Quantity must have at most 10 integer digits and 8 decimal places")
    private BigDecimal quantity;

    // Constructors
    public CreateSellOrderRequest() {}

    public CreateSellOrderRequest(String userId, String symbolCode, BigDecimal price, Currency currency, BigDecimal quantity) {
        this.userId = userId;
        this.symbolCode = symbolCode;
        this.price = price;
        this.currency = currency;
        this.quantity = quantity;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSymbolCode() { return symbolCode; }
    public void setSymbolCode(String symbolCode) { this.symbolCode = symbolCode; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
}