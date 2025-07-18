package core.ms.order.domain.ports.inbound;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Result type for transaction operations
 */
public class TransactionResult {
    private final boolean success;
    private final String transactionId;
    private final String message;
    private final LocalDateTime timestamp;
    private final List<String> errors;

    public TransactionResult(boolean success, String transactionId, String message, List<String> errors) {
        this.success = success;
        this.transactionId = transactionId;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.errors = errors != null ? errors : List.of();
    }

    public static TransactionResult success(String transactionId, String message) {
        return new TransactionResult(true, transactionId, message, null);
    }

    public static TransactionResult failure(String transactionId, String message, List<String> errors) {
        return new TransactionResult(false, transactionId, message, errors);
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getTransactionId() { return transactionId; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<String> getErrors() { return errors; }
}