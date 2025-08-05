package core.ms.OHLC.domain;

import core.ms.shared.money.Symbol;

import java.util.Objects;

/**
 * Value object containing metadata for chart display
 */
public class ChartMetadata {
    private final Symbol symbol;
    private final TimeInterval interval;
    private final DateRange dateRange;
    private final int totalCandles;
    private final PriceRange priceRange;
    private final VolumeRange volumeRange;

    public ChartMetadata(Symbol symbol, TimeInterval interval, DateRange dateRange,
                         int totalCandles, PriceRange priceRange, VolumeRange volumeRange) {
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");
        this.interval = Objects.requireNonNull(interval, "Interval cannot be null");
        this.dateRange = dateRange; // Can be null for empty data
        this.totalCandles = totalCandles;
        this.priceRange = priceRange; // Can be null for empty data
        this.volumeRange = volumeRange; // Can be null for empty data

        if (totalCandles < 0) {
            throw new IllegalArgumentException("Total candles cannot be negative");
        }
    }

    public Symbol getSymbol() { return symbol; }
    public TimeInterval getInterval() { return interval; }
    public DateRange getDateRange() { return dateRange; }
    public int getTotalCandles() { return totalCandles; }
    public PriceRange getPriceRange() { return priceRange; }
    public VolumeRange getVolumeRange() { return volumeRange; }

    public boolean hasData() {
        return totalCandles > 0;
    }

    @Override
    public String toString() {
        return String.format("ChartMetadata[%s %s, %d candles, %s]",
                symbol.getCode(), interval.getCode(), totalCandles,
                dateRange != null ? dateRange.toString() : "no data");
    }
}