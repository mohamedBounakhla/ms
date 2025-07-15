package core.ms.OHLC.domain;

public interface ChartData {
    long getTime();
    double getOpen();
    double getHigh();
    double getLow();
    double getClose();
    double getVolume();
}
