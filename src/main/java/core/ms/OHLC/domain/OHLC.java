package core.ms.OHLC.domain;

import core.ms.shared.domain.Money;

import java.util.Objects;

/**
 * Simple value object for OHLC calculations
 */
public class OHLC {
    private final Money open;
    private final Money high;
    private final Money low;
    private final Money close;

    public OHLC(Money open, Money high, Money low, Money close) {
        this.open = Objects.requireNonNull(open, "Open cannot be null");
        this.high = Objects.requireNonNull(high, "High cannot be null");
        this.low = Objects.requireNonNull(low, "Low cannot be null");
        this.close = Objects.requireNonNull(close, "Close cannot be null");

        validateOHLC();
    }

    public Money getOpen() { return open; }
    public Money getHigh() { return high; }
    public Money getLow() { return low; }
    public Money getClose() { return close; }

    public boolean isValid() {
        try {
            validateOHLC();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void validateOHLC() {
        if (high.isLessThan(low)) {
            throw new IllegalArgumentException("High must be >= Low");
        }
        if (open.isGreaterThan(high) || open.isLessThan(low)) {
            throw new IllegalArgumentException("Open must be between High and Low");
        }
        if (close.isGreaterThan(high) || close.isLessThan(low)) {
            throw new IllegalArgumentException("Close must be between High and Low");
        }
    }

    @Override
    public String toString() {
        return String.format("OHLC[O:%s H:%s L:%s C:%s]",
                open.toDisplayString(), high.toDisplayString(),
                low.toDisplayString(), close.toDisplayString());
    }
}