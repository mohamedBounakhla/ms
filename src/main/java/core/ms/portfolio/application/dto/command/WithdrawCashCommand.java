package core.ms.portfolio.application.dto.command;

import core.ms.shared.money.Currency;

import java.math.BigDecimal;

public class WithdrawCashCommand {
    private String portfolioId;
    private BigDecimal amount;
    private Currency currency;

    public WithdrawCashCommand() {}

    public WithdrawCashCommand(String portfolioId, BigDecimal amount, Currency currency) {
        this.portfolioId = portfolioId;
        this.amount = amount;
        this.currency = currency;
    }

    // Getters and Setters
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
}