package core.ms.order.application.dto.command;

import core.ms.shared.domain.Currency;

import java.math.BigDecimal;

public class CreateTransactionCommand {
    private String buyOrderId;
    private String sellOrderId;
    private BigDecimal executionPrice;
    private Currency currency;
    private BigDecimal quantity;

    public CreateTransactionCommand() {}

    public CreateTransactionCommand(String buyOrderId, String sellOrderId,
                                    BigDecimal executionPrice, Currency currency, BigDecimal quantity) {
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