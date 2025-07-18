package core.ms.order.domain.ports.inbound;


import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.validators.ValidationErrorMessage;
import core.ms.shared.domain.Symbol;
import core.ms.shared.domain.Money;
import java.math.BigDecimal;
import java.util.List;

/**
 * Service interface for order validation operations.
 * Handles complex validation logic that spans multiple entities.
 */
public interface OrderValidationService {

    /**
     * Validates order creation parameters
     */
    List<ValidationErrorMessage> validateOrderCreation(String userId, Symbol symbol,
                                                       Money price, BigDecimal quantity);

    /**
     * Validates order state transitions
     */
    List<ValidationErrorMessage> validateOrderStateTransition(IOrder order, String newState);

    /**
     * Validates order modification
     */
    List<ValidationErrorMessage> validateOrderModification(IOrder order, Money newPrice);

    /**
     * Validates order cancellation
     */
    List<ValidationErrorMessage> validateOrderCancellation(IOrder order);

    /**
     * Validates transaction creation
     */
    List<ValidationErrorMessage> validateTransactionCreation(IOrder buyOrder, IOrder sellOrder,
                                                             Money price, BigDecimal quantity);
}
