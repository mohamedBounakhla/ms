package core.ms.market_engine;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OrderResult {
    private final String orderId;
    private final boolean success;
    private final String message;
    private final LocalDateTime timestamp;
    private final List<String> transactionIds;

    public OrderResult(String orderId, boolean success, String message, List<String> transactionIds) {
        this.orderId = Objects.requireNonNull(orderId, "Order ID cannot be null");
        this.success = success;
        this.message = Objects.requireNonNull(message, "Message cannot be null");
        this.timestamp = LocalDateTime.now();
        this.transactionIds = transactionIds != null ? new ArrayList<>(transactionIds) : new ArrayList<>();
    }

    // Static factory methods
    public static OrderResult accepted(String orderId) {
        return new OrderResult(orderId, true, "Order accepted", new ArrayList<>());
    }

    public static OrderResult acceptedWithTransactions(String orderId, List<String> transactionIds) {
        return new OrderResult(orderId, true, "Order accepted and executed", transactionIds);
    }

    public static OrderResult rejected(String orderId, String reason) {
        return new OrderResult(orderId, false, reason, new ArrayList<>());
    }

    // Getters
    public String getOrderId() {
        return orderId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public List<String> getTransactionIds() {
        return new ArrayList<>(transactionIds);
    }

    @Override
    public String toString() {
        return "OrderResult{" +
                "orderId='" + orderId + '\'' +
                ", success=" + success +
                ", message='" + message + '\'' +
                ", transactionIds=" + transactionIds.size() +
                '}';
    }
}