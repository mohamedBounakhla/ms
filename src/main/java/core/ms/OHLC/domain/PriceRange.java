package core.ms.OHLC.domain;

import core.ms.shared.money.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object representing a price range
 */
public class PriceRange {
    private final Money min;
    private final Money max;

    public PriceRange(Money min, Money max) {
        this.min = Objects.requireNonNull(min, "Min price cannot be null");
        this.max = Objects.requireNonNull(max, "Max price cannot be null");

        if (!min.getCurrency().equals(max.getCurrency())) {
            throw new IllegalArgumentException("Min and max prices must have the same currency");
        }

        if (min.isGreaterThan(max)) {
            throw new IllegalArgumentException("Min price must be <= max price");
        }
    }

    public Money getMin() { return min; }
    public Money getMax() { return max; }

    public Money getRange() {
        return max.subtract(min);
    }

    public boolean contains(Money price) {
        Objects.requireNonNull(price, "Price cannot be null");
        return price.isGreaterThanOrEqual(min) && price.isLessThanOrEqual(max);
    }

    /**
     * Returns the percentage position of a price within this range (0.0 to 1.0)
     */
    public BigDecimal getPercentagePosition(Money price) {
        Objects.requireNonNull(price, "Price cannot be null");

        if (!contains(price)) {
            throw new IllegalArgumentException("Price is outside the range");
        }

        if (getRange().isZero()) {
            return BigDecimal.ZERO;
        }

        Money priceFromMin = price.subtract(min);
        return priceFromMin.getAmount()
                .divide(getRange().getAmount(), 4, RoundingMode.HALF_UP);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PriceRange that = (PriceRange) obj;
        return Objects.equals(min, that.min) && Objects.equals(max, that.max);
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max);
    }

    @Override
    public String toString() {
        return String.format("PriceRange[%s - %s]", min.toDisplayString(), max.toDisplayString());
    }
}
