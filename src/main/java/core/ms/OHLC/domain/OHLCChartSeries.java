package core.ms.OHLC.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of ChartSeries for OHLC data
 */
public class OHLCChartSeries implements ChartSeries {
    private final String id;
    private final String name;
    private final String symbol;
    private final String interval;
    private final List<ChartData> data;

    public OHLCChartSeries(String id, String name, String symbol, String interval, List<ChartData> data) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");
        this.interval = Objects.requireNonNull(interval, "Interval cannot be null");
        this.data = new ArrayList<>(Objects.requireNonNull(data, "Data cannot be null"));
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getName() { return name; }

    @Override
    public String getSymbol() { return symbol; }

    @Override
    public String getInterval() { return interval; }

    @Override
    public List<ChartData> getData() { return new ArrayList<>(data); }

    @Override
    public boolean isEmpty() { return data.isEmpty(); }

    @Override
    public int size() { return data.size(); }

    @Override
    public String toString() {
        return String.format("ChartSeries[%s: %s, %d points]", id, name, data.size());
    }
}