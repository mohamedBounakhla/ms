package core.ms.order.application.dto.query;

import java.time.LocalDateTime;
import java.util.List;

public class OrderOperationResultDTO {
    private boolean success;
    private String orderId;
    private String message;
    private LocalDateTime timestamp;
    private List<String> errors;

    public OrderOperationResultDTO() {}

    public OrderOperationResultDTO(boolean success, String orderId, String message,
                                   LocalDateTime timestamp, List<String> errors) {
        this.success = success;
        this.orderId = orderId;
        this.message = message;
        this.timestamp = timestamp;
        this.errors = errors;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}