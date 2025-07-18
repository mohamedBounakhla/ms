package core.ms.order.application.dto.command;

import core.ms.shared.domain.Currency;

import java.math.BigDecimal;

public class UpdateOrderPriceCommand {
    private String orderId;
    private BigDecimal newPrice;
    private Currency currency;

    public UpdateOrderPriceCommand() {}

    public UpdateOrderPriceCommand(String orderId, BigDecimal newPrice, Currency currency) {
        this.orderId = orderId;
        this.newPrice = newPrice;
        this.currency = currency;
    }

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public BigDecimal getNewPrice() { return newPrice; }
    public void setNewPrice(BigDecimal newPrice) { this.newPrice = newPrice; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
}
