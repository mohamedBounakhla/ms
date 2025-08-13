package core.ms.portfolio.domain.value_object.state;

import core.ms.portfolio.domain.entities.Reservation;
import core.ms.portfolio.domain.value_object.validation.ValidationResult;

public interface ReservationState {

    // State identification
    String getStateName();
    StateType getStateType();

    // State capabilities
    boolean isTerminal();
    boolean canExecute();
    boolean canCancel();
    boolean canExpire();

    // State transitions with validation
    ValidationResult validateExecute(ReservationTransitionContext context);
    ValidationResult validateCancel(ReservationTransitionContext context);
    ValidationResult validateExpire(ReservationTransitionContext context);

    // Lifecycle hooks
    void onEntry(Reservation<?> reservation);
    void onExit(Reservation<?> reservation);

    // State type enumeration with clear names
    enum StateType {
        PENDING,    // Order created, resources locked
        EXECUTED,   // Order matched/filled, resources consumed
        CANCELLED,  // Order cancelled, resources freed
        EXPIRED     // Timeout reached, resources freed
    }
}
