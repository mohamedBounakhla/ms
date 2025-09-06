package core.ms.portfolio.application.dto.command;

import core.ms.shared.money.Symbol;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class DepositAssetCommand {
    @NotNull(message = "Portfolio ID is required")
    private String portfolioId;

    @NotNull(message = "Symbol is required")
    private Symbol symbol;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    // Constructors
    public DepositAssetCommand() {}

    public DepositAssetCommand(String portfolioId, Symbol symbol, BigDecimal quantity) {
        this.portfolioId = portfolioId;
        this.symbol = symbol;
        this.quantity = quantity;
    }

    // Getters and Setters
    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}