package core.ms.robot.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BotStatusDTO {
    private String botId;
    private String portfolioId;
    private String botName;
    private String strategy;
    private String symbol;
    private String status;
    private Integer tradesExecuted;
    private LocalDateTime lastActionTime;
    private BigDecimal lastKnownPrice;
    private List<String> recentHistory;

    // Getters and Setters
    public String getBotId() { return botId; }
    public void setBotId(String botId) { this.botId = botId; }
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public String getBotName() { return botName; }
    public void setBotName(String botName) { this.botName = botName; }
    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTradesExecuted() { return tradesExecuted; }
    public void setTradesExecuted(Integer tradesExecuted) { this.tradesExecuted = tradesExecuted; }
    public LocalDateTime getLastActionTime() { return lastActionTime; }
    public void setLastActionTime(LocalDateTime lastActionTime) { this.lastActionTime = lastActionTime; }
    public BigDecimal getLastKnownPrice() { return lastKnownPrice; }
    public void setLastKnownPrice(BigDecimal lastKnownPrice) { this.lastKnownPrice = lastKnownPrice; }
    public List<String> getRecentHistory() { return recentHistory; }
    public void setRecentHistory(List<String> recentHistory) { this.recentHistory = recentHistory; }
}