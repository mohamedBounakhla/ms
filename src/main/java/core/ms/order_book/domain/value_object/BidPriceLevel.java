package core.ms.order_book.domain.value_object;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.shared.money.Money;

public class BidPriceLevel extends AbstractPriceLevel<IBuyOrder> {

    public BidPriceLevel(Money price) {
        super(price);
    }

}