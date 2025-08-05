package core.ms.order_book.domain.value_object;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.shared.money.Money;

public class BidSideManager extends AbstractPriceLevelManager<IBuyOrder,BidPriceLevel>{
    public BidSideManager() {
        super(new BuyOrderPriorityCalculator());
    }

    protected BidPriceLevel createPriceLevel(Money price) {
        return new BidPriceLevel(price);
    }
}
