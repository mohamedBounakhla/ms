package core.ms.portfolio.domain.value_object;

import core.ms.portfolio.domain.Reservation;

public class ConfirmedState extends AbstractReservationState {

    public ConfirmedState() {
        super(StateType.CONFIRMED);
    }

    @Override
    public boolean canConfirm() {
        return false;
    }

    @Override
    public boolean canRelease() {
        return false;
    }

    @Override
    public boolean canExpire() {
        return false;
    }

    @Override
    public void onEntry(Reservation<?> reservation) {
        // Consume the reserved resources
        reservation.consumeResources();
        reservation.recordStateChange("Reservation CONFIRMED - resources consumed");
    }
}