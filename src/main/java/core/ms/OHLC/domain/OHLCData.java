package core.ms.OHLC.domain;

import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregate root for managing collections of candlesticks
 * This was missing from the original implementation!
 */
public class OHLCData {
    private final String id;
    private final Symbol symbol;
    private final TimeInterval interval;
    private final List<Candlestick> candlesticks;
    private Instant lastUpdated;

    public OHLCData(String id, Symbol symbol, TimeInterval interval) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");
        this.interval = Objects.requireNonNull(interval, "Interval cannot be null");
        this.candlesticks = new ArrayList<>();
        this.lastUpdated = Instant.now();
    }

    // ===== GETTERS =====

    public String getId() { return id; }
    public Symbol getSymbol() { return symbol; }
    public TimeInterval getInterval() { return interval; }
    public List<Candlestick> getAllCandles() { return new ArrayList<>(candlesticks); }
    public boolean isEmpty() { return candlesticks.isEmpty(); }
    public int size() { return candlesticks.size(); }
    public Instant getLastUpdated() { return lastUpdated; }

    // ===== AGGREGATE OPERATIONS =====

    /**
     * Adds a new candlestick to this collection
     */
    public void addCandle(Candlestick candle) {
        Objects.requireNonNull(candle, "Candlestick cannot be null");
        validateCandle(candle);

        candlesticks.add(candle);
        sortCandlesByTimestamp();
        lastUpdated = Instant.now();
    }

    /**
     * Gets candlesticks within a specific time range
     */
    public List<Candlestick> getCandlesByTimeRange(Instant startTime, Instant endTime) {
        Objects.requireNonNull(startTime, "Start time cannot be null");
        Objects.requireNonNull(endTime, "End time cannot be null");

        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time must be before or equal to end time");
        }

        return candlesticks.stream()
                .filter(candle -> !candle.getTimestamp().isBefore(startTime) &&
                        !candle.getTimestamp().isAfter(endTime))
                .sorted(Comparator.comparing(Candlestick::getTimestamp))
                .collect(Collectors.toList());
    }

    /**
     * Gets the latest (most recent) candlestick
     */
    public Optional<Candlestick> getLatestCandle() {
        return candlesticks.stream()
                .max(Comparator.comparing(Candlestick::getTimestamp));
    }

    // ===== QUERY METHODS =====

    public String getSymbolCode() {
        return symbol.getCode();
    }

    /**
     * Gets the date range covered by this data
     */
    public Optional<DateRange> getDateRange() {
        if (candlesticks.isEmpty()) {
            return Optional.empty();
        }

        Instant earliest = candlesticks.stream()
                .min(Comparator.comparing(Candlestick::getTimestamp))
                .map(Candlestick::getTimestamp)
                .orElse(Instant.now());

        Instant latest = candlesticks.stream()
                .max(Comparator.comparing(Candlestick::getTimestamp))
                .map(Candlestick::getTimestamp)
                .orElse(Instant.now());

        return Optional.of(new DateRange(earliest, latest));
    }

    /**
     * Gets the highest price across all candlesticks
     */
    public Optional<Money> getHighestPrice() {
        return candlesticks.stream()
                .map(Candlestick::getHigh)
                .reduce((money1, money2) -> money1.isGreaterThan(money2) ? money1 : money2);
    }

    /**
     * Gets the lowest price across all candlesticks
     */
    public Optional<Money> getLowestPrice() {
        return candlesticks.stream()
                .map(Candlestick::getLow)
                .reduce((money1, money2) -> money1.isLessThan(money2) ? money1 : money2);
    }

    /**
     * Gets the average volume across all candlesticks
     */
    public BigDecimal getAverageVolume() {
        if (candlesticks.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalVolume = candlesticks.stream()
                .map(Candlestick::getVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalVolume.divide(new BigDecimal(candlesticks.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    // ===== CHART CONVERSION =====

    /**
     * Converts this data to a chart series
     */
    public ChartSeries toChartSeries() {
        List<ChartData> chartDataList = candlesticks.stream()
                .sorted(Comparator.comparing(Candlestick::getTimestamp))
                .map(Candlestick::toChartData)
                .collect(Collectors.toList());

        return new OHLCChartSeries(
                id,
                symbol.getCode() + " " + interval.getCode(),
                symbol.getCode(),
                interval.getCode(),
                chartDataList
        );
    }

    // ===== VALIDATION =====

    private void validateCandle(Candlestick candle) {
        if (!candle.getSymbol().equals(this.symbol)) {
            throw new IllegalArgumentException("Candlestick symbol must match OHLCData symbol");
        }
        if (!candle.getInterval().equals(this.interval)) {
            throw new IllegalArgumentException("Candlestick interval must match OHLCData interval");
        }
    }

    private void sortCandlesByTimestamp() {
        candlesticks.sort(Comparator.comparing(Candlestick::getTimestamp));
    }

    // ===== OBJECT METHODS =====

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OHLCData ohlcData = (OHLCData) obj;
        return Objects.equals(id, ohlcData.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("OHLCData[%s: %s %s, %d candlesticks, updated: %s]",
                id, symbol.getCode(), interval.getCode(), candlesticks.size(), lastUpdated);
    }
}