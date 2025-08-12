package core.ms.portfolio.domain.value_object;

import core.ms.portfolio.domain.Reservation;

public class ExpiredState extends AbstractReservationState {

    public ExpiredState() {
        super(StateType.EXPIRED);
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
        // Free the reserved resources
        reservation.freeResources();
        reservation.recordStateChange("Reservation EXPIRED - resources freed");
    }
}