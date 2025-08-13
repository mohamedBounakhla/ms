package core.ms.portfolio.domain.value_object.state;

import core.ms.portfolio.domain.entities.Reservation;
import core.ms.portfolio.domain.value_object.validation.ReservationValidationDSL;
import core.ms.portfolio.domain.value_object.validation.ValidationResult;

public class PendingState extends AbstractReservationState {

    public PendingState() {
        super(StateType.PENDING);
    }

    @Override
    public boolean canExecute() {
        return true;
    }

    @Override
    public boolean canCancel() {
        return true;
    }

    @Override
    public boolean canExpire() {
        return true;
    }

    @Override
    public ValidationResult validateExecute(ReservationTransitionContext context) {
        return ReservationValidationDSL.builder()
                .withContext(context)
                .validateTimeWindow()
                .validateResourceAvailability()
                .validateNoDoubleExecution()
                .build();
    }

    @Override
    public ValidationResult validateCancel(ReservationTransitionContext context) {
        return ReservationValidationDSL.builder()
                .withContext(context)
                .validateResourcesFullyReserved()
                .validateCancellationAuthorization()
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
        reservation.recordStateChange("Entered PENDING state - resources locked");
    }
}