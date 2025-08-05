package core.ms.order.domain.validators;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order.domain.validators.annotation.OrderNotFinal;
import core.ms.shared.money.Symbol;
import core.ms.utils.IdGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Progressive transaction builder with incremental validation.
 * Each parameter is validated individually, then relationally with prior parameters.
 * No redundant checks, no global validation.
 */
public class TransactionBuilderValidation {

    private String id;
    private Symbol symbol;

    @OrderNotFinal(message = "Buy order must be active (not FILLED or CANCELLED)")
    private IBuyOrder buyOrder;

    @OrderNotFinal(message = "Sell order must be active (not FILLED or CANCELLED)")
    private ISellOrder sellOrder;

    private BigDecimal quantity;
    private LocalDateTime createdAt;

    // Validator instance for annotation checking
    private static final ValidateOrderState validator = new ValidateOrderState();

    private TransactionBuilderValidation() {}

    public static TransactionBuilder builder() {
        return new TransactionBuilder();
    }

    public static class TransactionBuilder {
        private final TransactionBuilderValidation transaction = new TransactionBuilderValidation();

        public TransactionBuilder withId(String id) {

            if (id == null || id.trim().isEmpty()) {
                throw new ValidationTransactionException("Transaction ID cannot be null or empty");
            }

            transaction.id = id;
            return this;
        }


        public TransactionBuilder withSymbol(Symbol symbol) {

            if (symbol == null) {
                throw new ValidationTransactionException("Symbol cannot be null");
            }

            transaction.symbol = symbol;
            return this;
        }

        // ===== STEP 3: BUY ORDER =====
        public TransactionBuilder withBuyOrder(IBuyOrder buyOrder) {

            if (buyOrder == null) {
                throw new ValidationTransactionException("Buy order cannot be null");
            }

            // Validate using annotations (individual order state) - need temp assignment for annotation validation
            TransactionBuilderValidation tempTransaction = new TransactionBuilderValidation();
            tempTransaction.buyOrder = buyOrder;
            validator.validateAndThrow(tempTransaction);

            if (transaction.symbol != null) {

                if (!buyOrder.getSymbol().equals(transaction.symbol)) {
                    throw new ValidationTransactionException(
                            "Buy order symbol must match transaction symbol");
                }
            }
            transaction.buyOrder = buyOrder;
            return this;
        }

        public TransactionBuilder withSellOrder(ISellOrder sellOrder) {
            // 1. Validate SellOrder individually
            if (sellOrder == null) {
                throw new ValidationTransactionException("Sell order cannot be null");
            }


            TransactionBuilderValidation tempTransaction = new TransactionBuilderValidation();
            tempTransaction.sellOrder = sellOrder;
            validator.validateAndThrow(tempTransaction);


            if (transaction.symbol != null) {
                // Symbol + SellOrder: Symbol compatibility
                if (!sellOrder.getSymbol().equals(transaction.symbol)) {
                    throw new ValidationTransactionException(
                            "Sell order symbol must match transaction symbol");
                }
            }

            if (transaction.buyOrder != null) {
                // BuyOrder + SellOrder: Symbol compatibility
                if (!transaction.buyOrder.getSymbol().equals(sellOrder.getSymbol())) {
                    throw new ValidationTransactionException(
                            "Buy and sell orders must have the same symbol");
                }

                // BuyOrder + SellOrder: Order matching (can they match?)
                if (transaction.buyOrder.getPrice().isLessThan(sellOrder.getPrice())) {
                    throw new ValidationTransactionException(
                            "Buy order price must be greater than or equal to sell order price");
                }
            }

            transaction.sellOrder = sellOrder;
            return this;
        }



        public TransactionBuilder withQuantity(BigDecimal quantity) {

            if (quantity == null) {
                throw new ValidationTransactionException("Quantity cannot be null");
            }

            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationTransactionException("Quantity must be positive");
            }

            // 2. Relational validation with prior parameters
            if (transaction.buyOrder != null) {
                // BuyOrder + Quantity: Remaining quantity constraint
                if (quantity.compareTo(transaction.buyOrder.getRemainingQuantity()) > 0) {
                    throw new ValidationTransactionException(
                            "Transaction quantity cannot exceed buy order remaining quantity");
                }
            }

            if (transaction.sellOrder != null) {
                // SellOrder + Quantity: Remaining quantity constraint
                if (quantity.compareTo(transaction.sellOrder.getRemainingQuantity()) > 0) {
                    throw new ValidationTransactionException(
                            "Transaction quantity cannot exceed sell order remaining quantity");
                }
            }


            transaction.quantity = quantity;
            return this;
        }


        public TransactionValidationResult build() {
            // Set final derived fields
            transaction.createdAt = LocalDateTime.now();

            // If we reach here, all validation passed progressively
            // No additional validation needed!

            return new TransactionValidationResult(transaction);
        }
    }

    public static TransactionBuilder builderWithGeneratedId() {
        return new TransactionBuilder().withId(generateTransactionId());
    }

    // ===== VALIDATION RESULT =====
    public static class TransactionValidationResult {
        private final TransactionBuilderValidation validatedData;

        private TransactionValidationResult(TransactionBuilderValidation validatedData) {
            this.validatedData = validatedData;
        }

        // Getters for validated data
        public String getId() { return validatedData.id; }
        public Symbol getSymbol() { return validatedData.symbol; }
        public IBuyOrder getBuyOrder() { return validatedData.buyOrder; }
        public ISellOrder getSellOrder() { return validatedData.sellOrder; }
        public BigDecimal getQuantity() { return validatedData.quantity; }
        public LocalDateTime getCreatedAt() { return validatedData.createdAt; }
    }

    // ===== DOMAIN EXCEPTION =====
    public static class ValidationTransactionException extends RuntimeException {
        public ValidationTransactionException(String message) {
            super(message);
        }
    }

    // ===== HELPER =====
    private static String generateTransactionId() {
        return IdGenerator.generateTransactionId();
    }
}