package core.ms.portfolio.domain.value_object;

import core.ms.portfolio.domain.Portfolio;
import core.ms.portfolio.domain.Reservation;

import java.math.BigDecimal;
import java.time.Instant;

public class ReservationTransitionContext {

    private final Reservation<?> reservation;
    private final Portfolio portfolio;
    private ExecutionDetails executionDetails;
    private ReleaseContext releaseContext;
    private Instant transitionTime;

    public ReservationTransitionContext(Reservation<?> reservation, Portfolio portfolio) {
        this.reservation = reservation;
        this.portfolio = portfolio;
        this.transitionTime = Instant.now();
    }

    // Builder pattern for optional context
    public ReservationTransitionContext withExecutionDetails(ExecutionDetails details) {
        this.executionDetails = details;
        return this;
    }

    public ReservationTransitionContext withReleaseContext(ReleaseContext context) {
        this.releaseContext = context;
        return this;
    }

    public ReservationTransitionContext withTransitionTime(Instant time) {
        this.transitionTime = time;
        return this;
    }

    // Getters
    public Reservation<?> getReservation() { return reservation; }
    public Portfolio getPortfolio() { return portfolio; }
    public ExecutionDetails getExecutionDetails() { return executionDetails; }
    public ReleaseContext getReleaseContext() { return releaseContext; }
    public Instant getTransitionTime() { return transitionTime; }

    // Context classes
    public static class ExecutionDetails {
        private final BigDecimal executedQuantity;
        private final BigDecimal executedPrice;
        private final String transactionId;

        public ExecutionDetails(BigDecimal executedQuantity, BigDecimal executedPrice, String transactionId) {
            this.executedQuantity = executedQuantity;
            this.executedPrice = executedPrice;
            this.transactionId = transactionId;
        }

        public BigDecimal getExecutedQuantity() { return executedQuantity; }
        public BigDecimal getExecutedPrice() { return executedPrice; }
        public String getTransactionId() { return transactionId; }
    }

    public static class ReleaseContext {
        private final String reason;
        private final boolean systemTriggered;
        private final String userId;

        public ReleaseContext(String reason, boolean systemTriggered, String userId) {
            this.reason = reason;
            this.systemTriggered = systemTriggered;
            this.userId = userId;
        }

        public String getReason() { return reason; }
        public boolean isSystemTriggered() { return systemTriggered; }
        public boolean isAuthorized() { return userId != null; }
        public boolean hasValidReason() { return reason != null && !reason.isEmpty(); }
    }
}