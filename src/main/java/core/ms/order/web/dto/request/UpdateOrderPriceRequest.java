package core.ms.order.web.dto.request;

import core.ms.shared.money.Currency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class UpdateOrderPriceRequest {

    @NotNull(message = "New price cannot be null")
    @DecimalMin(value = "0.00000001", message = "New price must be greater than 0")
    @Digits(integer = 10, fraction = 8, message = "New price must have at most 10 integer digits and 8 decimal places")
    private BigDecimal newPrice;

    @NotNull(message = "Currency cannot be null")
    private Currency currency;

    // Constructors
    public UpdateOrderPriceRequest() {}

    public UpdateOrderPriceRequest(BigDecimal newPrice, Currency currency) {
        this.newPrice = newPrice;
        this.currency = currency;
    }

    // Getters and Setters
    public BigDecimal getNewPrice() { return newPrice; }
    public void setNewPrice(BigDecimal newPrice) { this.newPrice = newPrice; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
}