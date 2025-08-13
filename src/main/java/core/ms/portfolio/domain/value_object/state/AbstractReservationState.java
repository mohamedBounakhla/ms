package core.ms.portfolio.domain.value_object.state;

import core.ms.portfolio.domain.entities.Reservation;
import core.ms.portfolio.domain.value_object.validation.ValidationResult;

public abstract class AbstractReservationState implements ReservationState {

    protected final StateType stateType;
    protected final String stateName;

    protected AbstractReservationState(StateType stateType) {
        this.stateType = stateType;
        this.stateName = stateType.name();
    }

    @Override
    public String getStateName() {
        return stateName;
    }

    @Override
    public StateType getStateType() {
        return stateType;
    }

    @Override
    public boolean isTerminal() {
        // Terminal states cannot transition anywhere
        return !canExecute() && !canCancel() && !canExpire();
    }

    // Default implementations - most states can't transition
    @Override
    public ValidationResult validateExecute(ReservationTransitionContext context) {
        return ValidationResult.invalid(
                String.format("Cannot execute reservation in %s state", stateName)
        );
    }

    @Override
    public ValidationResult validateCancel(ReservationTransitionContext context) {
        return ValidationResult.invalid(
                String.format("Cannot cancel reservation in %s state", stateName)
        );
    }

    @Override
    public ValidationResult validateExpire(ReservationTransitionContext context) {
        return ValidationResult.invalid(
                String.format("Cannot expire reservation in %s state", stateName)
        );
    }

    @Override
    public void onEntry(Reservation<?> reservation) {
        // Default: no action
    }

    @Override
    public void onExit(Reservation<?> reservation) {
        // Default: no action
    }

    protected ValidationResult success() {
        return ValidationResult.valid();
    }

    protected ValidationResult failure(String reason) {
        return ValidationResult.invalid(reason);
    }
}