package core.ms.order_book;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order_book.domain.value_object.BuyOrderPriorityCalculator;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Buy Order Priority Calculator Tests")
class BuyOrderPriorityCalculatorTest {

    private BuyOrderPriorityCalculator calculator;
    private IBuyOrder buyOrder1;
    private IBuyOrder buyOrder2;
    private LocalDateTime earlierTime;
    private LocalDateTime laterTime;

    @BeforeEach
    void setUp() {
        calculator = new BuyOrderPriorityCalculator();
        earlierTime = LocalDateTime.of(2024, 1, 1, 10, 0);
        laterTime = LocalDateTime.of(2024, 1, 1, 10, 5);

        buyOrder1 = mock(IBuyOrder.class);
        buyOrder2 = mock(IBuyOrder.class);
    }

    @Nested
    @DisplayName("Price Priority Tests")
    class PricePriorityTests {

        @Test
        @DisplayName("Should prioritize higher price over lower price")
        void should_prioritize_higher_price_over_lower_price() {
            // Given: Two different prices
            Money higherPrice = Money.of("100.50", Currency.USD);
            Money lowerPrice = Money.of("100.00", Currency.USD);

            // When: Comparing prices
            boolean result = calculator.isPriceBetter(higherPrice, lowerPrice);

            // Then: Higher price should be better
            assertTrue(result);
        }

        @Test
        @DisplayName("Should not prioritize lower price over higher price")
        void should_not_prioritize_lower_price_over_higher_price() {
            // Given: Two different prices
            Money higherPrice = Money.of("100.50", Currency.USD);
            Money lowerPrice = Money.of("100.00", Currency.USD);

            // When: Comparing prices (reversed)
            boolean result = calculator.isPriceBetter(lowerPrice, higherPrice);

            // Then: Lower price should not be better
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false for equal prices")
        void should_return_false_for_equal_prices() {
            // Given: Same price
            Money price1 = Money.of("100.00", Currency.USD);
            Money price2 = Money.of("100.00", Currency.USD);

            // When: Comparing equal prices
            boolean result = calculator.isPriceBetter(price1, price2);

            // Then: Neither should be better
            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle null prices")
        void should_handle_null_prices() {
            // Given: Null prices
            Money validPrice = Money.of("100.00", Currency.USD);

            // When/Then: Should throw exception
            assertThrows(NullPointerException.class, () ->
                    calculator.isPriceBetter(null, validPrice));
            assertThrows(NullPointerException.class, () ->
                    calculator.isPriceBetter(validPrice, null));
        }
    }

    @Nested
    @DisplayName("Order Priority Tests")
    class OrderPriorityTests {

        @Test
        @DisplayName("Should prioritize higher price order over lower price order")
        void should_prioritize_higher_price_order_over_lower_price_order() {
            // Given: Two orders with different prices
            Money higherPrice = Money.of("100.50", Currency.USD);
            Money lowerPrice = Money.of("100.00", Currency.USD);

            when(buyOrder1.getPrice()).thenReturn(higherPrice);
            when(buyOrder1.getCreatedAt()).thenReturn(earlierTime);
            when(buyOrder2.getPrice()).thenReturn(lowerPrice);
            when(buyOrder2.getCreatedAt()).thenReturn(laterTime);

            // When: Comparing order priority
            boolean result = calculator.isHigherPriority(buyOrder1, buyOrder2);

            // Then: Higher price order should have priority
            assertTrue(result);
        }

        @Test
        @DisplayName("Should prioritize earlier time when same price")
        void should_prioritize_earlier_time_when_same_price() {
            // Given: Two orders at same price, different times
            Money samePrice = Money.of("100.00", Currency.USD);

            when(buyOrder1.getPrice()).thenReturn(samePrice);
            when(buyOrder1.getCreatedAt()).thenReturn(earlierTime);
            when(buyOrder2.getPrice()).thenReturn(samePrice);
            when(buyOrder2.getCreatedAt()).thenReturn(laterTime);

            // When: Comparing order priority
            boolean result = calculator.isHigherPriority(buyOrder1, buyOrder2);

            // Then: Earlier order should have priority
            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle equal price and time")
        void should_handle_equal_price_and_time() {
            // Given: Identical orders
            Money samePrice = Money.of("100.00", Currency.USD);
            LocalDateTime sameTime = LocalDateTime.of(2024, 1, 1, 10, 0);

            when(buyOrder1.getPrice()).thenReturn(samePrice);
            when(buyOrder1.getCreatedAt()).thenReturn(sameTime);
            when(buyOrder2.getPrice()).thenReturn(samePrice);
            when(buyOrder2.getCreatedAt()).thenReturn(sameTime);

            // When: Comparing order priority
            boolean result = calculator.isHigherPriority(buyOrder1, buyOrder2);

            // Then: Should return false (no priority)
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Utility Methods Tests")
    class UtilityMethodsTests {

        @Test
        @DisplayName("Should calculate price difference correctly")
        void should_calculate_price_difference_correctly() {
            // Given: Two prices
            Money price1 = Money.of("100.50", Currency.USD);
            Money price2 = Money.of("100.00", Currency.USD);

            // When: Calculating difference
            Money result = calculator.calculatePriceDifference(price1, price2);

            // Then: Should return absolute difference
            assertEquals(Money.of("0.50", Currency.USD), result);
        }

        @Test
        @DisplayName("Should handle negative difference")
        void should_handle_negative_difference() {
            // Given: Two prices (reversed)
            Money price1 = Money.of("100.00", Currency.USD);
            Money price2 = Money.of("100.50", Currency.USD);

            // When: Calculating difference
            Money result = calculator.calculatePriceDifference(price1, price2);

            // Then: Should return absolute difference
            assertEquals(Money.of("0.50", Currency.USD), result);
        }

        @Test
        @DisplayName("Should identify same price orders")
        void should_identify_same_price_orders() {
            // Given: Orders with same price
            Money samePrice = Money.of("100.00", Currency.USD);
            when(buyOrder1.getPrice()).thenReturn(samePrice);
            when(buyOrder2.getPrice()).thenReturn(samePrice);

            // When: Checking if same price
            boolean result = calculator.hasSamePrice(buyOrder1, buyOrder2);

            // Then: Should return true
            assertTrue(result);
        }

        @Test
        @DisplayName("Should identify different price orders")
        void should_identify_different_price_orders() {
            // Given: Orders with different prices
            when(buyOrder1.getPrice()).thenReturn(Money.of("100.50", Currency.USD));
            when(buyOrder2.getPrice()).thenReturn(Money.of("100.00", Currency.USD));

            // When: Checking if same price
            boolean result = calculator.hasSamePrice(buyOrder1, buyOrder2);

            // Then: Should return false
            assertFalse(result);
        }
    }
}