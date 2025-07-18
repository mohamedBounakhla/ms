package core.ms.order.application.services;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order.domain.ports.inbound.OrderValidationService;
import core.ms.order.domain.validators.ValidationErrorMessage;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderValidationApplicationService implements OrderValidationService {

    // ===== ORDER CREATION VALIDATION =====

    @Override
    public List<ValidationErrorMessage> validateOrderCreation(String userId, Symbol symbol,
                                                              Money price, BigDecimal quantity) {
        List<ValidationErrorMessage> errors = new ArrayList<>();

        // Basic parameter validation
        if (userId == null || userId.trim().isEmpty()) {
            errors.add(new ValidationErrorMessage("User ID cannot be null or empty"));
        }
        if (symbol == null) {
            errors.add(new ValidationErrorMessage("Symbol cannot be null"));
        }
        if (price == null) {
            errors.add(new ValidationErrorMessage("Price cannot be null"));
        }
        if (quantity == null) {
            errors.add(new ValidationErrorMessage("Quantity cannot be null"));
        }

        // Early return if basic validation fails
        if (!errors.isEmpty()) {
            return errors;
        }

        // Price validation
        if (price.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(new ValidationErrorMessage("Price must be positive"));
        }

        // Quantity validation
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(new ValidationErrorMessage("Quantity must be positive"));
        }

        // Symbol-price currency validation
        if (!price.getCurrency().equals(symbol.getQuoteCurrency())) {
            errors.add(new ValidationErrorMessage(
                    String.format("Price currency %s does not match symbol quote currency %s",
                            price.getCurrency(), symbol.getQuoteCurrency())));
        }

        return errors;
    }

    @Override
    public List<ValidationErrorMessage> validateOrderStateTransition(IOrder order, String newState) {
        List<ValidationErrorMessage> errors = new ArrayList<>();

        if (order == null) {
            errors.add(new ValidationErrorMessage("Order cannot be null"));
            return errors;
        }

        if (newState == null || newState.trim().isEmpty()) {
            errors.add(new ValidationErrorMessage("New state cannot be null or empty"));
            return errors;
        }

        OrderStatusEnum currentStatus = order.getStatus().getStatus();
        OrderStatusEnum targetStatus;

        try {
            targetStatus = OrderStatusEnum.valueOf(newState.toUpperCase());
        } catch (IllegalArgumentException e) {
            errors.add(new ValidationErrorMessage("Invalid order state: " + newState));
            return errors;
        }

        // Validate state transitions
        switch (currentStatus) {
            case PENDING -> {
                if (targetStatus != OrderStatusEnum.PARTIAL &&
                        targetStatus != OrderStatusEnum.FILLED &&
                        targetStatus != OrderStatusEnum.CANCELLED) {
                    errors.add(new ValidationErrorMessage(
                            String.format("Cannot transition from %s to %s", currentStatus, targetStatus)));
                }
            }
            case PARTIAL -> {
                if (targetStatus != OrderStatusEnum.FILLED &&
                        targetStatus != OrderStatusEnum.CANCELLED) {
                    errors.add(new ValidationErrorMessage(
                            String.format("Cannot transition from %s to %s", currentStatus, targetStatus)));
                }
            }
            case FILLED, CANCELLED -> {
                errors.add(new ValidationErrorMessage(
                        String.format("Cannot transition from terminal state %s", currentStatus)));
            }
        }

        return errors;
    }

    @Override
    public List<ValidationErrorMessage> validateOrderModification(IOrder order, Money newPrice) {
        List<ValidationErrorMessage> errors = new ArrayList<>();

        if (order == null) {
            errors.add(new ValidationErrorMessage("Order cannot be null"));
            return errors;
        }

        if (newPrice == null) {
            errors.add(new ValidationErrorMessage("New price cannot be null"));
            return errors;
        }

        // Check if order can be modified
        if (order.getStatus().isTerminal()) {
            errors.add(new ValidationErrorMessage("Cannot modify order in terminal state"));
        }

        // Price validation
        if (newPrice.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(new ValidationErrorMessage("New price must be positive"));
        }

        // Currency validation
        if (!newPrice.getCurrency().equals(order.getPrice().getCurrency())) {
            errors.add(new ValidationErrorMessage(
                    String.format("New price currency %s does not match order currency %s",
                            newPrice.getCurrency(), order.getPrice().getCurrency())));
        }

        return errors;
    }

    @Override
    public List<ValidationErrorMessage> validateOrderCancellation(IOrder order) {
        List<ValidationErrorMessage> errors = new ArrayList<>();

        if (order == null) {
            errors.add(new ValidationErrorMessage("Order cannot be null"));
            return errors;
        }

        // Check if order can be cancelled
        if (order.getStatus().isTerminal()) {
            errors.add(new ValidationErrorMessage("Cannot cancel order in terminal state"));
        }

        return errors;
    }

    @Override
    public List<ValidationErrorMessage> validateTransactionCreation(IOrder buyOrder, IOrder sellOrder,
                                                                    Money price, BigDecimal quantity) {
        List<ValidationErrorMessage> errors = new ArrayList<>();

        // Basic parameter validation
        if (buyOrder == null) {
            errors.add(new ValidationErrorMessage("Buy order cannot be null"));
        }
        if (sellOrder == null) {
            errors.add(new ValidationErrorMessage("Sell order cannot be null"));
        }
        if (price == null) {
            errors.add(new ValidationErrorMessage("Transaction price cannot be null"));
        }
        if (quantity == null) {
            errors.add(new ValidationErrorMessage("Transaction quantity cannot be null"));
        }

        // Early return if basic validation fails
        if (!errors.isEmpty()) {
            return errors;
        }

        // Validate order types
        if (!(buyOrder instanceof IBuyOrder)) {
            errors.add(new ValidationErrorMessage("First order must be a buy order"));
        }
        if (!(sellOrder instanceof ISellOrder)) {
            errors.add(new ValidationErrorMessage("Second order must be a sell order"));
        }

        // Symbol compatibility validation
        if (!buyOrder.getSymbol().equals(sellOrder.getSymbol())) {
            errors.add(new ValidationErrorMessage("Buy and sell orders must have the same symbol"));
        }

        // Order status validation
        if (buyOrder.getStatus().isTerminal()) {
            errors.add(new ValidationErrorMessage("Buy order is in terminal state and cannot be used for transaction"));
        }
        if (sellOrder.getStatus().isTerminal()) {
            errors.add(new ValidationErrorMessage("Sell order is in terminal state and cannot be used for transaction"));
        }

        // Price validation
        if (price.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(new ValidationErrorMessage("Transaction price must be positive"));
        }

        // Quantity validation
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(new ValidationErrorMessage("Transaction quantity must be positive"));
        }

        // Currency validation
        if (!price.getCurrency().equals(buyOrder.getPrice().getCurrency()) ||
                !price.getCurrency().equals(sellOrder.getPrice().getCurrency())) {
            errors.add(new ValidationErrorMessage("Transaction price currency must match order currencies"));
        }

        // Order matching validation
        IBuyOrder buyOrderTyped = (IBuyOrder) buyOrder;
        ISellOrder sellOrderTyped = (ISellOrder) sellOrder;

        if (buyOrderTyped.getPrice().isLessThan(sellOrderTyped.getPrice())) {
            errors.add(new ValidationErrorMessage("Buy order price must be greater than or equal to sell order price"));
        }

        // Execution price range validation
        if (price.isLessThan(sellOrderTyped.getPrice()) || price.isGreaterThan(buyOrderTyped.getPrice())) {
            errors.add(new ValidationErrorMessage("Execution price must be between sell price and buy price"));
        }

        // Quantity constraints validation
        if (quantity.compareTo(buyOrderTyped.getRemainingQuantity()) > 0) {
            errors.add(new ValidationErrorMessage("Transaction quantity cannot exceed buy order remaining quantity"));
        }
        if (quantity.compareTo(sellOrderTyped.getRemainingQuantity()) > 0) {
            errors.add(new ValidationErrorMessage("Transaction quantity cannot exceed sell order remaining quantity"));
        }

        return errors;
    }
}