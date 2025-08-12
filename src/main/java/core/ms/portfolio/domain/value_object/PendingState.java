package core.ms.portfolio.domain.value_object;

import core.ms.portfolio.domain.*;

public class PendingState extends AbstractReservationState {

    public PendingState() {
        super(StateType.PENDING);
    }

    @Override
    public boolean canConfirm() {
        return true;
    }

    @Override
    public boolean canRelease() {
        return true;
    }

    @Override
    public boolean canExpire() {
        return true;
    }

    @Override
    public ValidationResult validateConfirm(ReservationTransitionContext context) {
        return ReservationValidationDSL.builder()
                .withContext(context)
                .validateTimeWindow()
                .validateExecution()
                .validateResourceAvailability()
                .validateNoDoubleSpending()
                .build();
    }

    @Override
    public ValidationResult validateRelease(ReservationTransitionContext context) {
        return ReservationValidationDSL.builder()
                .withContext(context)
                .validateResourcesFullyReserved()
                .validateReleaseAuthorization()
                .build();
    }

    @Override
    public ValidationResult validateExpire(ReservationTransitionContext context) {
        return ReservationValidationDSL.builder()
                .withContext(context)
                .validateExpirationTime()
                .validateNoRecentActivity()
                .validateResourcesFullyReserved()
                .build();
    }

    @Override
    public void onEntry(Reservation<?> reservation) {
        reservation.recordStateChange("Entered PENDING state");
    }

    @Override
    public void onExit(Reservation<?> reservation) {
        reservation.recordStateChange("Exiting PENDING state");
    }
}