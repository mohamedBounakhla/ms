package core.ms.portfolio.domain.entities;

import core.ms.order.domain.entities.ISellOrder;
import core.ms.portfolio.domain.aggregates.Portfolio;
import core.ms.portfolio.domain.value_object.ResourceConsumption;
import core.ms.shared.money.Symbol;
import core.ms.shared.money.Money;

import java.math.BigDecimal;

public class AssetReservation extends Reservation<ISellOrder> {
    private final Symbol symbol;
    private final BigDecimal quantity;

    public AssetReservation(String reservationId, ISellOrder sellOrder,
                            Symbol symbol, BigDecimal quantity) {
        super(reservationId, sellOrder);
        this.symbol = symbol;
        this.quantity = quantity;
    }

    @Override
    public ResourceConsumption getResourceConsumption() {
        return ResourceConsumption.forAsset(symbol, quantity);
    }

    @Override
    public BigDecimal getReservedQuantity() {
        return quantity;
    }

    @Override
    public Money getReservedAmount() {
        // For asset reservations, we don't have a monetary amount
        // This would need market price to calculate
        return Money.zero(symbol.getQuoteCurrency());
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }
}