package core.ms.portfolio.domain.value_object;

import core.ms.portfolio.domain.Reservation;

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
        return !canConfirm() && !canRelease() && !canExpire();
    }

    // Default implementations - most states can't transition
    @Override
    public ValidationResult validateConfirm(ReservationTransitionContext context) {
        return ValidationResult.invalid(
                String.format("Cannot confirm reservation in %s state", stateName)
        );
    }

    @Override
    public ValidationResult validateRelease(ReservationTransitionContext context) {
        return ValidationResult.invalid(
                String.format("Cannot release reservation in %s state", stateName)
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