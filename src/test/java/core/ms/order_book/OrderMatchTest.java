package core.ms.order_book;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order_book.domain.value_object.OrderMatch;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Order Match Tests")
class OrderMatchTest {

    private IBuyOrder buyOrder;
    private ISellOrder sellOrder;
    private Symbol symbol;
    private LocalDateTime earlierTime;
    private LocalDateTime laterTime;

    @BeforeEach
    void setUp() {
        symbol = Symbol.btcUsd();
        buyOrder = mock(IBuyOrder.class);
        sellOrder = mock(ISellOrder.class);
        earlierTime = LocalDateTime.of(2024, 1, 1, 10, 0);
        laterTime = LocalDateTime.of(2024, 1, 1, 10, 5);
    }

    @Nested
    @DisplayName("Order Match Creation Tests")
    class OrderMatchCreationTests {

        @Test
        @DisplayName("Should create valid order match with buy order having time priority")
        void should_create_valid_order_match_with_buy_order_time_priority() {
            // Given: Buy order arrives first (has time priority)
            setupCompatibleOrdersWithTime(
                    Money.of("100.00", Currency.USD),
                    Money.of("99.00", Currency.USD),
                    new BigDecimal("10"),
                    new BigDecimal("5"),
                    earlierTime,  // Buy order first
                    laterTime     // Sell order later
            );

            // When: Creating order match
            OrderMatch match = new OrderMatch(buyOrder, sellOrder);

            // Then: Should create valid match with buy order price (time priority)
            assertNotNull(match);
            assertEquals(buyOrder, match.getBuyOrder());
            assertEquals(sellOrder, match.getSellOrder());
            assertEquals(new BigDecimal("5"), match.getMatchableQuantity());
            assertEquals(Money.of("100.00", Currency.USD), match.getSuggestedPrice()); // Buy order price
            assertTrue(match.isValid());
            assertNotNull(match.getTimestamp());
        }

        @Test
        @DisplayName("Should create valid order match with sell order having time priority")
        void should_create_valid_order_match_with_sell_order_time_priority() {
            // Given: Sell order arrives first (has time priority)
            setupCompatibleOrdersWithTime(
                    Money.of("100.00", Currency.USD),
                    Money.of("99.00", Currency.USD),
                    new BigDecimal("10"),
                    new BigDecimal("5"),
                    laterTime,    // Buy order later
                    earlierTime   // Sell order first
            );

            // When: Creating order match
            OrderMatch match = new OrderMatch(buyOrder, sellOrder);

            // Then: Should create valid match with sell order price (time priority)
            assertNotNull(match);
            assertEquals(buyOrder, match.getBuyOrder());
            assertEquals(sellOrder, match.getSellOrder());
            assertEquals(new BigDecimal("5"), match.getMatchableQuantity());
            assertEquals(Money.of("99.00", Currency.USD), match.getSuggestedPrice()); // Sell order price
            assertTrue(match.isValid());
            assertNotNull(match.getTimestamp());
        }

        @Test
        @DisplayName("Should use midpoint pricing when orders have same timestamp")
        void should_use_midpoint_pricing_when_orders_have_same_timestamp() {
            // Given: Orders with same timestamp
            LocalDateTime sameTime = LocalDateTime.of(2024, 1, 1, 10, 0);
            setupCompatibleOrdersWithTime(
                    Money.of("100.00", Currency.USD),
                    Money.of("98.00", Currency.USD),
                    new BigDecimal("10"),
                    new BigDecimal("5"),
                    sameTime,  // Same time
                    sameTime   // Same time
            );

            // When: Creating order match
            OrderMatch match = new OrderMatch(buyOrder, sellOrder);

            // Then: Should use midpoint pricing
            assertEquals(Money.of("99.00", Currency.USD), match.getSuggestedPrice()); // Midpoint
            assertTrue(match.isValid());
        }

        @Test
        @DisplayName("Should calculate total value correctly with time priority")
        void should_calculate_total_value_correctly_with_time_priority() {
            // Given: Sell order has time priority
            setupCompatibleOrdersWithTime(
                    Money.of("100.00", Currency.USD),
                    Money.of("98.00", Currency.USD),
                    new BigDecimal("10"),
                    new BigDecimal("5"),
                    laterTime,    // Buy order later
                    earlierTime   // Sell order first (time priority)
            );

            // When: Creating order match
            OrderMatch match = new OrderMatch(buyOrder, sellOrder);

            // Then: Should calculate total value using sell order price
            Money expectedPrice = Money.of("98.00", Currency.USD); // Sell order price (time priority)
            BigDecimal expectedQuantity = new BigDecimal("5");
            Money expectedTotal = expectedPrice.multiply(expectedQuantity);

            assertEquals(expectedTotal, match.getTotalValue());
        }

