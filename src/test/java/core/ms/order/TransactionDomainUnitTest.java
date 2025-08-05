package core.ms.order;

import core.ms.order.domain.entities.*;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DisplayName("Transaction Domain Unit Tests")
class TransactionDomainUnitTest {

    @Nested
    @DisplayName("Transaction Creation Tests")
    class TransactionCreationTests {

        @Test
        @DisplayName("Should create a valid transaction")
        void shouldCreateValidTransaction() {
            // Given
            IBuyOrder buyOrder = new BuyOrder(
                    "BUY_ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.5")
            );

            ISellOrder sellOrder = new SellOrder(
                    "SELL_ORDER_456",
                    Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD),
                    new BigDecimal("0.3")
            );

            Money executionPrice = Money.of("44500.00", Currency.USD);
            BigDecimal quantity = new BigDecimal("0.3");

            // When
            Transaction transaction = new Transaction(
                    "TXN_789",
                    Symbol.btcUsd(),
                    buyOrder,
                    sellOrder,
                    executionPrice,
                    quantity
            );

            // Then
            assertThat(transaction.getId()).isEqualTo("TXN_789");
            assertThat(transaction.getSymbol()).isEqualTo(Symbol.btcUsd());
            assertThat(transaction.getBuyOrder()).isEqualTo(buyOrder);
            assertThat(transaction.getSellOrder()).isEqualTo(sellOrder);
            assertThat(transaction.getPrice()).isEqualTo(executionPrice);
            assertThat(transaction.getQuantity()).isEqualTo(quantity);
            assertThat(transaction.getTotalValue()).isEqualTo(Money.of("13350.00", Currency.USD)); // 44500 * 0.3
        }

        @Test
        @DisplayName("Should create transaction from matching orders using factory method")
        void shouldCreateTransactionFromMatchingOrders() {
            // Given
            IBuyOrder buyOrder = new BuyOrder(
                    "BUY_ORDER_123",
                    Symbol.ethUsd(),
                    Money.of("3000.00", Currency.USD),
                    new BigDecimal("1.0")
            );

            ISellOrder sellOrder = new SellOrder(
                    "SELL_ORDER_456",
                    Symbol.ethUsd(),
                    Money.of("2950.00", Currency.USD),
                    new BigDecimal("1.5")
            );

            Money executionPrice = Money.of("2975.00", Currency.USD);
            BigDecimal executionQuantity = new BigDecimal("1.0");

            // When
            Transaction transaction = Transaction.fromMatchingOrders(
                    "TXN_789",
                    buyOrder,
                    sellOrder,
                    executionPrice,
                    executionQuantity
            );

            // Then
            assertThat(transaction.getId()).isEqualTo("TXN_789");
            assertThat(transaction.getSymbol()).isEqualTo(Symbol.ethUsd());
            assertThat(transaction.getBuyOrder()).isEqualTo(buyOrder);
            assertThat(transaction.getSellOrder()).isEqualTo(sellOrder);
            assertThat(transaction.getPrice()).isEqualTo(executionPrice);
            assertThat(transaction.getQuantity()).isEqualTo(executionQuantity);
        }
    }

    @Nested
    @DisplayName("Transaction Validation Tests")
    class TransactionValidationTests {

        @Test
        @DisplayName("Should reject transaction with mismatched symbols")
        void shouldRejectTransactionWithMismatchedSymbols() {
            // Given
            IBuyOrder buyOrder = new BuyOrder(
                    "BUY_ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.5")
            );

            ISellOrder sellOrder = new SellOrder(
                    "SELL_ORDER_456",
                    Symbol.ethUsd(), // Different symbol
                    Money.of("3000.00", Currency.USD),
                    new BigDecimal("0.3")
            );

            // When & Then
            assertThatThrownBy(() -> new Transaction(
                    "TXN_789",
                    Symbol.btcUsd(),
                    buyOrder,
                    sellOrder,
                    Money.of("40000.00", Currency.USD),
                    new BigDecimal("0.3")
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("All orders must have the same symbol");
        }

        @Test
        @DisplayName("Should reject transaction when buy price is less than sell price")
        void shouldRejectTransactionWhenBuyPriceLessThanSellPrice() {
            // Given
            IBuyOrder buyOrder = new BuyOrder(
                    "BUY_ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD), // Lower than sell price
                    new BigDecimal("0.5")
            );

            ISellOrder sellOrder = new SellOrder(
                    "SELL_ORDER_456",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), // Higher than buy price
                    new BigDecimal("0.3")
            );

            // When & Then
            assertThatThrownBy(() -> new Transaction(
                    "TXN_789",
                    Symbol.btcUsd(),
                    buyOrder,
                    sellOrder,
                    Money.of("44500.00", Currency.USD),
                    new BigDecimal("0.3")
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Orders cannot match: buy price is less than sell price");
        }

        @Test
        @DisplayName("Should reject transaction with execution price outside valid range")
        void shouldRejectTransactionWithExecutionPriceOutsideValidRange() {
            // Given
            IBuyOrder buyOrder = new BuyOrder(
                    "BUY_ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.5")
            );

            ISellOrder sellOrder = new SellOrder(
                    "SELL_ORDER_456",
                    Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD),
                    new BigDecimal("0.3")
            );

            Money invalidExecutionPrice = Money.of("46000.00", Currency.USD); // Above buy price

            // When & Then
            assertThatThrownBy(() -> new Transaction(
                    "TXN_789",
                    Symbol.btcUsd(),
                    buyOrder,
                    sellOrder,
                    invalidExecutionPrice,
                    new BigDecimal("0.3")
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Execution price must be between sell price and buy price");
        }

        @Test
        @DisplayName("Should reject transaction with quantity exceeding order limits")
        void shouldRejectTransactionWithQuantityExceedingOrderLimits() {
            // Given
            IBuyOrder buyOrder = new BuyOrder(
                    "BUY_ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.5")
            );

            ISellOrder sellOrder = new SellOrder(
                    "SELL_ORDER_456",
                    Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD),
                    new BigDecimal("0.3")
            );

            BigDecimal excessiveQuantity = new BigDecimal("0.6"); // Exceeds buy order quantity

            // When & Then
            assertThatThrownBy(() -> new Transaction(
                    "TXN_789",
                    Symbol.btcUsd(),
                    buyOrder,
                    sellOrder,
                    Money.of("44500.00", Currency.USD),
                    excessiveQuantity
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Transaction quantity cannot exceed buy order remaining quantity");
        }

        @Test
        @DisplayName("Should reject transaction with negative quantity")
        void shouldRejectTransactionWithNegativeQuantity() {
            // Given
            IBuyOrder buyOrder = new BuyOrder(
                    "BUY_ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.5")
            );

            ISellOrder sellOrder = new SellOrder(
                    "SELL_ORDER_456",
                    Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD),
                    new BigDecimal("0.3")
            );

            BigDecimal negativeQuantity = new BigDecimal("-0.1");

            // When & Then
            assertThatThrownBy(() -> new Transaction(
                    "TXN_789",
                    Symbol.btcUsd(),
                    buyOrder,
                    sellOrder,
                    Money.of("44500.00", Currency.USD),
                    negativeQuantity
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quantity must be positive");
        }
    }

    @Nested
    @DisplayName("Transaction Pricing Logic Tests")
    class TransactionPricingTests {

        @Test
        @DisplayName("Should calculate optimal execution price using midpoint")
        void shouldCalculateOptimalExecutionPriceUsingMidpoint() {
            // Given
            IBuyOrder buyOrder = new BuyOrder(
                    "BUY_ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.5")
            );

            ISellOrder sellOrder = new SellOrder(
                    "SELL_ORDER_456",
                    Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD),
                    new BigDecimal("0.3")
            );

            // When
            Money executionPrice = AbstractTransaction.determineExecutionPrice(buyOrder, sellOrder);

            // Then
            Money expectedPrice = Money.of("44500.00", Currency.USD); // (45000 + 44000) / 2
            assertThat(executionPrice).isEqualTo(expectedPrice);
        }

        @Test
        @DisplayName("Should reject execution price calculation for non-matching orders")
        void shouldRejectExecutionPriceCalculationForNonMatchingOrders() {
            // Given
            IBuyOrder buyOrder = new BuyOrder(
                    "BUY_ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD), // Lower than sell price
                    new BigDecimal("0.5")
            );

            ISellOrder sellOrder = new SellOrder(
                    "SELL_ORDER_456",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), // Higher than buy price
                    new BigDecimal("0.3")
            );

            // When & Then
            assertThatThrownBy(() -> AbstractTransaction.determineExecutionPrice(buyOrder, sellOrder))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Orders cannot match: buy price is less than sell price");
        }
    }

    @Nested
    @DisplayName("Transaction Business Logic Tests")
    class TransactionBusinessLogicTests {

        @Test
        @DisplayName("Should calculate total value correctly")
        void shouldCalculateTotalValueCorrectly() {
            // Given
            IBuyOrder buyOrder = new BuyOrder(
                    "BUY_ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.5")
            );

            ISellOrder sellOrder = new SellOrder(
                    "SELL_ORDER_456",
                    Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD),
                    new BigDecimal("0.3")
            );

            Transaction transaction = new Transaction(
                    "TXN_789",
                    Symbol.btcUsd(),
                    buyOrder,
                    sellOrder,
                    Money.of("44500.00", Currency.USD),
                    new BigDecimal("0.25")
            );

            // When
            Money totalValue = transaction.getTotalValue();

            // Then
            Money expectedValue = Money.of("11125.00", Currency.USD); // 44500 * 0.25
            assertThat(totalValue).isEqualTo(expectedValue);
        }

        @Test
        @DisplayName("Should maintain transaction timestamp")
        void shouldMaintainTransactionTimestamp() {
            // Given
            IBuyOrder buyOrder = new BuyOrder(
                    "BUY_ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.5")
            );

            ISellOrder sellOrder = new SellOrder(
                    "SELL_ORDER_456",
                    Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD),
                    new BigDecimal("0.3")
            );

            // When
            Transaction transaction = new Transaction(
                    "TXN_789",
                    Symbol.btcUsd(),
                    buyOrder,
                    sellOrder,
                    Money.of("44500.00", Currency.USD),
                    new BigDecimal("0.25")
            );

            // Then
            assertThat(transaction.getCreatedAt()).isNotNull();
            assertThat(transaction.getCreatedAt()).isBeforeOrEqualTo(java.time.LocalDateTime.now());
        }

        @Test
        @DisplayName("Should handle transaction equality correctly")
        void shouldHandleTransactionEqualityCorrectly() {
            // Given
            IBuyOrder buyOrder = new BuyOrder(
                    "BUY_ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.5")
            );

            ISellOrder sellOrder = new SellOrder(
                    "SELL_ORDER_456",
                    Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD),
                    new BigDecimal("0.3")
            );

            Transaction transaction1 = new Transaction(
                    "TXN_789",
                    Symbol.btcUsd(),
                    buyOrder,
                    sellOrder,
                    Money.of("44500.00", Currency.USD),
                    new BigDecimal("0.25")
            );

            Transaction transaction2 = new Transaction(
                    "TXN_789", // Same ID
                    Symbol.btcUsd(),
                    buyOrder,
                    sellOrder,
                    Money.of("3000.00", Currency.USD),
                    new BigDecimal("1.0")
            );

            Transaction transaction3 = new Transaction(
                    "TXN_999", // Different ID
                    Symbol.btcUsd(),
                    buyOrder,
                    sellOrder,
                    Money.of("44500.00", Currency.USD),
                    new BigDecimal("0.25")
            );

            // When & Then
            assertThat(transaction1).isEqualTo(transaction2); // Same ID
            assertThat(transaction1).isNotEqualTo(transaction3); // Different ID
            assertThat(transaction1.hashCode()).isEqualTo(transaction2.hashCode());
        }

        @Test
        @DisplayName("Should have meaningful toString representation")
        void shouldHaveMeaningfulToStringRepresentation() {
            // Given
            IBuyOrder buyOrder = new BuyOrder(
                    "BUY_ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.5")
            );

            ISellOrder sellOrder = new SellOrder(
                    "SELL_ORDER_456",
                    Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD),
                    new BigDecimal("0.3")
            );

            Transaction transaction = new Transaction(
                    "TXN_789",
                    Symbol.btcUsd(),
                    buyOrder,
                    sellOrder,
                    Money.of("44500.00", Currency.USD),
                    new BigDecimal("0.25")
            );

            // When
            String toString = transaction.toString();

            // Then
            assertThat(toString).contains("TXN_789");
            assertThat(toString).contains("BTC");
            assertThat(toString).contains("0.25");
            assertThat(toString).contains("44500.00");
        }
    }
}