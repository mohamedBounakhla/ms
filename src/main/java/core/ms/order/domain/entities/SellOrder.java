package core.ms.order.domain.entities;

import core.ms.order.domain.value_objects.OrderStatus;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pure sell order entity - NO validation.
 * Creation controlled by OrderFactory using validation builders.
 */
public class SellOrder extends AbstractOrder implements ISellOrder {

    public SellOrder(String id, Symbol symbol, Money price, BigDecimal quantity) {
        super(id, symbol, price, quantity);
    }

    // Constructor for builder pattern with all fields
    public SellOrder(String id, Symbol symbol, Money price, BigDecimal quantity,
                     OrderStatus status, LocalDateTime createdAt, LocalDateTime updatedAt,
                     BigDecimal executedQuantity) {
        super(id, symbol, price, quantity, status, createdAt, updatedAt, executedQuantity);
    }

    @Override
    public Money getProceeds() {
        return getPrice().multiply(getExecutedQuantity());
    }
}