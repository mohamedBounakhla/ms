package core.ms.robot.config;

import java.math.BigDecimal;

public class BotConfig {
    private String botName;
    private BigDecimal initialCash;
    private String strategy; // "random", "momentum", "marketMaker", "panic"
    private String symbolCode; // "BTC", "ETH", etc.
    private BigDecimal maxOrderSize;
    private BigDecimal minOrderSize;
    private Integer tickIntervalSeconds;
    private BigDecimal riskTolerance; // 0.0 to 1.0
    private boolean autoStart;

    // Constructor
    public BotConfig() {
        this.initialCash = new BigDecimal("10000");
        this.strategy = "random";
        this.symbolCode = "BTC";
        this.maxOrderSize = new BigDecimal("1");
        this.minOrderSize = new BigDecimal("0.01");
        this.tickIntervalSeconds = 5;
        this.riskTolerance = new BigDecimal("0.5");
        this.autoStart = true;
    }

    // Builder pattern for easy configuration
    public static BotConfigBuilder builder() {
        return new BotConfigBuilder();
    }

    public static class BotConfigBuilder {
        private BotConfig config = new BotConfig();

        public BotConfigBuilder name(String name) {
            config.botName = name;
            return this;
        }

        public BotConfigBuilder initialCash(BigDecimal cash) {
            config.initialCash = cash;
            return this;
        }

        public BotConfigBuilder strategy(String strategy) {
            config.strategy = strategy;
            return this;
        }

        public BotConfigBuilder symbol(String symbol) {
            config.symbolCode = symbol;
            return this;
        }

        public BotConfigBuilder maxOrderSize(BigDecimal size) {
            config.maxOrderSize = size;
            return this;
        }

        public BotConfig build() {
            return config;
        }
    }

    // Getters and Setters
    public String getBotName() { return botName; }
    public void setBotName(String botName) { this.botName = botName; }
    public BigDecimal getInitialCash() { return initialCash; }
    public void setInitialCash(BigDecimal initialCash) { this.initialCash = initialCash; }
    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
    public String getSymbolCode() { return symbolCode; }
    public void setSymbolCode(String symbolCode) { this.symbolCode = symbolCode; }
    public BigDecimal getMaxOrderSize() { return maxOrderSize; }
    public void setMaxOrderSize(BigDecimal maxOrderSize) { this.maxOrderSize = maxOrderSize; }
    public BigDecimal getMinOrderSize() { return minOrderSize; }
    public void setMinOrderSize(BigDecimal minOrderSize) { this.minOrderSize = minOrderSize; }
    public Integer getTickIntervalSeconds() { return tickIntervalSeconds; }
    public void setTickIntervalSeconds(Integer tickIntervalSeconds) { this.tickIntervalSeconds = tickIntervalSeconds; }
    public BigDecimal getRiskTolerance() { return riskTolerance; }
    public void setRiskTolerance(BigDecimal riskTolerance) { this.riskTolerance = riskTolerance; }
    public boolean isAutoStart() { return autoStart; }
    public void setAutoStart(boolean autoStart) { this.autoStart = autoStart; }
}
