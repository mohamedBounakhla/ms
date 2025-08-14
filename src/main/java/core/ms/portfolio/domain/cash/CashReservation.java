package core.ms.portfolio.domain.cash;
import core.ms.order.domain.entities.IBuyOrder;
import core.ms.portfolio.domain.reservations.Reservation;
import core.ms.shared.money.Currency;

import java.math.BigDecimal;

public class CashReservation extends Reservation<IBuyOrder> {

    public CashReservation(String reservationId, IBuyOrder buyOrder) {
        super(reservationId, buyOrder);
    }

    @Override
    public BigDecimal getReservedAmount() {
        return order.getTotalValue().getAmount();
    }

    public Currency getCurrency() {
        return order.getPrice().getCurrency();
    }
}