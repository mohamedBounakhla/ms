package core.ms.order.domain.entities;

import core.ms.shared.money.Symbol;

import java.math.BigDecimal;

/**
 * Pure transaction entity - NO business rules, NO validation.
 * Creation is controlled by TransactionFactory using validation builders.
 */
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