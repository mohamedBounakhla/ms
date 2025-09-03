package core.ms.order_book.application.dto.query;

import java.time.LocalDateTime;

public class CandlestickUpdate {
    private String symbol;
    private String interval;
    private CandlestickDTO candle;
    private LocalDateTime timestamp;

    public CandlestickUpdate() {
        this.timestamp = LocalDateTime.now();
    }

    public CandlestickUpdate(String symbol, String interval, CandlestickDTO candle) {
        this.symbol = symbol;
        this.interval = interval;
        this.candle = candle;
        this.timestamp = LocalDateTime.now();
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getInterval() { return interval; }
    public void setInterval(String interval) { this.interval = interval; }
    public CandlestickDTO getCandle() { return candle; }
    public void setCandle(CandlestickDTO candle) { this.candle = candle; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}