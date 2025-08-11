package core.ms.portfolio.domain;

import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.shared.money.Symbol;
import core.ms.shared.money.Money;

import java.math.BigDecimal;

public class AssetReservation extends Reservation<ISellOrder> {
    private Symbol symbol;
    private BigDecimal quantity;

    public AssetReservation(String reservationId, ISellOrder sellOrder, Symbol symbol, BigDecimal quantity) {
        super(reservationId, sellOrder);
        this.symbol = symbol;
        this.quantity = quantity;
    }

    @Override
    public Money getReservedAmount() {
        // Needs market price to calculate
        return Money.zero(symbol.getQuoteCurrency());
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }
}