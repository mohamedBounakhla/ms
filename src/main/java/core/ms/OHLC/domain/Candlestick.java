package core.ms.OHLC.domain;

import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import core.ms.utils.BigDecimalNormalizer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Core domain entity representing a single candlestick for chart data
 */
public class Candlestick {
    private final String id;
    private final Symbol symbol;
    private final Instant timestamp;
    private final TimeInterval interval;
    private final Money open;
    private final Money high;
    private final Money low;
    private final Money close;
    private final BigDecimal volume;

    public Candlestick(String id, Symbol symbol, Instant timestamp, TimeInterval interval,
                       Money open, Money high, Money low, Money close, BigDecimal volume) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.interval = Objects.requireNonNull(interval, "Interval cannot be null");
        this.open = Objects.requireNonNull(open, "Open price cannot be null");
        this.high = Objects.requireNonNull(high, "High price cannot be null");
        this.low = Objects.requireNonNull(low, "Low price cannot be null");
        this.close = Objects.requireNonNull(close, "Close price cannot be null");
        this.volume = Objects.requireNonNull(volume, "Volume cannot be null");

        validateOHLC();
        validatePriceCurrency();
        validateVolume();
    }

    // ===== GETTERS =====
    public String getId() { return id; }
    public Symbol getSymbol() { return symbol; }
    public Instant getTimestamp() { return timestamp; }
    public TimeInterval getInterval() { return interval; }
    public Money getOpen() { return open; }
    public Money getHigh() { return high; }
    public Money getLow() { return low; }
    public Money getClose() { return close; }
    public BigDecimal getVolume() { return BigDecimalNormalizer.normalize(volume); }

    // ===== BUSINESS LOGIC METHODS =====

    /**
     * Returns true if this is a bullish (green) candle
     */
    public boolean isBullish() {
        return close.isGreaterThan(open);
    }

    /**
     * Returns true if this is a bearish (red) candle
     */
    public boolean isBearish() {
        return close.isLessThan(open);
    }

    /**
     * Returns true if this is a doji (open equals close)
     */
    public boolean isDoji() {
        return close.equals(open);
    }

    /**
     * Returns the size of the candle body (absolute difference between open and close)
     */
    public Money getBodySize() {
        return close.subtract(open).abs();
    }

    /**
     * Returns the total range of the candle (high minus low)
     */
    public Money getRange() {
        return high.subtract(low);
    }

    /**
     * Returns the upper shadow length
     */
    public Money getUpperShadow() {
        Money topPrice = isBullish() ? close : open;
        return high.subtract(topPrice);
    }

    /**
     * Returns the lower shadow length
     */
    public Money getLowerShadow() {
        Money bottomPrice = isBullish() ? open : close;
        return bottomPrice.subtract(low);
    }

    /**
     * Returns the typical price (high + low + close) / 3
     */
    public Money getTypicalPrice() {
        Money sum = high.add(low).add(close);
        return sum.divide(new BigDecimal("3"));
    }

    /**
     * Returns the weighted price considering volume
     */
    public Money getWeightedPrice() {
        if (volume.compareTo(BigDecimal.ZERO) == 0) {
            return getTypicalPrice();
        }
        Money totalValue = getTypicalPrice().multiply(volume);
        return totalValue.divide(volume);
    }

    // ===== CHART CONVERSION =====

    /**
     * Converts this candlestick to chart data format
     */
    public ChartData toChartData() {
        return new OHLCChartData(
                timestamp.getEpochSecond(),
                open.getAmount().doubleValue(),
                high.getAmount().doubleValue(),
                low.getAmount().doubleValue(),
                close.getAmount().doubleValue(),
                volume.doubleValue()
        );
    }

    // ===== VALIDATION =====

    private void validateOHLC() {
        if (high.isLessThan(low)) {
            throw new IllegalArgumentException("High price must be >= Low price");
        }
        if (open.isGreaterThan(high) || open.isLessThan(low)) {
            throw new IllegalArgumentException("Open price must be between High and Low");
        }
        if (close.isGreaterThan(high) || close.isLessThan(low)) {
            throw new IllegalArgumentException("Close price must be between High and Low");
        }
    }

    private void validatePriceCurrency() {
        Currency expectedCurrency = symbol.getQuoteCurrency();
        if (!open.getCurrency().equals(expectedCurrency) ||
                !high.getCurrency().equals(expectedCurrency) ||
                !low.getCurrency().equals(expectedCurrency) ||
                !close.getCurrency().equals(expectedCurrency)) {
            throw new IllegalArgumentException("All prices must use symbol's quote currency: " + expectedCurrency);
        }
    }

    private void validateVolume() {
        if (volume.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Volume cannot be negative");
        }
    }

    // ===== OBJECT METHODS =====

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Candlestick that = (Candlestick) obj;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Candlestick[%s: %s %s O:%s H:%s L:%s C:%s V:%s %s]",
                id, symbol.getCode(), interval.getCode(),
                open.toDisplayString(), high.toDisplayString(),
                low.toDisplayString(), close.toDisplayString(),
                BigDecimalNormalizer.normalize(volume).toPlainString(),
                isBullish() ? "📈" : isBearish() ? "📉" : "➡️");
    }
}