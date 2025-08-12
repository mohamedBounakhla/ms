package core.ms.portfolio.domain;

import core.ms.order.domain.entities.IOrder;
import core.ms.portfolio.domain.value_object.ReservationState;
import core.ms.portfolio.domain.value_object.ReservationStates;
import core.ms.portfolio.domain.value_object.ReservationTransitionContext;
import core.ms.portfolio.domain.value_object.ValidationResult;
import core.ms.shared.money.Money;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class Reservation<T extends IOrder> {

    protected final String reservationId;
    protected final T order;
    protected final Instant createdAt;
    protected Instant lastActivityTime;
    protected final Instant expirationTime;
    protected BigDecimal consumedAmount;
    protected final Portfolio portfolio;

    // State pattern composition
    protected ReservationState state;

    // Audit trail
    protected final List<String> stateChangeLog;

    // Configuration
    private static final Duration DEFAULT_EXPIRATION = Duration.ofMinutes(5);

    protected Reservation(String reservationId, T order, Portfolio portfolio) {
        this.reservationId = Objects.requireNonNull(reservationId, "Reservation ID cannot be null");
        this.order = Objects.requireNonNull(order, "Order cannot be null");
        this.portfolio = Objects.requireNonNull(portfolio, "Portfolio cannot be null");
        this.createdAt = Instant.now();
        this.lastActivityTime = this.createdAt;
        this.expirationTime = createdAt.plus(DEFAULT_EXPIRATION);
        this.consumedAmount = BigDecimal.ZERO;
        this.state = ReservationStates.PENDING;
        this.stateChangeLog = new ArrayList<>();

        // Record initial state
        this.state.onEntry(this);
    }

    // ===== STATE TRANSITIONS =====

    public void confirm(ReservationTransitionContext.ExecutionDetails executionDetails) {
        ReservationTransitionContext context = new ReservationTransitionContext(this, portfolio)
                .withExecutionDetails(executionDetails);

        ValidationResult validation = state.validateConfirm(context);

        if (!validation.isValid()) {
            throw new InvalidStateTransitionException(
                    "Cannot confirm reservation: " + validation.getErrorMessage()
            );
        }

        transitionTo(ReservationStates.CONFIRMED);
        recordActivity();
    }

    public void release(ReservationTransitionContext.ReleaseContext releaseContext) {
        ReservationTransitionContext context = new ReservationTransitionContext(this, portfolio)
                .withReleaseContext(releaseContext);

        ValidationResult validation = state.validateRelease(context);

        if (!validation.isValid()) {
            throw new InvalidStateTransitionException(
                    "Cannot release reservation: " + validation.getErrorMessage()
            );
        }

        transitionTo(ReservationStates.RELEASED);
        recordActivity();
    }

    public void expire() {
        ReservationTransitionContext context = new ReservationTransitionContext(this, portfolio);

        ValidationResult validation = state.validateExpire(context);

        if (!validation.isValid()) {
            throw new InvalidStateTransitionException(
                    "Cannot expire reservation: " + validation.getErrorMessage()
            );
        }

        transitionTo(ReservationStates.EXPIRED);
        recordActivity();
    }

    // ===== STATE MANAGEMENT =====

    protected void transitionTo(ReservationState newState) {
        ReservationState oldState = this.state;
        oldState.onExit(this);
        this.state = newState;
        newState.onEntry(this);

        recordStateChange(String.format("Transitioned from %s to %s",
                oldState.getStateName(), newState.getStateName()));
    }

    public void recordStateChange(String message) {
        String logEntry = String.format("[%s] %s", Instant.now(), message);
        stateChangeLog.add(logEntry);
    }

    protected void recordActivity() {
        this.lastActivityTime = Instant.now();
    }

    // ===== ABSTRACT METHODS =====

    public abstract Money getReservedAmount();
    public abstract void consumeResources();
    public abstract void freeResources();
    public abstract BigDecimal getReservedQuantity();

    // ===== QUERY METHODS =====

    public boolean isActive() {
        return !state.isTerminal();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expirationTime);
    }

    public boolean canConfirm() {
        return state.canConfirm();
    }

    public boolean canRelease() {
        return state.canRelease();
    }

    public boolean canExpire() {
        return state.canExpire();
    }

    // ===== GETTERS =====

    public String getReservationId() { return reservationId; }
    public T getOrder() { return order; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpirationTime() { return expirationTime; }
    public Instant getLastActivityTime() { return lastActivityTime; }
    public BigDecimal getConsumedAmount() { return consumedAmount; }
    public ReservationState getState() { return state; }
    public String getStateName() { return state.getStateName(); }
    public List<String> getStateChangeLog() { return new ArrayList<>(stateChangeLog); }

    // ===== EXCEPTION =====

    public static class InvalidStateTransitionException extends RuntimeException {
        public InvalidStateTransitionException(String message) {
            super(message);
        }
    }
}