        @Test
        @DisplayName("Should reject incompatible symbols")
        void should_reject_incompatible_symbols() {
            // Given: Orders with different symbols
            when(buyOrder.getSymbol()).thenReturn(Symbol.btcUsd());
            when(sellOrder.getSymbol()).thenReturn(Symbol.ethUsd());
            when(buyOrder.getPrice()).thenReturn(Money.of("100.00", Currency.USD));
            when(sellOrder.getPrice()).thenReturn(Money.of("99.00", Currency.USD));
            when(buyOrder.isActive()).thenReturn(true);
            when(sellOrder.isActive()).thenReturn(true);
            when(buyOrder.getCreatedAt()).thenReturn(earlierTime);
            when(sellOrder.getCreatedAt()).thenReturn(laterTime);

            // When/Then: Should throw exception
            assertThrows(IllegalArgumentException.class, () ->
                    new OrderMatch(buyOrder, sellOrder));
        }

        @Test
        @DisplayName("Should reject invalid price combination")
        void should_reject_invalid_price_combination() {
            // Given: Buy price < sell price
            setupCompatibleOrdersWithTime(
                    Money.of("98.00", Currency.USD),  // Buy price lower
                    Money.of("100.00", Currency.USD), // Sell price higher
                    new BigDecimal("10"),
                    new BigDecimal("5"),
                    earlierTime,
                    laterTime
            );

            // When/Then: Should throw exception
            assertThrows(IllegalArgumentException.class, () ->
                    new OrderMatch(buyOrder, sellOrder));
        }

        @Test
        @DisplayName("Should reject inactive orders")
        void should_reject_inactive_orders() {
            // Given: Inactive buy order
            setupCompatibleOrdersWithTime(
                    Money.of("100.00", Currency.USD),
                    Money.of("99.00", Currency.USD),
                    new BigDecimal("10"),
                    new BigDecimal("5"),
                    earlierTime,
                    laterTime
            );
            when(buyOrder.isActive()).thenReturn(false);

            // When/Then: Should throw exception
            assertThrows(IllegalArgumentException.class, () ->
                    new OrderMatch(buyOrder, sellOrder));
        }
    }

    @Nested
    @DisplayName("Price-Time Priority Logic Tests")
    class PriceTimePriorityTests {

        @Test
        @DisplayName("Should give time priority to buy order when it arrives first")
        void should_give_time_priority_to_buy_order_when_it_arrives_first() {
            // Given: Buy order arrives 1 hour before sell order
            LocalDateTime buyTime = LocalDateTime.of(2024, 1, 1, 9, 0);  // 9:00 AM
            LocalDateTime sellTime = LocalDateTime.of(2024, 1, 1, 10, 0); // 10:00 AM

            setupCompatibleOrdersWithTime(
                    Money.of("105.00", Currency.USD), // Buy price
                    Money.of("100.00", Currency.USD), // Sell price
                    new BigDecimal("8"),
                    new BigDecimal("12"),
                    buyTime,   // Buy order first
                    sellTime   // Sell order later
            );

            // When: Creating order match
            OrderMatch match = new OrderMatch(buyOrder, sellOrder);

            // Then: Should execute at buy order price (resting order gets filled)
            assertEquals(Money.of("105.00", Currency.USD), match.getSuggestedPrice());
            assertEquals(new BigDecimal("8"), match.getMatchableQuantity()); // Min quantity
        }

        @Test
        @DisplayName("Should give time priority to sell order when it arrives first")
        void should_give_time_priority_to_sell_order_when_it_arrives_first() {
            // Given: Sell order arrives before buy order
            LocalDateTime sellTime = LocalDateTime.of(2024, 1, 1, 9, 30);  // 9:30 AM
            LocalDateTime buyTime = LocalDateTime.of(2024, 1, 1, 10, 15);  // 10:15 AM

            setupCompatibleOrdersWithTime(
                    Money.of("102.50", Currency.USD), // Buy price
                    Money.of("101.75", Currency.USD), // Sell price
                    new BigDecimal("15"),
                    new BigDecimal("7"),
                    buyTime,   // Buy order later
                    sellTime   // Sell order first
            );

            // When: Creating order match
            OrderMatch match = new OrderMatch(buyOrder, sellOrder);

            // Then: Should execute at sell order price (resting order gets filled)
            assertEquals(Money.of("101.75", Currency.USD), match.getSuggestedPrice());
            assertEquals(new BigDecimal("7"), match.getMatchableQuantity()); // Min quantity
        }

