package core.ms.portfolio.domain.positions;

import core.ms.order.domain.entities.ISellOrder;

import core.ms.portfolio.domain.reservations.Reservation;
import core.ms.shared.money.Symbol;


import java.math.BigDecimal;

public class AssetReservation extends Reservation<ISellOrder> {

    public AssetReservation(String reservationId, ISellOrder sellOrder) {
        super(reservationId, sellOrder);
    }

    @Override
    public BigDecimal getReservedAmount() {
        return order.getQuantity();
    }


    public Symbol getSymbol() {
        return order.getSymbol();
    }
}