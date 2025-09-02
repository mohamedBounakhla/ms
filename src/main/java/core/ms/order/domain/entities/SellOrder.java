package core.ms.order.domain.entities;

import core.ms.order.domain.value_objects.OrderStatus;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public class SellOrder extends AbstractOrder implements ISellOrder {

    public SellOrder(String id, String portfolioId, String reservationId,
                     Symbol symbol, Money price, BigDecimal quantity) {
        super(id, portfolioId, reservationId, symbol, price, quantity);
    }

    @Override
    public Money getProceeds() {
        return getPrice().multiply(getExecutedQuantity());
    }
}