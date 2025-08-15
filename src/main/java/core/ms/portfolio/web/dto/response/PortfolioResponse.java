package core.ms.portfolio.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class PortfolioResponse {
    private String portfolioId;
    private String ownerId;
    private Map<String, BigDecimal> cashBalances;
    private List<PositionResponse> positions;
    private BigDecimal totalValue;
    private String valueCurrency;
    private LocalDateTime lastUpdated;

    // Constructors
    public PortfolioResponse() {}

    // Getters and Setters
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public Map<String, BigDecimal> getCashBalances() { return cashBalances; }
    public void setCashBalances(Map<String, BigDecimal> cashBalances) { this.cashBalances = cashBalances; }
    public List<PositionResponse> getPositions() { return positions; }
    public void setPositions(List<PositionResponse> positions) { this.positions = positions; }
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    public String getValueCurrency() { return valueCurrency; }
    public void setValueCurrency(String valueCurrency) { this.valueCurrency = valueCurrency; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}