package core.ms.order_book;

import core.ms.order.domain.entities.ISellOrder;
import core.ms.order_book.domain.value_object.SellOrderPriorityCalculator;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Sell Order Priority Calculator Tests")
class SellOrderPriorityCalculatorTest {

    private SellOrderPriorityCalculator calculator;
    private ISellOrder sellOrder1;
    private ISellOrder sellOrder2;
    private LocalDateTime earlierTime;
    private LocalDateTime laterTime;

    @BeforeEach
    void setUp() {
        calculator = new SellOrderPriorityCalculator();
        earlierTime = LocalDateTime.of(2024, 1, 1, 10, 0);
        laterTime = LocalDateTime.of(2024, 1, 1, 10, 5);

        sellOrder1 = mock(ISellOrder.class);
        sellOrder2 = mock(ISellOrder.class);
    }

    @Nested
    @DisplayName("Price Priority Tests")
    class PricePriorityTests {

        @Test
        @DisplayName("Should prioritize lower price over higher price")
        void should_prioritize_lower_price_over_higher_price() {
            // Given: Two different prices
            Money lowerPrice = Money.of("99.50", Currency.USD);
            Money higherPrice = Money.of("100.00", Currency.USD);

            // When: Comparing prices
            boolean result = calculator.isPriceBetter(lowerPrice, higherPrice);

            // Then: Lower price should be better
            assertTrue(result);
        }

        @Test
        @DisplayName("Should not prioritize higher price over lower price")
        void should_not_prioritize_higher_price_over_lower_price() {
            // Given: Two different prices
            Money lowerPrice = Money.of("99.50", Currency.USD);
            Money higherPrice = Money.of("100.00", Currency.USD);

            // When: Comparing prices (reversed)
            boolean result = calculator.isPriceBetter(higherPrice, lowerPrice);

            // Then: Higher price should not be better
            assertFalse(result);
        }

        @Test
        @DisplayName("Should prioritize earlier time when same price")
        void should_prioritize_earlier_time_when_same_price() {
            // Given: Two orders at same price, different times
            Money samePrice = Money.of("100.00", Currency.USD);

            when(sellOrder1.getPrice()).thenReturn(samePrice);
            when(sellOrder1.getCreatedAt()).thenReturn(earlierTime);
            when(sellOrder2.getPrice()).thenReturn(samePrice);
            when(sellOrder2.getCreatedAt()).thenReturn(laterTime);

            // When: Comparing order priority
            boolean result = calculator.isHigherPriority(sellOrder1, sellOrder2);

            // Then: Earlier order should have priority
            assertTrue(result);
        }
    }
}