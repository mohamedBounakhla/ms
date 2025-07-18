package core.ms.order.domain.ports.inbound;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Result type for order domain operations (create, cancel, update)
 * Different from market_engine.OrderResult which is for trade execution results
 */
public class OrderOperationResult {
    private final boolean success;
    private final String orderId;
    private final String message;
    private final LocalDateTime timestamp;
    private final List<String> errors;

    public OrderOperationResult(boolean success, String orderId, String message, List<String> errors) {
        this.success = success;
        this.orderId = orderId;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.errors = errors != null ? errors : List.of();
    }

    public static OrderOperationResult success(String orderId, String message) {
        return new OrderOperationResult(true, orderId, message, null);
    }

    public static OrderOperationResult failure(String orderId, String message, List<String> errors) {
        return new OrderOperationResult(false, orderId, message, errors);
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getOrderId() { return orderId; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<String> getErrors() { return errors; }
}