package core.ms.portfolio.domain.value_object.state;

import core.ms.portfolio.domain.entities.Reservation;

public class ExpiredState extends AbstractReservationState {

    public ExpiredState() {
        super(StateType.EXPIRED);
    }

    @Override
    public boolean canExecute() {
        return false;
    }

    @Override
    public boolean canCancel() {
        return false;
    }

    @Override
    public boolean canExpire() {
        return false;
    }

    @Override
    public void onEntry(Reservation<?> reservation) {
        reservation.recordStateChange("Reservation EXPIRED - timeout reached, resources freed");
    }
}