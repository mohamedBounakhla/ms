package core.ms.OHLC.domain;

public interface ChartSeries {
    String getId();
    String getName();
    String getSymbol();
    String getInterval();
    java.util.List<ChartData> getData();
    boolean isEmpty();
    int size();
}
