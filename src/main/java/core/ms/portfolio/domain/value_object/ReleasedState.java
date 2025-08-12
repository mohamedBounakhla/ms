package core.ms.portfolio.domain.value_object;

import core.ms.portfolio.domain.Reservation;

public class ReleasedState extends AbstractReservationState {

    public ReleasedState() {
        super(StateType.RELEASED);
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
        reservation.recordStateChange("Reservation RELEASED - resources freed");
    }
}