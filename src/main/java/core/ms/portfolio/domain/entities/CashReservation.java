package core.ms.portfolio.domain.entities;
import core.ms.order.domain.entities.IBuyOrder;
import core.ms.portfolio.domain.aggregates.Portfolio;
import core.ms.portfolio.domain.value_object.ResourceConsumption;
import core.ms.shared.money.Money;
import core.ms.shared.money.Currency;

import java.math.BigDecimal;

public class CashReservation extends Reservation<IBuyOrder> {
    private final Money amount;

    public CashReservation(String reservationId, IBuyOrder buyOrder, Money amount) {
        super(reservationId, buyOrder);
        this.amount = amount;
    }

    @Override
    public ResourceConsumption getResourceConsumption() {
        return ResourceConsumption.forCash(amount);
    }

    @Override
    public BigDecimal getReservedQuantity() {
        // For cash reservations, return the amount as BigDecimal
        return amount.getAmount();
    }

    @Override
    public Money getReservedAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return amount.getCurrency();
    }
}