        @Test
        @DisplayName("Should handle microsecond time differences")
        void should_handle_microsecond_time_differences() {
            // Given: Orders with very small time difference
            LocalDateTime firstTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0, 100000);  // .1 second
            LocalDateTime secondTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0, 200000); // .2 second

            setupCompatibleOrdersWithTime(
                    Money.of("100.00", Currency.USD),
                    Money.of("99.50", Currency.USD),
                    new BigDecimal("5"),
                    new BigDecimal("5"),
                    secondTime, // Buy order later
                    firstTime   // Sell order first (by 0.1 second)
            );

            // When: Creating order match
            OrderMatch match = new OrderMatch(buyOrder, sellOrder);

            // Then: Should still respect time priority
            assertEquals(Money.of("99.50", Currency.USD), match.getSuggestedPrice()); // Sell order price
        }
    }

    @Nested
    @DisplayName("Aggressor-Based Pricing Tests")
    class AggressorBasedPricingTests {

        @Test
        @DisplayName("Should calculate price correctly when buy order is aggressor")
        void should_calculate_price_correctly_when_buy_order_is_aggressor() {
            // Given: Compatible orders
            setupCompatibleOrdersWithTime(
                    Money.of("103.00", Currency.USD),
                    Money.of("102.00", Currency.USD),
                    new BigDecimal("10"),
                    new BigDecimal("5"),
                    earlierTime,
                    laterTime
            );

            // When: Creating match and calculating with buy aggressor
            OrderMatch match = new OrderMatch(buyOrder, sellOrder);
            Money aggressorPrice = match.calculatePriceWithAggressor(true); // Buy is aggressor

            // Then: Should use sell order price (aggressor pays)
            assertEquals(Money.of("102.00", Currency.USD), aggressorPrice);
        }

        @Test
        @DisplayName("Should calculate price correctly when sell order is aggressor")
        void should_calculate_price_correctly_when_sell_order_is_aggressor() {
            // Given: Compatible orders
            setupCompatibleOrdersWithTime(
                    Money.of("103.00", Currency.USD),
                    Money.of("102.00", Currency.USD),
                    new BigDecimal("10"),
                    new BigDecimal("5"),
                    earlierTime,
                    laterTime
            );

            // When: Creating match and calculating with sell aggressor
            OrderMatch match = new OrderMatch(buyOrder, sellOrder);
            Money aggressorPrice = match.calculatePriceWithAggressor(false); // Sell is aggressor

            // Then: Should use buy order price (aggressor accepts)
            assertEquals(Money.of("103.00", Currency.USD), aggressorPrice);
        }
    }

    private void setupCompatibleOrdersWithTime(Money buyPrice, Money sellPrice,
                                               BigDecimal buyQuantity, BigDecimal sellQuantity,
                                               LocalDateTime buyTime, LocalDateTime sellTime) {
        when(buyOrder.getSymbol()).thenReturn(symbol);
        when(sellOrder.getSymbol()).thenReturn(symbol);
        when(buyOrder.getPrice()).thenReturn(buyPrice);
        when(sellOrder.getPrice()).thenReturn(sellPrice);
        when(buyOrder.isActive()).thenReturn(true);
        when(sellOrder.isActive()).thenReturn(true);
        when(buyOrder.getRemainingQuantity()).thenReturn(buyQuantity);
        when(sellOrder.getRemainingQuantity()).thenReturn(sellQuantity);
        when(buyOrder.getCreatedAt()).thenReturn(buyTime);
        when(sellOrder.getCreatedAt()).thenReturn(sellTime);
    }

    // Backward compatibility method
    private void setupCompatibleOrders(Money buyPrice, Money sellPrice,
                                       BigDecimal buyQuantity, BigDecimal sellQuantity) {
        setupCompatibleOrdersWithTime(buyPrice, sellPrice, buyQuantity, sellQuantity,
                earlierTime, laterTime);
    }
}