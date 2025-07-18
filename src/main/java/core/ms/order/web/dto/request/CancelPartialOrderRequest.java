package core.ms.order.web.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class CancelPartialOrderRequest {

    @NotNull(message = "Quantity to cancel cannot be null")
    @DecimalMin(value = "0.00000001", message = "Quantity to cancel must be greater than 0")
    @Digits(integer = 10, fraction = 8, message = "Quantity to cancel must have at most 10 integer digits and 8 decimal places")
    private BigDecimal quantityToCancel;

    // Constructors
    public CancelPartialOrderRequest() {}

    public CancelPartialOrderRequest(BigDecimal quantityToCancel) {
        this.quantityToCancel = quantityToCancel;
    }

    // Getters and Setters
    public BigDecimal getQuantityToCancel() { return quantityToCancel; }
    public void setQuantityToCancel(BigDecimal quantityToCancel) { this.quantityToCancel = quantityToCancel; }
}