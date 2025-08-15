package core.ms.portfolio.domain.ports.inbound;

import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public class PortfolioSnapshot {
    private final String portfolioId;
    private final String ownerId;
    private final Map<Currency, Money> cashBalances;
    private final Map<Symbol, BigDecimal> positions;
    private final Map<Currency, Money> reservedCash;
    private final Map<Symbol, BigDecimal> reservedAssets;
    private final Money totalValue; // Estimated total portfolio value
    private final LocalDateTime timestamp;

    public PortfolioSnapshot(String portfolioId, String ownerId,
                             Map<Currency, Money> cashBalances,
                             Map<Symbol, BigDecimal> positions,
                             Map<Currency, Money> reservedCash,
                             Map<Symbol, BigDecimal> reservedAssets,
                             Money totalValue) {
        this.portfolioId = portfolioId;
        this.ownerId = ownerId;
        this.cashBalances = Map.copyOf(cashBalances);
        this.positions = Map.copyOf(positions);
        this.reservedCash = Map.copyOf(reservedCash);
        this.reservedAssets = Map.copyOf(reservedAssets);
        this.totalValue = totalValue;
        this.timestamp = LocalDateTime.now();
    }

    // Getters
    public String getPortfolioId() { return portfolioId; }
    public String getOwnerId() { return ownerId; }
    public Map<Currency, Money> getCashBalances() { return Map.copyOf(cashBalances); }
    public Map<Symbol, BigDecimal> getPositions() { return Map.copyOf(positions); }
    public Map<Currency, Money> getReservedCash() { return Map.copyOf(reservedCash); }
    public Map<Symbol, BigDecimal> getReservedAssets() { return Map.copyOf(reservedAssets); }
    public Money getTotalValue() { return totalValue; }
    public LocalDateTime getTimestamp() { return timestamp; }
}