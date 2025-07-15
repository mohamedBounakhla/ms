package core.ms.OHLC.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public enum TimeInterval {
    ONE_MINUTE("1m", Duration.ofMinutes(1)),
    FIVE_MINUTES("5m", Duration.ofMinutes(5)),
    FIFTEEN_MINUTES("15m", Duration.ofMinutes(15)),
    THIRTY_MINUTES("30m", Duration.ofMinutes(30)),
    ONE_HOUR("1h", Duration.ofHours(1)),
    FOUR_HOURS("4h", Duration.ofHours(4)),
    ONE_DAY("1d", Duration.ofDays(1)),
    ONE_WEEK("1w", Duration.ofDays(7)),
    ONE_MONTH("1M", Duration.ofDays(30)); // Approximate

    private final String code;
    private final Duration duration;

    TimeInterval(String code, Duration duration) {
        this.code = code;
        this.duration = duration;
    }

    // ===== GETTERS =====

    public String getCode() { return code; }
    public Duration getDuration() { return duration; }
    public long getMilliseconds() { return duration.toMillis(); }

    // ===== BUSINESS METHODS =====

    public boolean isHigherThan(TimeInterval other) {
        return this.duration.compareTo(other.duration) > 0;
    }

    public boolean isLowerThan(TimeInterval other) {
        return this.duration.compareTo(other.duration) < 0;
    }

    /**
     * Aligns a timestamp to the interval boundary
     * For example, 14:23:45 with 1h interval becomes 14:00:00
     */
    public Instant alignTimestamp(Instant timestamp) {
        long epochSecond = timestamp.getEpochSecond();
        long intervalSeconds = duration.getSeconds();
        long alignedSeconds = (epochSecond / intervalSeconds) * intervalSeconds;
        return Instant.ofEpochSecond(alignedSeconds);
    }

    /**
     * Gets the next interval timestamp
     */
    public Instant getNextTimestamp(Instant current) {
        Instant aligned = alignTimestamp(current);
        return aligned.plus(duration);
    }

    /**
     * Gets the previous interval timestamp
     */
    public Instant getPreviousTimestamp(Instant current) {
        Instant aligned = alignTimestamp(current);
        return aligned.minus(duration);
    }

    @Override
    public String toString() {
        return code;
    }
}
