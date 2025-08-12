package core.ms.portfolio.domain.value_object;

import core.ms.portfolio.domain.Reservation;

public interface ReservationState {

    // State identification
    String getStateName();
    StateType getStateType();

    // State capabilities
    boolean isTerminal();
    boolean canConfirm();
    boolean canRelease();
    boolean canExpire();

    // State transitions with validation
    ValidationResult validateConfirm(ReservationTransitionContext context);
    ValidationResult validateRelease(ReservationTransitionContext context);
    ValidationResult validateExpire(ReservationTransitionContext context);

    // Lifecycle hooks
    void onEntry(Reservation<?> reservation);
    void onExit(Reservation<?> reservation);

    // State type enumeration
    enum StateType {
        PENDING,
        CONFIRMED,
        RELEASED,
        EXPIRED
    }
}