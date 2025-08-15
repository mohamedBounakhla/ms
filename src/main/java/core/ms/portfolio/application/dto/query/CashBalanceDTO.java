package core.ms.portfolio.application.dto.query;

import core.ms.shared.money.Currency;

import java.math.BigDecimal;

public class CashBalanceDTO {
    private Currency currency;
    private BigDecimal available;
    private BigDecimal reserved;
    private BigDecimal total;

    public CashBalanceDTO() {}

    public CashBalanceDTO(Currency currency, BigDecimal available,
                          BigDecimal reserved, BigDecimal total) {
        this.currency = currency;
        this.available = available;
        this.reserved = reserved;
        this.total = total;
    }

    // Getters and Setters
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    public BigDecimal getAvailable() { return available; }
    public void setAvailable(BigDecimal available) { this.available = available; }
    public BigDecimal getReserved() { return reserved; }
    public void setReserved(BigDecimal reserved) { this.reserved = reserved; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
}