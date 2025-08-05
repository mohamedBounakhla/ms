package core.ms.order.application.dto.command;

import core.ms.shared.money.Currency;

import java.math.BigDecimal;

public class CreateBuyOrderCommand {
    private String userId;
    private String symbolCode;
    private BigDecimal price;
    private Currency currency;
    private BigDecimal quantity;

    public CreateBuyOrderCommand() {}

    public CreateBuyOrderCommand(String userId, String symbolCode, BigDecimal price,
                                 Currency currency, BigDecimal quantity) {
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