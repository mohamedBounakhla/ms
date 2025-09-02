package core.ms.order.domain.entities;

import core.ms.shared.money.Symbol;

import java.math.BigDecimal;


public class Transaction extends AbstractTransaction {

    public Transaction(
            String id,
            Symbol symbol,
            IBuyOrder buyOrder,
            ISellOrder sellOrder,
            BigDecimal quantity
    ) {
        // Pure delegation - NO validation
        super(id, symbol, buyOrder, sellOrder, quantity);
    }
}