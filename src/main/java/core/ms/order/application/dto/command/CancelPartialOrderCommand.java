package core.ms.order.application.dto.command;

import java.math.BigDecimal;

public class CancelPartialOrderCommand {
    private String orderId;
    private BigDecimal quantityToCancel;

    public CancelPartialOrderCommand() {}

    public CancelPartialOrderCommand(String orderId, BigDecimal quantityToCancel) {
        this.orderId = orderId;
        this.quantityToCancel = quantityToCancel;
    }

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public BigDecimal getQuantityToCancel() { return quantityToCancel; }
    public void setQuantityToCancel(BigDecimal quantityToCancel) { this.quantityToCancel = quantityToCancel; }
}