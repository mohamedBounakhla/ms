package core.ms.order.application.dto.command;

import core.ms.shared.money.Currency;

import java.math.BigDecimal;

public class CreateBuyOrderCommand {
    private String portfolioId;
    private String reservationId;
    private String symbolCode;
    private BigDecimal price;
    private Currency currency;
    private BigDecimal quantity;

    public CreateBuyOrderCommand() {}

    public CreateBuyOrderCommand(String portfolioId, String reservationId, String symbolCode,
                                 BigDecimal price, Currency currency, BigDecimal quantity) {
        this.portfolioId = portfolioId;
        this.reservationId = reservationId;
        this.symbolCode = symbolCode;
        this.price = price;
        this.currency = currency;
        this.quantity = quantity;
    }

    // Getters and Setters
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public String getSymbolCode() { return symbolCode; }
    public void setSymbolCode(String symbolCode) { this.symbolCode = symbolCode; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
}