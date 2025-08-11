package core.ms.portfolio.domain;
import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.IOrder;
import core.ms.shared.money.Money;
import core.ms.shared.money.Currency;

public class CashReservation extends Reservation<IBuyOrder> {
    private Money amount;

    public CashReservation(String reservationId, IBuyOrder buyOrder, Money amount) {
        super(reservationId, buyOrder);
        this.amount = amount;
    }

    @Override
    public Money getReservedAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return amount.getCurrency();
    }
}