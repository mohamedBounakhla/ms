package core.ms.portfolio.domain.value_object.state;

import core.ms.portfolio.domain.entities.Reservation;

public class ExecutedState extends AbstractReservationState {

    public ExecutedState() {
        super(StateType.EXECUTED);
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
        reservation.recordStateChange("Reservation EXECUTED - order matched, resources consumed");
    }
}