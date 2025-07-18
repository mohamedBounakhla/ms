package core.ms.order_book.domain.value_object;

import core.ms.order.domain.entities.ISellOrder;
import core.ms.shared.domain.Money;

import java.util.Objects;

public class SellOrderPriorityCalculator extends AbstractPriorityCalculator<ISellOrder> {

    @Override
    public boolean isPriceBetter(Money price1, Money price2) {
        Objects.requireNonNull(price1, "First price cannot be null");
        Objects.requireNonNull(price2, "Second price cannot be null");

        return price1.isLessThan(price2);
    }


}