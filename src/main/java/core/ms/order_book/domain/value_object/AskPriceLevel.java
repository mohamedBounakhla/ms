package core.ms.order_book.domain.value_object;

import core.ms.order.domain.entities.ISellOrder;
import core.ms.shared.money.Money;

public class AskPriceLevel extends AbstractPriceLevel<ISellOrder> {

    public AskPriceLevel(Money price) {
        super(price);
    }

}