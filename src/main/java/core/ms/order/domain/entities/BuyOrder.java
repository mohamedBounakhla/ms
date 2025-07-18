package core.ms.order.domain.entities;

import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;

import java.math.BigDecimal;

public class BuyOrder extends AbstractOrder implements IBuyOrder {

    public BuyOrder(String id, Symbol symbol, Money price, BigDecimal quantity) {
        super(id, symbol, price, quantity);
    }

    @Override
    public Money getCostBasis() {
        return getPrice().multiply(getExecutedQuantity());
    }
}