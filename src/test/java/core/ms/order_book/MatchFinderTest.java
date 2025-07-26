package core.ms.order_book;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Match Finder Tests")
class MatchFinderTest {

    private MatchFinder matchFinder;
    private IBuyOrder buyOrder;
    private ISellOrder sellOrder;
    private Symbol symbol;

    @BeforeEach
    void setUp() {
        matchFinder = new MatchFinder();
        symbol = Symbol.btcUsd();

        buyOrder = mock(IBuyOrder.class);
        sellOrder = mock(ISellOrder.class);
    }

    @Nested
    @DisplayName("Order Matching Tests")
    class OrderMatchingTests {

        @Test
        @DisplayName("Should match when bid meets ask")
        void should_match_when_bid_meets_ask() {
            // Given: Compatible orders
            setupCompatibleOrders(
                    Money.of("100.00", Currency.USD), // Buy price
                    Money.of("99.00", Currency.USD),  // Sell price
                    new BigDecimal("10"),             // Buy quantity
                    new BigDecimal("5")               // Sell quantity
            );

            // When: Checking if orders can match
            boolean canMatch = matchFinder.canMatch(buyOrder, sellOrder);

            // Then: Should be able to match
            assertTrue(canMatch);
        }

        @Test
        @DisplayName("Should not match when bid below ask")
        void should_not_match_when_bid_below_ask() {
            // Given: Incompatible orders (bid < ask)
            setupCompatibleOrders(
                    Money.of("99.00", Currency.USD),  // Buy price (lower)
                    Money.of("100.00", Currency.USD), // Sell price (higher)
                    new BigDecimal("10"),
                    new BigDecimal("5")
            );

            // When: Checking if orders can match
            boolean canMatch = matchFinder.canMatch(buyOrder, sellOrder);

            // Then: Should not be able to match
            assertFalse(canMatch);
        }

        @Test
        @DisplayName("Should not match orders with different symbols")
        void should_not_match_orders_with_different_symbols() {
            // Given: Orders with different symbols
            when(buyOrder.getSymbol()).thenReturn(Symbol.btcUsd());
            when(sellOrder.getSymbol()).thenReturn(Symbol.ethUsd());
            when(buyOrder.getPrice()).thenReturn(Money.of("100.00", Currency.USD));
            when(sellOrder.getPrice()).thenReturn(Money.of("99.00", Currency.USD));
            when(buyOrder.isActive()).thenReturn(true);
            when(sellOrder.isActive()).thenReturn(true);
            when(buyOrder.getRemainingQuantity()).thenReturn(new BigDecimal("10"));
            when(sellOrder.getRemainingQuantity()).thenReturn(new BigDecimal("5"));

            // When: Checking if orders can match
            boolean canMatch = matchFinder.canMatch(buyOrder, sellOrder);

            // Then: Should not be able to match
            assertFalse(canMatch);
        }

        @Test
        @DisplayName("Should not match inactive orders")
        void should_not_match_inactive_orders() {
            // Given: Inactive buy order
            setupCompatibleOrders(
                    Money.of("100.00", Currency.USD),
                    Money.of("99.00", Currency.USD),
                    new BigDecimal("10"),
                    new BigDecimal("5")
            );
            when(buyOrder.isActive()).thenReturn(false); // Inactive

            // When: Checking if orders can match
            boolean canMatch = matchFinder.canMatch(buyOrder, sellOrder);

            // Then: Should not be able to match
            assertFalse(canMatch);
        }

        @Test
        @DisplayName("Should not match orders with zero quantity")
        void should_not_match_orders_with_zero_quantity() {
            // Given: Order with zero remaining quantity
            setupCompatibleOrders(
                    Money.of("100.00", Currency.USD),
                    Money.of("99.00", Currency.USD),
                    BigDecimal.ZERO,  // Zero quantity
                    new BigDecimal("5")
            );

            // When: Checking if orders can match
            boolean canMatch = matchFinder.canMatch(buyOrder, sellOrder);

            // Then: Should not be able to match
            assertFalse(canMatch);
        }
    }

    @Nested
    @DisplayName("Price Calculation Tests")
    class PriceCalculationTests {

