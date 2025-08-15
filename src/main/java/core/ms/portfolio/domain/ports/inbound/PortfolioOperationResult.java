package core.ms.portfolio.domain.ports.inbound;

import java.time.LocalDateTime;
import java.util.List;

public class PortfolioOperationResult {
    private final boolean success;
    private final String message;
    private final String portfolioId;
    private final LocalDateTime timestamp;
    private final List<String> errors;

    private PortfolioOperationResult(Builder builder) {
        this.success = builder.success;
        this.message = builder.message;
        this.portfolioId = builder.portfolioId;
        this.timestamp = LocalDateTime.now();
        this.errors = builder.errors;
    }

    // Builder pattern
    public static class Builder {
        private boolean success;
        private String message;
        private String portfolioId;
        private List<String> errors = List.of();

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder portfolioId(String portfolioId) {
            this.portfolioId = portfolioId;
            return this;
        }

        public Builder errors(List<String> errors) {
            this.errors = errors;
            return this;
        }

        public PortfolioOperationResult build() {
            return new PortfolioOperationResult(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getPortfolioId() { return portfolioId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<String> getErrors() { return errors; }
}