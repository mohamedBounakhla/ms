package core.ms.order_book.domain.ports.inbound;

import core.ms.order_book.domain.events.OrderMatchedEvent;

import java.time.LocalDateTime;
import java.util.List;

public class OrderBookOperationResult {
    private final boolean success;
    private final String message;
    private final List<OrderMatchedEvent> matchEvents;
    private final LocalDateTime timestamp;
    private final String orderId;

    private OrderBookOperationResult(Builder builder) {
        this.success = builder.success;
        this.message = builder.message;
        this.matchEvents = builder.matchEvents;
        this.timestamp = LocalDateTime.now();
        this.orderId = builder.orderId;
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<OrderMatchedEvent> getMatchEvents() { return matchEvents; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getOrderId() { return orderId; }
    public boolean hasMatches() { return matchEvents != null && !matchEvents.isEmpty(); }
    public int getMatchCount() { return matchEvents != null ? matchEvents.size() : 0; }

    // Builder
    public static class Builder {
        private boolean success;
        private String message;
        private List<OrderMatchedEvent> matchEvents = List.of();
        private String orderId;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder matchEvents(List<OrderMatchedEvent> matchEvents) {
            this.matchEvents = matchEvents;
            return this;
        }

        public Builder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public OrderBookOperationResult build() {
            return new OrderBookOperationResult(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}