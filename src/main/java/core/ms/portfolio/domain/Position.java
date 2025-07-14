package core.ms.portfolio.domain;

import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import core.ms.utils.BigDecimalNormalizer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

public class Position {
    private final String id;
    private final String portfolioId;
    private final Symbol symbol;
    private BigDecimal quantity;
    private Money averagePrice;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Position(String id, String portfolioId, Symbol symbol, BigDecimal quantity, Money averagePrice) {
        this.id = Objects.requireNonNull(id, "Position ID cannot be null");
        this.portfolioId = Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null");
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");
        this.quantity = Objects.requireNonNull(quantity, "Quantity cannot be null");
        this.averagePrice = Objects.requireNonNull(averagePrice, "Average price cannot be null");
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        validatePosition();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public BigDecimal getQuantity() {
        return BigDecimalNormalizer.normalize(quantity);
    }

    public Money getAveragePrice() {
        return averagePrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Business methods
    public String getSymbolCode() {
        return symbol.getCode();
    }

    /**
     * Calculates the current market value of this position.
     */
    public Money getCurrentValue(Money currentPrice) {
        Objects.requireNonNull(currentPrice, "Current price cannot be null");
        validateCurrency(currentPrice, "Current price");
        return currentPrice.multiply(quantity);
    }

    /**
     * Calculates the total cost basis of this position.
     */
    public Money getCostBasis() {
        return averagePrice.multiply(quantity);
    }

    /**
     * Calculates the profit or loss based on current market price.
     * Positive value indicates profit, negative indicates loss.
     */
    public Money getProfitLoss(Money currentPrice) {
        Objects.requireNonNull(currentPrice, "Current price cannot be null");
        validateCurrency(currentPrice, "Current price");

        Money currentValue = getCurrentValue(currentPrice);
        Money costBasis = getCostBasis();
        return currentValue.subtract(costBasis);
    }

    /**
     * Calculates the profit or loss percentage based on current market price.
     */
    public BigDecimal getProfitLossPercentage(Money currentPrice) {
        Objects.requireNonNull(currentPrice, "Current price cannot be null");
        validateCurrency(currentPrice, "Current price");

        Money profitLoss = getProfitLoss(currentPrice);
        Money costBasis = getCostBasis();

        if (costBasis.isZero()) {
            return BigDecimal.ZERO;
        }

        // Calculate: (profitLoss / costBasis) * 100
        BigDecimal profitLossAmount = profitLoss.getAmount();
        BigDecimal costBasisAmount = costBasis.getAmount();

        return profitLossAmount
                .divide(costBasisAmount, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * Adds quantity to this position and updates the average price.
     */
    public void addQuantity(BigDecimal additionalQuantity, Money price) {
        Objects.requireNonNull(additionalQuantity, "Additional quantity cannot be null");
        Objects.requireNonNull(price, "Price cannot be null");
        validateCurrency(price, "Price");

        if (additionalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Additional quantity must be positive");
        }

        // Calculate new average price using weighted average
        Money newAveragePrice = calculateNewAveragePrice(additionalQuantity, price);

        this.quantity = this.quantity.add(additionalQuantity);
        this.averagePrice = newAveragePrice;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Removes quantity from this position. Does not affect average price.
     */
    public void removeQuantity(BigDecimal quantityToRemove) {
        Objects.requireNonNull(quantityToRemove, "Quantity to remove cannot be null");
        validateQuantityRemoval(quantityToRemove);

        this.quantity = this.quantity.subtract(quantityToRemove);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Returns true if this position has no quantity (empty position).
     */
    public boolean isEmpty() {
        return quantity.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Returns true if this position has sufficient quantity for removal.
     */
    public boolean hasSufficientQuantity(BigDecimal quantityToCheck) {
        Objects.requireNonNull(quantityToCheck, "Quantity to check cannot be null");
        return quantity.compareTo(quantityToCheck) >= 0;
    }

    private Money calculateNewAveragePrice(BigDecimal additionalQuantity, Money price) {
        // Weighted average: ((existing_qty * avg_price) + (new_qty * new_price)) / (existing_qty + new_qty)
        Money existingCost = averagePrice.multiply(quantity);
        Money additionalCost = price.multiply(additionalQuantity);
        Money totalCost = existingCost.add(additionalCost);
        BigDecimal totalQuantity = quantity.add(additionalQuantity);

        return totalCost.divide(totalQuantity);
    }

    private void validateQuantityRemoval(BigDecimal quantityToRemove) {
        if (quantityToRemove.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity to remove must be positive");
        }

        if (quantityToRemove.compareTo(quantity) > 0) {
            throw new IllegalArgumentException(
                    String.format("Cannot remove %s units. Only %s units available",
                            quantityToRemove, quantity));
        }
    }

    private void validateCurrency(Money money, String fieldName) {
        if (!money.getCurrency().equals(averagePrice.getCurrency())) {
            throw new IllegalArgumentException(
                    String.format("%s currency %s does not match position currency %s",
                            fieldName, money.getCurrency(), averagePrice.getCurrency()));
        }
    }

    private void validatePosition() {
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }

        if (averagePrice.isNegative()) {
            throw new IllegalArgumentException("Average price cannot be negative");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Position position = (Position) obj;
        return Objects.equals(id, position.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Position[%s: %s %s @ %s avg, value: %s]",
                id, quantity, symbol.getCode(), averagePrice, getCostBasis());
    }
}