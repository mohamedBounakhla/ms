package core.ms.order.domain.factories;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order.domain.entities.Transaction;
import core.ms.order.domain.validators.TransactionBuilderValidation;
import core.ms.order.domain.validators.ValidationErrorMessage;
import core.ms.utils.idgenerator.IdGen;

import java.math.BigDecimal;
import java.util.List;

/**
 * Domain factory for creating validated transactions.
 * Encapsulates all business rules and validation logic within the domain.
 */
public class TransactionFactory {

    private TransactionFactory() {
        // Static factory - no instantiation
    }

    /**
     * Creates a validated transaction from matching orders.
     * ID generation is implicit - client doesn't need to worry about it.
     *
     * @param buyOrder The buy order
     * @param sellOrder The sell order
     * @param quantity The transaction quantity
     * @return A valid transaction
     * @throws TransactionCreationException if validation fails
     */
    public static Transaction create(IBuyOrder buyOrder, ISellOrder sellOrder, BigDecimal quantity) {

        try {
            // Factory responsibility: Generate ID before validation
            String transactionId = IdGen.generate("transaction");

            // All validation happens in the builder - factory just orchestrates
            TransactionBuilderValidation.TransactionValidationResult validation =
                    TransactionBuilderValidation
                            .builder()
                            .withId(transactionId)              // ← Factory-generated ID
                            .withSymbol(buyOrder.getSymbol())   // Derive symbol from buy order
                            .withBuyOrder(buyOrder)             // Progressive validation
                            .withSellOrder(sellOrder)           // Progressive validation
                            .withQuantity(quantity)             // Progressive validation
                            .build();                           // Final state

            // Pure entity creation - NO validation needed here
            Transaction transaction = new Transaction(
                    validation.getId(),
                    validation.getSymbol(),
                    validation.getBuyOrder(),
                    validation.getSellOrder(),
                    validation.getQuantity()
            );

            // Domain logic: Update order execution quantities
            updateOrderExecution(validation.getBuyOrder(), validation.getQuantity());
            updateOrderExecution(validation.getSellOrder(), validation.getQuantity());

            return transaction;

        } catch (TransactionBuilderValidation.ValidationTransactionException e) {
            throw new TransactionCreationException("Transaction creation failed: " + e.getMessage(), e);
        } catch (ClassCastException e) {
            throw new TransactionCreationException("Invalid order types: orders must be IBuyOrder and ISellOrder", e);
        }
    }

    /**
     * Updates order execution quantity as part of transaction creation.
     * This is domain logic that belongs in the factory.
     */
    private static void updateOrderExecution(IOrder order, BigDecimal executedQuantity) {
        try {
            order.updateExecution(executedQuantity);
        } catch (Exception e) {
            throw new TransactionCreationException(
                    "Failed to update order execution: " + e.getMessage()
            );
        }
    }

    // ===== DOMAIN EXCEPTION =====

    /**
     * Domain exception for transaction creation failures.
     * Encapsulates validation errors within the domain.
     */
    public static class TransactionCreationException extends RuntimeException {
        private final List<ValidationErrorMessage> validationErrors;

        public TransactionCreationException(String message) {
            super(message);
            this.validationErrors = List.of();
        }

        public TransactionCreationException(String message, List<ValidationErrorMessage> validationErrors) {
            super(message);
            this.validationErrors = validationErrors;
        }

        public TransactionCreationException(String message, Throwable cause) {
            super(message, cause);
            this.validationErrors = List.of();
        }

        public List<ValidationErrorMessage> getValidationErrors() {
            return validationErrors;
        }

        public List<String> getErrors() {
            return validationErrors.stream()
                    .map(ValidationErrorMessage::getMessage)
                    .toList();
        }

        public boolean hasValidationErrors() {
            return !validationErrors.isEmpty();
        }

        @Override
        public String getMessage() {
            if (validationErrors.isEmpty()) {
                return super.getMessage();
            }

            StringBuilder sb = new StringBuilder(super.getMessage());
            sb.append(". Validation errors: ");
            validationErrors.forEach(error -> sb.append(error.getMessage()).append("; "));
            return sb.toString();
        }
    }
}