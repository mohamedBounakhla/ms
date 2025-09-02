package core.ms.order_book.domain.ports.inbound;


import java.time.LocalDateTime;


public class OrderBookOperationResult {
    private final boolean success;
    private final String message;
    private final String orderId;
    private final LocalDateTime timestamp;

    private OrderBookOperationResult(Builder builder) {
        this.success = builder.success;
        this.message = builder.message;
        this.orderId = builder.orderId;
        this.timestamp = LocalDateTime.now();
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getOrderId() { return orderId; }
    public LocalDateTime getTimestamp() { return timestamp; }

    // Builder
    public static class Builder {
        private boolean success;
        private String message;
        private String orderId;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
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