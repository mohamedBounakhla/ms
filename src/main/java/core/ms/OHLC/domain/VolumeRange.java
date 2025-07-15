package core.ms.OHLC.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object representing a volume range
 */
public class VolumeRange {
    private final BigDecimal min;
    private final BigDecimal max;
    private final BigDecimal average;

    public VolumeRange(BigDecimal min, BigDecimal max, BigDecimal average) {
        this.min = Objects.requireNonNull(min, "Min volume cannot be null");
        this.max = Objects.requireNonNull(max, "Max volume cannot be null");
        this.average = Objects.requireNonNull(average, "Average volume cannot be null");

        if (min.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Min volume cannot be negative");
        }
        if (max.compareTo(min) < 0) {
            throw new IllegalArgumentException("Max volume must be >= min volume");
        }
        if (average.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Average volume cannot be negative");
        }
    }

    public BigDecimal getMin() { return min; }
    public BigDecimal getMax() { return max; }
    public BigDecimal getAverage() { return average; }

    public BigDecimal getRange() {
        return max.subtract(min);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        VolumeRange that = (VolumeRange) obj;
        return Objects.equals(min, that.min) &&
                Objects.equals(max, that.max) &&
                Objects.equals(average, that.average);
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max, average);
    }

    @Override
    public String toString() {
        return String.format("VolumeRange[min:%s, max:%s, avg:%s]",
                min.toPlainString(), max.toPlainString(), average.toPlainString());
    }
}