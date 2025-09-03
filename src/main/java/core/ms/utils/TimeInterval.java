package core.ms.utils;

public enum TimeInterval {
    ONE_MINUTE("1m", 60000),
    FIVE_MINUTES("5m", 300000),
    FIFTEEN_MINUTES("15m", 900000),
    ONE_HOUR("1h", 3600000),
    ONE_DAY("1d", 86400000);

    private final String code;
    private final long milliseconds;

    TimeInterval(String code, long milliseconds) {
        this.code = code;
        this.milliseconds = milliseconds;
    }

    public String toString() { return code; }
    public long getMilliseconds() { return milliseconds; }
}