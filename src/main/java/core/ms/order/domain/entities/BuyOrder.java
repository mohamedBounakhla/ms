package core.ms.order.domain.entities;
import core.ms.order.domain.value_objects.OrderStatus;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pure buy order entity - NO validation.
 * Creation controlled by OrderFactory using validation builders.
 */
public class BuyOrder extends AbstractOrder implements IBuyOrder {

    public BuyOrder(String id, Symbol symbol, Money price, BigDecimal quantity) {
        super(id, symbol, price, quantity);
    }

    // Constructor for builder pattern with all fields
    public BuyOrder(String id, Symbol symbol, Money price, BigDecimal quantity,
                    OrderStatus status, LocalDateTime createdAt, LocalDateTime updatedAt,
                    BigDecimal executedQuantity) {
        super(id, symbol, price, quantity, status, createdAt, updatedAt, executedQuantity);
    }

    @Override
    public Money getCostBasis() {
        return getPrice().multiply(getExecutedQuantity());
    }
}

