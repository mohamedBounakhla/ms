package core.ms.order_book.application.dto.query;

import core.ms.order_book.infrastructure.events.dto.OrderMatchedEventDTO;

import java.time.LocalDateTime;
import java.util.List;

public class OrderBookOperationResultDTO {
    private boolean success;
    private String message;
    private String orderId;
    private int matchCount;
    private List<OrderMatchedEventDTO> matchEvents;
    private LocalDateTime timestamp;

    public OrderBookOperationResultDTO() {}

    public OrderBookOperationResultDTO(boolean success, String message, String orderId,
                                       int matchCount, List<OrderMatchedEventDTO> matchEvents,
                                       LocalDateTime timestamp) {
        this.success = success;
        this.message = message;
        this.orderId = orderId;
        this.matchCount = matchCount;
        this.matchEvents = matchEvents;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public int getMatchCount() { return matchCount; }
    public void setMatchCount(int matchCount) { this.matchCount = matchCount; }
    public List<OrderMatchedEventDTO> getMatchEvents() { return matchEvents; }
    public void setMatchEvents(List<OrderMatchedEventDTO> matchEvents) { this.matchEvents = matchEvents; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}