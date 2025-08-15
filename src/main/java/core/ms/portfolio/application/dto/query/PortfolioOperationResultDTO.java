package core.ms.portfolio.application.dto.query;

import java.time.LocalDateTime;
import java.util.List;

public class PortfolioOperationResultDTO {
    private boolean success;
    private String portfolioId;
    private String message;
    private LocalDateTime timestamp;
    private List<String> errors;

    public PortfolioOperationResultDTO() {}

    public PortfolioOperationResultDTO(boolean success, String portfolioId,
                                       String message, LocalDateTime timestamp,
                                       List<String> errors) {
        this.success = success;
        this.portfolioId = portfolioId;
        this.message = message;
        this.timestamp = timestamp;
        this.errors = errors;
    }

    // Static factory methods
    public static PortfolioOperationResultDTO success(String portfolioId, String message) {
        return new PortfolioOperationResultDTO(true, portfolioId, message,
                LocalDateTime.now(), null);
    }

    public static PortfolioOperationResultDTO error(String portfolioId, String message) {
        return new PortfolioOperationResultDTO(false, portfolioId, message,
                LocalDateTime.now(), List.of(message));
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}