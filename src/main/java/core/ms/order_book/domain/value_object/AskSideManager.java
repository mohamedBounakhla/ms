package core.ms.order_book.domain.value_object;

import core.ms.order.domain.entities.ISellOrder;
import core.ms.shared.domain.Money;

public class AskSideManager extends AbstractPriceLevelManager<ISellOrder, AskPriceLevel>{
    public AskSideManager() {
        super(new SellOrderPriorityCalculator());
    }

    @Override
    protected AskPriceLevel createPriceLevel(Money price) {
        return new AskPriceLevel(price);
    }
}
