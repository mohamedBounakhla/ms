package core.ms.order;

import core.ms.order.application.services.OrderValidationApplicationService;
import core.ms.order.domain.entities.BuyOrder;
import core.ms.order.domain.entities.SellOrder;
import core.ms.order.domain.validators.ValidationErrorMessage;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;



@DisplayName("Order Validation Service Unit Tests")
class OrderValidationServiceUnitTest {

    private OrderValidationApplicationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new OrderValidationApplicationService();
    }

    @Nested
    @DisplayName("Order Creation Validation Tests")
    class OrderCreationValidationTests {

        @Test
        @DisplayName("Should validate successful order creation")
        void shouldValidateSuccessfulOrderCreation() {
            // Given
            String userId = "user123";
            Symbol symbol = Symbol.btcUsd();
            Money price = Money.of("45000.00", Currency.USD);
            BigDecimal quantity = new BigDecimal("0.1");

            // When
            List<ValidationErrorMessage> errors = validationService.validateOrderCreation(userId, symbol, price, quantity);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Should reject order creation with null user ID")
        void shouldRejectOrderCreationWithNullUserId() {
            // Given
            String userId = null;
            Symbol symbol = Symbol.btcUsd();
            Money price = Money.of("45000.00", Currency.USD);
            BigDecimal quantity = new BigDecimal("0.1");

            // When
            List<ValidationErrorMessage> errors = validationService.validateOrderCreation(userId, symbol, price, quantity);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getMessage()).contains("User ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should reject order creation with empty user ID")
        void shouldRejectOrderCreationWithEmptyUserId() {
            // Given
            String userId = "   ";
            Symbol symbol = Symbol.btcUsd();
            Money price = Money.of("45000.00", Currency.USD);
            BigDecimal quantity = new BigDecimal("0.1");

            // When
            List<ValidationErrorMessage> errors = validationService.validateOrderCreation(userId, symbol, price, quantity);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getMessage()).contains("User ID cannot be null or empty");
        }

        @Test
        @DisplayName("Should reject order creation with null symbol")
        void shouldRejectOrderCreationWithNullSymbol() {
            // Given
            String userId = "user123";
            Symbol symbol = null;
            Money price = Money.of("45000.00", Currency.USD);
            BigDecimal quantity = new BigDecimal("0.1");

            // When
            List<ValidationErrorMessage> errors = validationService.validateOrderCreation(userId, symbol, price, quantity);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getMessage()).contains("Symbol cannot be null");
        }

        @Test
        @DisplayName("Should reject order creation with null price")
        void shouldRejectOrderCreationWithNullPrice() {
            // Given
            String userId = "user123";
            Symbol symbol = Symbol.btcUsd();
            Money price = null;
            BigDecimal quantity = new BigDecimal("0.1");

            // When
            List<ValidationErrorMessage> errors = validationService.validateOrderCreation(userId, symbol, price, quantity);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getMessage()).contains("Price cannot be null");
        }

        @Test
        @DisplayName("Should reject order creation with null quantity")
        void shouldRejectOrderCreationWithNullQuantity() {
            // Given
            String userId = "user123";
            Symbol symbol = Symbol.btcUsd();
            Money price = Money.of("45000.00", Currency.USD);
            BigDecimal quantity = null;

            // When
            List<ValidationErrorMessage> errors = validationService.validateOrderCreation(userId, symbol, price, quantity);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getMessage()).contains("Quantity cannot be null");
        }

        @Test
        @DisplayName("Should reject order creation with negative price")
        void shouldRejectOrderCreationWithNegativePrice() {
            // Given
            String userId = "user123";
            Symbol symbol = Symbol.btcUsd();
            Money price = Money.of("-1000.00", Currency.USD);
            BigDecimal quantity = new BigDecimal("0.1");

            // When
            List<ValidationErrorMessage> errors = validationService.validateOrderCreation(userId, symbol, price, quantity);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getMessage()).contains("Price must be positive");
        }

        @Test
        @DisplayName("Should reject order creation with zero price")
        void shouldRejectOrderCreationWithZeroPrice() {
            // Given
            String userId = "user123";
            Symbol symbol = Symbol.btcUsd();
            Money price = Money.of("0.00", Currency.USD);
            BigDecimal quantity = new BigDecimal("0.1");

            // When
            List<ValidationErrorMessage> errors = validationService.validateOrderCreation(userId, symbol, price, quantity);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getMessage()).contains("Price must be positive");
        }

        @Test
        @DisplayName("Should reject order creation with negative quantity")
        void shouldRejectOrderCreationWithNegativeQuantity() {
            // Given
            String userId = "user123";
            Symbol symbol = Symbol.btcUsd();
            Money price = Money.of("45000.00", Currency.USD);
            BigDecimal quantity = new BigDecimal("-0.1");

            // When
            List<ValidationErrorMessage> errors = validationService.validateOrderCreation(userId, symbol, price, quantity);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getMessage()).contains("Quantity must be positive");
        }

        @Test
        @DisplayName("Should reject order creation with zero quantity")
        void shouldRejectOrderCreationWithZeroQuantity() {
            // Given
            String userId = "user123";
            Symbol symbol = Symbol.btcUsd();
            Money price = Money.of("45000.00", Currency.USD);
            BigDecimal quantity = BigDecimal.ZERO;

            // When
            List<ValidationErrorMessage> errors = validationService.validateOrderCreation(userId, symbol, price, quantity);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getMessage()).contains("Quantity must be positive");
        }

        @Test
        @DisplayName("Should reject order creation with mismatched currency")
        void shouldRejectOrderCreationWithMismatchedCurrency() {
            // Given
            String userId = "user123";
            Symbol symbol = Symbol.btcUsd(); // Expects USD
            Money price = Money.of("40000.00", Currency.EUR); // But EUR provided
            BigDecimal quantity = new BigDecimal("0.1");

            // When
            List<ValidationErrorMessage> errors = validationService.validateOrderCreation(userId, symbol, price, quantity);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getMessage()).contains("Price currency EUR does not match symbol quote currency USD");
        }

        @Test
        @DisplayName("Should accumulate multiple validation errors")
        void shouldAccumulateMultipleValidationErrors() {
            // Given
            String userId = null;
            Symbol symbol = null;
            Money price = null;
            BigDecimal quantity = null;

            // When
            List<ValidationErrorMessage> errors = validationService.validateOrderCreation(userId, symbol, price, quantity);

            // Then
            assertThat(errors).hasSize(4);
            assertThat(errors.stream().map(ValidationErrorMessage::getMessage))
                    .contains(
                            "User ID cannot be null or empty",
                            "Symbol cannot be null",
                            "Price cannot be null",
                            "Quantity cannot be null"
                    );
        }
    }

    @Nested
    @DisplayName("Order Modification Validation Tests")
    class OrderModificationValidationTests {

        @Test
        @DisplayName("Should validate successful order modification")
        void shouldValidateSuccessfulOrderModification() {
            // Given
            BuyOrder order = new BuyOrder(
                    "ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.1")
            );
            Money newPrice = Money.of("46000.00", Currency.USD);

            // When
            List<ValidationErrorMessage> errors = validationService.validateOrderModification(order, newPrice);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Should reject modification of cancelled order")
        void shouldRejectModificationOfCancelledOrder() {
            // Given
            BuyOrder order = new BuyOrder(
                    "ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.1")
            );
            order.cancel();
            Money newPrice = Money.of("46000.00", Currency.USD);

            // When
            List<ValidationErrorMessage> errors = validationService.validateOrderModification(order, newPrice);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getMessage()).contains("Cannot modify order in terminal state");
        }

        @Test
        @DisplayName("Should reject modification with null price")
        void shouldRejectModificationWithNullPrice() {
            // Given
            BuyOrder order = new BuyOrder(
                    "ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.1")
            );
            Money newPrice = null;

            // When
            List<ValidationErrorMessage> errors = validationService.validateOrderModification(order, newPrice);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getMessage()).contains("New price cannot be null");
        }

        @Test
        @DisplayName("Should reject modification with negative price")
        void shouldRejectModificationWithNegativePrice() {
            // Given
            BuyOrder order = new BuyOrder(
                    "ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.1")
            );
            Money newPrice = Money.of("-1000.00", Currency.USD);

            // When
            List<ValidationErrorMessage> errors = validationService.validateOrderModification(order, newPrice);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getMessage()).contains("New price must be positive");
        }

        @Test
        @DisplayName("Should reject modification with different currency")
        void shouldRejectModificationWithDifferentCurrency() {
            // Given
            BuyOrder order = new BuyOrder(
                    "ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.1")
            );
            Money newPrice = Money.of("40000.00", Currency.EUR); // Different currency

            // When
            List<ValidationErrorMessage> errors = validationService.validateOrderModification(order, newPrice);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getMessage()).contains("New price currency EUR does not match order currency USD");
        }
    }

    @Nested
    @DisplayName("Order Cancellation Validation Tests")
    class OrderCancellationValidationTests {

        @Test
        @DisplayName("Should validate successful order cancellation")
        void shouldValidateSuccessfulOrderCancellation() {
            // Given
            BuyOrder order = new BuyOrder(
                    "ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.1")
            );

            // When
            List<ValidationErrorMessage> errors = validationService.validateOrderCancellation(order);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Should reject cancellation of null order")
        void shouldRejectCancellationOfNullOrder() {
            // Given
            BuyOrder order = null;

            // When
            List<ValidationErrorMessage> errors = validationService.validateOrderCancellation(order);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getMessage()).contains("Order cannot be null");
        }

        @Test
        @DisplayName("Should reject cancellation of already cancelled order")
        void shouldRejectCancellationOfAlreadyCancelledOrder() {
            // Given
            BuyOrder order = new BuyOrder(
                    "ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.1")
            );
            order.cancel();

            // When
            List<ValidationErrorMessage> errors = validationService.validateOrderCancellation(order);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getMessage()).contains("Cannot cancel order in terminal state");
        }

        @Test
        @DisplayName("Should reject cancellation of filled order")
        void shouldRejectCancellationOfFilledOrder() {
            // Given
            BuyOrder order = new BuyOrder(
                    "ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.1")
            );
            order.updateExecution(new BigDecimal("0.1")); // Fill completely

            // When
            List<ValidationErrorMessage> errors = validationService.validateOrderCancellation(order);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getMessage()).contains("Cannot cancel order in terminal state");
        }
    }

    @Nested
    @DisplayName("Transaction Creation Validation Tests")
    class TransactionCreationValidationTests {

        @Test
        @DisplayName("Should validate successful transaction creation")
        void shouldValidateSuccessfulTransactionCreation() {
            // Given
            BuyOrder buyOrder = new BuyOrder(
                    "BUY_ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.5")
            );

            SellOrder sellOrder = new SellOrder(
                    "SELL_ORDER_456",
                    Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD),
                    new BigDecimal("0.3")
            );

            Money price = Money.of("44500.00", Currency.USD);
            BigDecimal quantity = new BigDecimal("0.3");

            // When
            List<ValidationErrorMessage> errors = validationService.validateTransactionCreation(buyOrder, sellOrder, price, quantity);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Should reject transaction creation with mismatched symbols")
        void shouldRejectTransactionCreationWithMismatchedSymbols() {
            // Given
            BuyOrder buyOrder = new BuyOrder(
                    "BUY_ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.5")
            );

            SellOrder sellOrder = new SellOrder(
                    "SELL_ORDER_456",
                    Symbol.ethUsd(), // Different symbol
                    Money.of("3000.00", Currency.USD),
                    new BigDecimal("0.3")
            );

            Money price = Money.of("44500.00", Currency.USD);
            BigDecimal quantity = new BigDecimal("0.3");

            // When
            List<ValidationErrorMessage> errors = validationService.validateTransactionCreation(buyOrder, sellOrder, price, quantity);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getMessage()).contains("Buy and sell orders must have the same symbol");
        }

        @Test
        @DisplayName("Should reject transaction creation when orders cannot match")
        void shouldRejectTransactionCreationWhenOrdersCannotMatch() {
            // Given
            BuyOrder buyOrder = new BuyOrder(
                    "BUY_ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD), // Lower than sell price
                    new BigDecimal("0.5")
            );

            SellOrder sellOrder = new SellOrder(
                    "SELL_ORDER_456",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), // Higher than buy price
                    new BigDecimal("0.3")
            );

            Money price = Money.of("44500.00", Currency.USD);
            BigDecimal quantity = new BigDecimal("0.3");

            // When
            List<ValidationErrorMessage> errors = validationService.validateTransactionCreation(buyOrder, sellOrder, price, quantity);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getMessage()).contains("Buy order price must be greater than or equal to sell order price");
        }

        @Test
        @DisplayName("Should reject transaction creation with invalid execution price")
        void shouldRejectTransactionCreationWithInvalidExecutionPrice() {
            // Given
            BuyOrder buyOrder = new BuyOrder(
                    "BUY_ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.5")
            );

            SellOrder sellOrder = new SellOrder(
                    "SELL_ORDER_456",
                    Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD),
                    new BigDecimal("0.3")
            );

            Money price = Money.of("46000.00", Currency.USD); // Above buy price
            BigDecimal quantity = new BigDecimal("0.3");

            // When
            List<ValidationErrorMessage> errors = validationService.validateTransactionCreation(buyOrder, sellOrder, price, quantity);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getMessage()).contains("Execution price must be between sell price and buy price");
        }

        @Test
        @DisplayName("Should reject transaction creation with excessive quantity")
        void shouldRejectTransactionCreationWithExcessiveQuantity() {
            // Given
            BuyOrder buyOrder = new BuyOrder(
                    "BUY_ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.5")
            );

            SellOrder sellOrder = new SellOrder(
                    "SELL_ORDER_456",
                    Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD),
                    new BigDecimal("0.3")
            );

            Money price = Money.of("44500.00", Currency.USD);
            BigDecimal quantity = new BigDecimal("0.6"); // Exceeds buy order quantity

            // When
            List<ValidationErrorMessage> errors = validationService.validateTransactionCreation(buyOrder, sellOrder, price, quantity);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getMessage()).contains("Transaction quantity cannot exceed buy order remaining quantity");
        }
    }
}