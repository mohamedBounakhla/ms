package core.ms.order.web.dto.request;

import core.ms.shared.money.Currency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class CreateTransactionRequest {

    @NotBlank(message = "Buy order ID cannot be blank")
    @Size(min = 1, max = 50, message = "Buy order ID must be between 1 and 50 characters")
    private String buyOrderId;

    @NotBlank(message = "Sell order ID cannot be blank")
    @Size(min = 1, max = 50, message = "Sell order ID must be between 1 and 50 characters")
    private String sellOrderId;

    @NotNull(message = "Execution price cannot be null")
    @DecimalMin(value = "0.00000001", message = "Execution price must be greater than 0")
    @Digits(integer = 10, fraction = 8, message = "Execution price must have at most 10 integer digits and 8 decimal places")
    private BigDecimal executionPrice;

    @NotNull(message = "Currency cannot be null")
    private Currency currency;

    @NotNull(message = "Quantity cannot be null")
    @DecimalMin(value = "0.00000001", message = "Quantity must be greater than 0")
    @Digits(integer = 10, fraction = 8, message = "Quantity must have at most 10 integer digits and 8 decimal places")
    private BigDecimal quantity;

    // Constructors
    public CreateTransactionRequest() {}

    public CreateTransactionRequest(String buyOrderId, String sellOrderId, BigDecimal executionPrice, Currency currency, BigDecimal quantity) {
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.executionPrice = executionPrice;
        this.currency = currency;
        this.quantity = quantity;
    }

    // Getters and Setters
    public String getBuyOrderId() { return buyOrderId; }
    public void setBuyOrderId(String buyOrderId) { this.buyOrderId = buyOrderId; }
    public String getSellOrderId() { return sellOrderId; }
    public void setSellOrderId(String sellOrderId) { this.sellOrderId = sellOrderId; }
    public BigDecimal getExecutionPrice() { return executionPrice; }
    public void setExecutionPrice(BigDecimal executionPrice) { this.executionPrice = executionPrice; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
}