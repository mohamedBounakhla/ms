package core.ms.order.domain;

import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;

import java.math.BigDecimal;

public class SellOrder extends AbstractOrder implements ISellOrder {

    public SellOrder(String id, Symbol symbol, Money price, BigDecimal quantity) {
        super(id, symbol, price, quantity);
    }

    @Override
    public Money getProceeds() {
        return getTotalValue();
    }
}
