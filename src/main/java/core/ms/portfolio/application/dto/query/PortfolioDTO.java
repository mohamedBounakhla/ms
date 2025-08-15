package core.ms.portfolio.application.dto.query;

import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public class PortfolioDTO {
    private String portfolioId;
    private String ownerId;
    private Map<Currency, Money> cashBalances;
    private Map<Symbol, BigDecimal> positions;
    private Money totalValue;
    private LocalDateTime lastUpdated;

    public PortfolioDTO() {}

    public PortfolioDTO(String portfolioId, String ownerId,
                        Map<Currency, Money> cashBalances,
                        Map<Symbol, BigDecimal> positions,
                        Money totalValue,
                        LocalDateTime lastUpdated) {
        this.portfolioId = portfolioId;
        this.ownerId = ownerId;
        this.cashBalances = cashBalances;
        this.positions = positions;
        this.totalValue = totalValue;
        this.lastUpdated = lastUpdated;
    }

    // Getters and Setters
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public Map<Currency, Money> getCashBalances() { return cashBalances; }
    public void setCashBalances(Map<Currency, Money> cashBalances) { this.cashBalances = cashBalances; }
    public Map<Symbol, BigDecimal> getPositions() { return positions; }
    public void setPositions(Map<Symbol, BigDecimal> positions) { this.positions = positions; }
    public Money getTotalValue() { return totalValue; }
    public void setTotalValue(Money totalValue) { this.totalValue = totalValue; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}