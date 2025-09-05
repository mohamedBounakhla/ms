package core.ms.robot.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BotTradeEventDTO {
    private String botId;
    private String botName;
    private String portfolioId;
    private String action; // "BUY", "SELL", "HOLD"
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal price;
    private String reason;
    private String status; // "SUCCESS", "FAILED"
    private String errorMessage;
    private LocalDateTime timestamp;

    // Constructor for successful trade
    public BotTradeEventDTO(String botId, String botName, String portfolioId,
                            String action, String symbol, BigDecimal quantity,
                            BigDecimal price, String reason) {
        this.botId = botId;
        this.botName = botName;
        this.portfolioId = portfolioId;
        this.action = action;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.reason = reason;
        this.status = "SUCCESS";
        this.timestamp = LocalDateTime.now();
    }

    // Constructor for failed trade
    public BotTradeEventDTO(String botId, String botName, String portfolioId,
                            String action, String symbol, String errorMessage) {
        this.botId = botId;
        this.botName = botName;
        this.portfolioId = portfolioId;
        this.action = action;
        this.symbol = symbol;
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getBotId() { return botId; }
    public void setBotId(String botId) { this.botId = botId; }

    public String getBotName() { return botName; }
    public void setBotName(String botName) { this.botName = botName; }

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}