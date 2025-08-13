package core.ms.portfolio.domain.entities;

import core.ms.order.domain.entities.IOrder;
import core.ms.portfolio.domain.value_object.ResourceConsumption;
import core.ms.portfolio.domain.value_object.state.ReservationState;
import core.ms.portfolio.domain.value_object.state.ReservationStates;
import core.ms.shared.money.Money;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class Reservation<T extends IOrder> {

    protected final String reservationId;
    protected final T order; //reference if micro services
    protected final Instant createdAt;
    protected Instant lastActivityTime;
    protected final Instant expirationTime;
    protected BigDecimal consumedAmount;
    protected ReservationState state;
    protected final List<String> stateChangeLog;

    private static final Duration DEFAULT_EXPIRATION = Duration.ofMinutes(5);

    protected Reservation(String reservationId, T order) {
        this.reservationId = Objects.requireNonNull(reservationId);
        this.order = Objects.requireNonNull(order);
        this.createdAt = Instant.now();
        this.lastActivityTime = this.createdAt;
        this.expirationTime = createdAt.plus(DEFAULT_EXPIRATION);
        this.consumedAmount = BigDecimal.ZERO;
        this.state = ReservationStates.PENDING;
        this.stateChangeLog = new ArrayList<>();
    }

    // ===== STATE MANAGEMENT =====

    public void transitionTo(ReservationState newState) {
        ReservationState oldState = this.state;
        this.state = newState;
        recordStateChange(String.format("Transitioned from %s to %s",
                oldState.getStateName(), newState.getStateName()));
        recordActivity();
    }

    public void recordStateChange(String message) {
        String logEntry = String.format("[%s] %s", Instant.now(), message);
        stateChangeLog.add(logEntry);
    }

    protected void recordActivity() {
        this.lastActivityTime = Instant.now();
    }

    public void markAsConsumed() {
        this.consumedAmount = getReservedQuantity();
    }

    public void markAsReleased() {
        // Resources are released, consumed amount remains zero
        this.consumedAmount = BigDecimal.ZERO;
    }

    // ===== ABSTRACT METHODS - What is Reserved =====

    /**
     * Returns a description of what resources should be consumed
     */
    public abstract ResourceConsumption getResourceConsumption();

    /**
     * Returns the quantity reserved (for assets) or amount (for cash) as BigDecimal
     */
    public abstract BigDecimal getReservedQuantity();

    /**
     * Returns the monetary amount reserved (mainly for cash reservations)
     */
    public abstract Money getReservedAmount();

    // ===== QUERY METHODS =====

    public boolean isActive() {
        return !state.isTerminal();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expirationTime);
    }

    public boolean canExecute() {
        return state.canExecute();
    }

    public boolean canCancel() {
        return state.canCancel();
    }

    public boolean canExpire() {
        return state.canExpire();
    }

    public boolean isPending() {
        return state.getStateType() == ReservationState.StateType.PENDING;
    }

    public boolean isExecuted() {
        return state.getStateType() == ReservationState.StateType.EXECUTED;
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