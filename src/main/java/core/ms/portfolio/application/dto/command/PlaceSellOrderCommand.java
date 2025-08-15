package core.ms.portfolio.application.dto.command;

import java.math.BigDecimal;

public class PlaceSellOrderCommand {
    private String portfolioId;
    private String orderId;
    private String symbolCode;
    private BigDecimal price;
    private String currency;
    private BigDecimal quantity;

    // Constructors
    public PlaceSellOrderCommand() {}

    // Getters and Setters
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getSymbolCode() { return symbolCode; }
    public void setSymbolCode(String symbolCode) { this.symbolCode = symbolCode; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
}