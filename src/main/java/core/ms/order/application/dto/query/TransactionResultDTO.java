package core.ms.order.application.dto.query;

import java.time.LocalDateTime;
import java.util.List;

public class TransactionResultDTO {
    private boolean success;
    private String transactionId;
    private String message;
    private LocalDateTime timestamp;
    private List<String> errors;

    public TransactionResultDTO() {}

    public TransactionResultDTO(boolean success, String transactionId, String message,
                                LocalDateTime timestamp, List<String> errors) {
        this.success = success;
        this.transactionId = transactionId;
        this.message = message;
        this.timestamp = timestamp;
        this.errors = errors;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}