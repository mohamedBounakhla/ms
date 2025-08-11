package core.ms.portfolio.domain;

import core.ms.order.domain.entities.IOrder;
import core.ms.shared.money.Money;

import java.time.Instant;

public abstract class Reservation<T extends IOrder> {
    protected String reservationId;
    protected T order;
    protected Instant createdAt;
    protected ReservationStatus status;

    public Reservation(String reservationId, T order) {
        this.reservationId = reservationId;
        this.order = order;
        this.createdAt = Instant.now();
        this.status = ReservationStatus.PENDING;
    }

    public abstract Money getReservedAmount();

    public boolean canRelease() {
        return status == ReservationStatus.PENDING;
    }

    public void release() {
        if (canRelease()) {
            status = ReservationStatus.RELEASED;
        }
    }

    public void confirm() {
        if (status == ReservationStatus.PENDING) {
            status = ReservationStatus.CONFIRMED;
        }
    }
}