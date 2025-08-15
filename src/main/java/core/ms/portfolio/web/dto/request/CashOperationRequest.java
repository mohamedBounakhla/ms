package core.ms.portfolio.web.dto.request;

import core.ms.shared.money.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CashOperationRequest {

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.00000001", message = "Amount must be greater than 0")
    @Digits(integer = 10, fraction = 8, message = "Amount must have at most 10 integer digits and 8 decimal places")
    private BigDecimal amount;

    @NotNull(message = "Currency cannot be null")
    private Currency currency;

    // Constructors
    public CashOperationRequest() {}

    public CashOperationRequest(BigDecimal amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }

    // Getters and Setters
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
}