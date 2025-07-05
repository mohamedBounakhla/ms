package core.ms.order.domain;

import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;

import java.math.BigDecimal;

public class Transaction extends AbstractTransaction {

    public Transaction(String id, Symbol symbol, IBuyOrder buyOrder,
                       ISellOrder sellOrder, Money price, BigDecimal quantity) {
        super(id, symbol, buyOrder, sellOrder, price, quantity);
    }

    public static Transaction fromMatchingOrders(String id, IBuyOrder buyOrder,
                                                 ISellOrder sellOrder, Money executionPrice,
                                                 BigDecimal executionQuantity) {
        if (!buyOrder.getSymbol().equals(sellOrder.getSymbol())) {
            throw new IllegalArgumentException("Orders must have the same symbol");
        }

        return new Transaction(id, buyOrder.getSymbol(), buyOrder,
                sellOrder, executionPrice, executionQuantity);
    }
}