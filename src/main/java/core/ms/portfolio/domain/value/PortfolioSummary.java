package core.ms.portfolio.domain.value;

import core.ms.shared.domain.Money;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class PortfolioSummary {
    private final String portfolioId;
    private final String portfolioName;
    private final Money totalValue;
    private final Money cashBalance;
    private final Money totalInvested;
    private final Money totalProfitLoss;
    private final BigDecimal profitLossPercentage;
    private final int positionCount;
    private final int transactionCount;
    private final LocalDateTime createdAt;

    public PortfolioSummary(String portfolioId, String portfolioName, Money totalValue,
                            Money cashBalance, Money totalInvested, Money totalProfitLoss,
                            BigDecimal profitLossPercentage, int positionCount, int transactionCount) {
        this.portfolioId = Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null");
        this.portfolioName = Objects.requireNonNull(portfolioName, "Portfolio name cannot be null");
        this.totalValue = Objects.requireNonNull(totalValue, "Total value cannot be null");
        this.cashBalance = Objects.requireNonNull(cashBalance, "Cash balance cannot be null");
        this.totalInvested = Objects.requireNonNull(totalInvested, "Total invested cannot be null");
        this.totalProfitLoss = Objects.requireNonNull(totalProfitLoss, "Total profit/loss cannot be null");
        this.profitLossPercentage = Objects.requireNonNull(profitLossPercentage, "Profit/loss percentage cannot be null");
        this.positionCount = positionCount;
        this.transactionCount = transactionCount;
        this.createdAt = LocalDateTime.now();

        validateSummary();
    }

    // Getters
    public String getPortfolioId() {
        return portfolioId;
    }

    public String getPortfolioName() {
        return portfolioName;
    }

    public Money getTotalValue() {
        return totalValue;
    }

    public Money getCashBalance() {
        return cashBalance;
    }

    public Money getTotalInvested() {
        return totalInvested;
    }

    public Money getTotalProfitLoss() {
        return totalProfitLoss;
    }

    public BigDecimal getProfitLossPercentage() {
        return profitLossPercentage;
    }

    public int getPositionCount() {
        return positionCount;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Business methods
    public boolean isPositive() {
        return totalProfitLoss.isPositive();
    }

    public boolean isNegative() {
        return totalProfitLoss.isNegative();
    }

    public boolean isBreakEven() {
        return totalProfitLoss.isZero();
    }

    public Money getPortfolioValue() {
        return totalInvested.add(cashBalance);
    }

    public BigDecimal getCashPercentage() {
        if (totalValue.isZero()) {
            return BigDecimal.ZERO;
        }

        return cashBalance.getAmount()
                .divide(totalValue.getAmount(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    public BigDecimal getInvestedPercentage() {
        if (totalValue.isZero()) {
            return BigDecimal.ZERO;
        }

        return totalInvested.getAmount()
                .divide(totalValue.getAmount(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    public String getPerformanceStatus() {
        if (isPositive()) {
            return "PROFIT";
        } else if (isNegative()) {
            return "LOSS";
        } else {
            return "BREAK_EVEN";
        }
    }

    private void validateSummary() {
        if (positionCount < 0) {
            throw new IllegalArgumentException("Position count cannot be negative");
        }

        if (transactionCount < 0) {
            throw new IllegalArgumentException("Transaction count cannot be negative");
        }

        if (totalValue.isNegative()) {
            throw new IllegalArgumentException("Total value cannot be negative");
        }

        if (cashBalance.isNegative()) {
            throw new IllegalArgumentException("Cash balance cannot be negative");
        }

        if (totalInvested.isNegative()) {
            throw new IllegalArgumentException("Total invested cannot be negative");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PortfolioSummary that = (PortfolioSummary) obj;
        return Objects.equals(portfolioId, that.portfolioId) &&
                Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, createdAt);
    }

    @Override
    public String toString() {
        return String.format("PortfolioSummary[%s: '%s' - Value: %s, P&L: %s (%.2f%%), %d positions]",
                portfolioId, portfolioName, totalValue, totalProfitLoss,
                profitLossPercentage, positionCount);
    }
}