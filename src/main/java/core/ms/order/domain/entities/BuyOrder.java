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

    public BuyOrder(String id, String portfolioId, String reservationId,
                    Symbol symbol, Money price, BigDecimal quantity) {
        super(id, portfolioId, reservationId, symbol, price, quantity);
    }

    @Override
    public Money getCostBasis() {
        return getPrice().multiply(getExecutedQuantity());
    }
}