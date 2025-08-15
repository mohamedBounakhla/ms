package core.ms.market_engine.application.dto;

import java.time.LocalDateTime;

public class OrderFlowDTO {
    private long totalOrders;
    private long matchedOrders;
    private long pendingOrders;
    private LocalDateTime timestamp;

    public OrderFlowDTO(long totalOrders, long matchedOrders,
                        long pendingOrders, LocalDateTime timestamp) {
        this.totalOrders = totalOrders;
        this.matchedOrders = matchedOrders;
        this.pendingOrders = pendingOrders;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }
    public long getMatchedOrders() { return matchedOrders; }
    public void setMatchedOrders(long matchedOrders) { this.matchedOrders = matchedOrders; }
    public long getPendingOrders() { return pendingOrders; }
    public void setPendingOrders(long pendingOrders) { this.pendingOrders = pendingOrders; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}