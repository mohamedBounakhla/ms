package core.ms.order.domain.validators;

import core.ms.order.domain.value_objects.OrderStatus;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import core.ms.utils.idgenerator.IdGen;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Progressive order builder with incremental validation.
 * Each parameter is validated individually, then relationally with prior parameters.
 * No redundant checks, no global validation.
 */
public class OrderBuilderValidation {

    private String id;
    private String portfolioId;
    private String reservationId;
    private Symbol symbol;
    private Money price;
    private BigDecimal quantity;

    private OrderBuilderValidation() {}

    public static OrderBuilder builder() {
        return new OrderBuilder();
    }

    public static class OrderBuilder {
        private final OrderBuilderValidation order = new OrderBuilderValidation();

        // ===== STEP 1: ID =====
        public OrderBuilder withId(String id) {
            // 1. Individual validation
            if (id == null || id.trim().isEmpty()) {
                throw new ValidationOrderException("Order ID cannot be null or empty");
            }

            // 2. No relational validation needed - first parameter

            // 3. Assignment after validation
            order.id = id;
            return this;
        }

        // ===== STEP 2: PORTFOLIO ID =====
        public OrderBuilder withPortfolioId(String portfolioId) {
            // MANDATORY parameter
            if (portfolioId == null || portfolioId.trim().isEmpty()) {
                throw new ValidationOrderException("Portfolio ID cannot be null or empty");
            }
            order.portfolioId = portfolioId;
            return this;
        }

        // ===== STEP 3: RESERVATION ID =====
        public OrderBuilder withReservationId(String reservationId) {
            // MANDATORY parameter
            if (reservationId == null || reservationId.trim().isEmpty()) {
                throw new ValidationOrderException("Reservation ID cannot be null or empty");
            }
            order.reservationId = reservationId;
            return this;
        }

        // ===== STEP 4: SYMBOL =====
        public OrderBuilder withSymbol(Symbol symbol) {
            // 1. Individual validation
            if (symbol == null) {
                throw new ValidationOrderException("Symbol cannot be null");
            }

            // 2. No relational validation with ID/portfolioId/reservationId

            // 3. Assignment after validation
            order.symbol = symbol;
            return this;
        }

        // ===== STEP 5: PRICE =====
        public OrderBuilder withPrice(Money price) {
            // 1. Individual validation
            if (price == null) {
                throw new ValidationOrderException("Price cannot be null");
            }

            if (price.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationOrderException("Price must be positive");
            }

            // 2. Relational validation with prior parameters
            if (order.symbol != null) {
                // Symbol + Price: Currency compatibility
                if (!price.getCurrency().equals(order.symbol.getQuoteCurrency())) {
                    throw new ValidationOrderException(
                            String.format("Price currency %s does not match symbol quote currency %s",
                                    price.getCurrency(), order.symbol.getQuoteCurrency()));
                }
            }

            // 3. Assignment after validation
            order.price = price;
            return this;
        }

        // ===== STEP 6: QUANTITY =====
        public OrderBuilder withQuantity(BigDecimal quantity) {
            // 1. Individual validation
            if (quantity == null) {
                throw new ValidationOrderException("Quantity cannot be null");
            }

            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationOrderException("Quantity must be positive");
            }

            // 2. No relational validation needed for quantity

            // 3. Assignment after validation
            order.quantity = quantity;
            return this;
        }

        // ===== BUILD =====
        public OrderValidationResult build() {
            return new OrderValidationResult(order);
        }
    }

    // ===== CONVENIENCE BUILDER WITH AUTO-ID =====
    public static OrderBuilder builderWithGeneratedId() {
        return new OrderBuilder().withId(generateOrderId());
    }

    // ===== VALIDATION RESULT =====
    public static class OrderValidationResult {
        private final OrderBuilderValidation validatedData;

        private OrderValidationResult(OrderBuilderValidation validatedData) {
            this.validatedData = validatedData;
        }

        // Getters for validated data
        public String getId() { return validatedData.id; }
        public String getPortfolioId() { return validatedData.portfolioId; }
        public String getReservationId() { return validatedData.reservationId; }
        public Symbol getSymbol() { return validatedData.symbol; }
        public Money getPrice() { return validatedData.price; }
        public BigDecimal getQuantity() { return validatedData.quantity; }
    }

    // ===== DOMAIN EXCEPTION =====
    public static class ValidationOrderException extends RuntimeException {
        public ValidationOrderException(String message) {
            super(message);
        }
    }

    // ===== HELPER =====
    private static String generateOrderId() {
        return IdGen.generate("order");
    }
}