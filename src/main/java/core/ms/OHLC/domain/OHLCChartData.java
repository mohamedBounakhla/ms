package core.ms.OHLC.domain;

public class OHLCChartData implements ChartData {
    private final long time;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final double volume;

    public OHLCChartData(long time, double open, double high, double low, double close, double volume) {
        this.time = time;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    @Override
    public long getTime() { return time; }
    @Override
    public double getOpen() { return open; }
    @Override
    public double getHigh() { return high; }
    @Override
    public double getLow() { return low; }
    @Override
    public double getClose() { return close; }
    @Override
    public double getVolume() { return volume; }

    @Override
    public String toString() {
        return String.format("ChartData[%d: O:%.2f H:%.2f L:%.2f C:%.2f V:%.2f]",
                time, open, high, low, close, volume);
    }
}