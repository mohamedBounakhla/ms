package core.ms.portfolio.domain.events.publish;

import core.ms.shared.money.Currency;
import core.ms.shared.money.Symbol;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PortfolioUpdateEvent {
    private final String portfolioId;
    private final UpdateType updateType;
    private final Map<Currency, BigDecimal> cashBalances;
    private final Map<String, BigDecimal> assetPositions; // Use String for symbol codes for JSON
    private final LocalDateTime timestamp;

    public enum UpdateType {
        CASH_CHANGE,
        POSITION_CHANGE,
        ORDER_UPDATE,
        FULL_UPDATE
    }

    public PortfolioUpdateEvent(String portfolioId, UpdateType updateType,
                                Map<Currency, BigDecimal> cashBalances,
                                Map<String, BigDecimal> assetPositions) {
        this.portfolioId = portfolioId;
        this.updateType = updateType;
        this.cashBalances = cashBalances;
        this.assetPositions = assetPositions;
        this.timestamp = LocalDateTime.now();
    }

    // Getters
    public String getPortfolioId() { return portfolioId; }
    public UpdateType getUpdateType() { return updateType; }
    public Map<Currency, BigDecimal> getCashBalances() { return cashBalances; }
    public Map<String, BigDecimal> getAssetPositions() { return assetPositions; }
    public LocalDateTime getTimestamp() { return timestamp; }
}