        @Test
        @DisplayName("Should calculate midpoint price correctly")
        void should_calculate_midpoint_price_correctly() {
            // Given: Buy at $100, Sell at $98
            Money buyPrice = Money.of("100.00", Currency.USD);
            Money sellPrice = Money.of("98.00", Currency.USD);

            // When: Calculating match price
            Money matchPrice = matchFinder.calculateMatchPrice(buyPrice, sellPrice);

            // Then: Should return midpoint
            assertEquals(Money.of("99.00", Currency.USD), matchPrice);
        }

        @Test
        @DisplayName("Should calculate midpoint for equal prices")
        void should_calculate_midpoint_for_equal_prices() {
            // Given: Same buy and sell price
            Money price = Money.of("100.00", Currency.USD);

            // When: Calculating match price
            Money matchPrice = matchFinder.calculateMatchPrice(price, price);

            // Then: Should return same price
            assertEquals(price, matchPrice);
        }

        @Test
        @DisplayName("Should reject invalid price combination")
        void should_reject_invalid_price_combination() {
            // Given: Buy price < sell price
            Money buyPrice = Money.of("98.00", Currency.USD);
            Money sellPrice = Money.of("100.00", Currency.USD);

            // When/Then: Should throw exception
            assertThrows(IllegalArgumentException.class, () ->
                    matchFinder.calculateMatchPrice(buyPrice, sellPrice));
        }
    }

    @Nested
    @DisplayName("Quantity Calculation Tests")
    class QuantityCalculationTests {

        @Test
        @DisplayName("Should calculate minimum quantity")
        void should_calculate_minimum_quantity() {
            // Given: Buy 10 shares, Sell 5 shares
            BigDecimal buyQuantity = new BigDecimal("10");
            BigDecimal sellQuantity = new BigDecimal("5");

            // When: Calculating match quantity
            BigDecimal matchQuantity = matchFinder.calculateMatchQuantity(buyQuantity, sellQuantity);

            // Then: Should return minimum
            assertEquals(new BigDecimal("5"), matchQuantity);
        }

        @Test
        @DisplayName("Should calculate minimum when sell quantity is larger")
        void should_calculate_minimum_when_sell_quantity_is_larger() {
            // Given: Buy 5 shares, Sell 10 shares
            BigDecimal buyQuantity = new BigDecimal("5");
            BigDecimal sellQuantity = new BigDecimal("10");

            // When: Calculating match quantity
            BigDecimal matchQuantity = matchFinder.calculateMatchQuantity(buyQuantity, sellQuantity);

            // Then: Should return minimum
            assertEquals(new BigDecimal("5"), matchQuantity);
        }

        @Test
        @DisplayName("Should reject zero quantities")
        void should_reject_zero_quantities() {
            // Given: Zero quantity
            BigDecimal zeroQuantity = BigDecimal.ZERO;
            BigDecimal validQuantity = new BigDecimal("10");

            // When/Then: Should throw exception
            assertThrows(IllegalArgumentException.class, () ->
                    matchFinder.calculateMatchQuantity(zeroQuantity, validQuantity));
            assertThrows(IllegalArgumentException.class, () ->
                    matchFinder.calculateMatchQuantity(validQuantity, zeroQuantity));
        }

        @Test
        @DisplayName("Should reject negative quantities")
        void should_reject_negative_quantities() {
            // Given: Negative quantity
            BigDecimal negativeQuantity = new BigDecimal("-5");
            BigDecimal validQuantity = new BigDecimal("10");

            // When/Then: Should throw exception
            assertThrows(IllegalArgumentException.class, () ->
                    matchFinder.calculateMatchQuantity(negativeQuantity, validQuantity));
        }
    }

    private void setupCompatibleOrders(Money buyPrice, Money sellPrice,
                                       BigDecimal buyQuantity, BigDecimal sellQuantity) {
        when(buyOrder.getSymbol()).thenReturn(symbol);
        when(sellOrder.getSymbol()).thenReturn(symbol);
        when(buyOrder.getPrice()).thenReturn(buyPrice);
        when(sellOrder.getPrice()).thenReturn(sellPrice);
        when(buyOrder.isActive()).thenReturn(true);
        when(sellOrder.isActive()).thenReturn(true);
        when(buyOrder.getRemainingQuantity()).thenReturn(buyQuantity);
        when(sellOrder.getRemainingQuantity()).thenReturn(sellQuantity);
    }
}