package core.ms.OHLC.domain;

import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Value object representing a date range
 */
public class DateRange {
    private final Instant start;
    private final Instant end;

    public DateRange(Instant start, Instant end) {
        this.start = Objects.requireNonNull(start, "Start time cannot be null");
        this.end = Objects.requireNonNull(end, "End time cannot be null");

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start time must be before or equal to end time");
        }
    }

    public Instant getStart() { return start; }
    public Instant getEnd() { return end; }

    public Duration getDuration() {
        return Duration.between(start, end);
    }

    public boolean contains(Instant timestamp) {
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        return !timestamp.isBefore(start) && !timestamp.isAfter(end);
    }

    public boolean overlaps(DateRange other) {
        Objects.requireNonNull(other, "Other date range cannot be null");
        return start.isBefore(other.end) && end.isAfter(other.start);
    }

    public DateRange expand(Duration duration) {
        Objects.requireNonNull(duration, "Duration cannot be null");
        return new DateRange(start.minus(duration), end.plus(duration));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DateRange dateRange = (DateRange) obj;
        return Objects.equals(start, dateRange.start) && Objects.equals(end, dateRange.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return String.format("DateRange[%s to %s]", start, end);
    }
}