package core.ms.order_book.domain.value_object;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.IOrder;
import core.ms.shared.domain.Money;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BidPriceLevel extends AbstractPriceLevel<IBuyOrder> {

    public BidPriceLevel(Money price) {
        super(price);
    }

}