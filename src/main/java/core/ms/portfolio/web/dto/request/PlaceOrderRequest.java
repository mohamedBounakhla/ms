package core.ms.portfolio.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PlaceOrderRequest {

    @NotBlank(message = "Symbol code cannot be blank")
    private String symbolCode;

    @NotNull(message = "Price cannot be null")
    @DecimalMin(value = "0.00000001", message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 8, message = "Price must have at most 10 integer digits and 8 decimal places")
    private BigDecimal price;

    @NotBlank(message = "Currency cannot be blank")
    private String currency;

    @NotNull(message = "Quantity cannot be null")
    @DecimalMin(value = "0.00000001", message = "Quantity must be greater than 0")
    @Digits(integer = 10, fraction = 8, message = "Quantity must have at most 10 integer digits and 8 decimal places")
    private BigDecimal quantity;

    // Constructors
    public PlaceOrderRequest() {}

    public PlaceOrderRequest(String symbolCode, BigDecimal price, String currency, BigDecimal quantity) {
        this.symbolCode = symbolCode;
        this.price = price;
        this.currency = currency;
        this.quantity = quantity;
    }

    // Getters and Setters
    public String getSymbolCode() { return symbolCode; }
    public void setSymbolCode(String symbolCode) { this.symbolCode = symbolCode; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
}