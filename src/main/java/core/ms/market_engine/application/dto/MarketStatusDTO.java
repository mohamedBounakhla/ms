package core.ms.market_engine.application.dto;

import java.time.LocalDateTime;

public class MarketStatusDTO {
    private String status;
    private long processedOrders;
    private long processedMatches;
    private LocalDateTime timestamp;

    public MarketStatusDTO(String status, long processedOrders,
                           long processedMatches, LocalDateTime timestamp) {
        this.status = status;
        this.processedOrders = processedOrders;
        this.processedMatches = processedMatches;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getProcessedOrders() { return processedOrders; }
    public void setProcessedOrders(long processedOrders) { this.processedOrders = processedOrders; }
    public long getProcessedMatches() { return processedMatches; }
    public void setProcessedMatches(long processedMatches) { this.processedMatches = processedMatches; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}