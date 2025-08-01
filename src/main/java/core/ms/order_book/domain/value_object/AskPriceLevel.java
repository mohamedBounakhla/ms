package core.ms.order_book.domain.value_object;

import core.ms.order.domain.entities.ISellOrder;
import core.ms.shared.domain.Money;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AskPriceLevel extends AbstractPriceLevel<ISellOrder> {

    public AskPriceLevel(Money price) {
        super(price);
    }

}