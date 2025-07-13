package core.ms.order_book;

import core.ms.order.domain.IBuyOrder;
import core.ms.order_book.domain.value_object.BidPriceLevel;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Bid Price Level Tests")
class BidPriceLevelTest {

    private BidPriceLevel bidLevel;
    private Money testPrice;
    private IBuyOrder buyOrder1;
    private IBuyOrder buyOrder2;
    private LocalDateTime earlierTime;
    private LocalDateTime laterTime;

    @BeforeEach
    void setUp() {
        // Initialize test data
        testPrice = Money.of("100.00", Currency.USD);
        bidLevel = new BidPriceLevel(testPrice);
        earlierTime = LocalDateTime.of(2024, 1, 1, 10, 0);
        laterTime = LocalDateTime.of(2024, 1, 1, 10, 5);

        // Create mocks
        buyOrder1 = mock(IBuyOrder.class);
        buyOrder2 = mock(IBuyOrder.class);

        // Setup buyOrder1 mock behavior
        when(buyOrder1.getId()).thenReturn("order1");
        when(buyOrder1.getPrice()).thenReturn(testPrice);
        when(buyOrder1.getRemainingQuantity()).thenReturn(new BigDecimal("10"));
        when(buyOrder1.getCreatedAt()).thenReturn(earlierTime);
        when(buyOrder1.isActive()).thenReturn(true);

        // Setup buyOrder2 mock behavior
        when(buyOrder2.getId()).thenReturn("order2");
        when(buyOrder2.getPrice()).thenReturn(testPrice);
        when(buyOrder2.getRemainingQuantity()).thenReturn(new BigDecimal("5"));
        when(buyOrder2.getCreatedAt()).thenReturn(laterTime);
        when(buyOrder2.isActive()).thenReturn(true);
    }

    @Nested
    @DisplayName("Order Management Tests")
    class OrderManagementTests {

        @Test
        @DisplayName("Should add buy order successfully")
        void should_add_buy_order_successfully() {
            // Given: Empty bid level
            assertTrue(bidLevel.isEmpty());

            // When: Adding order
            bidLevel.addOrder(buyOrder1);

            // Then: Level should contain order and update metrics
            assertFalse(bidLevel.isEmpty());
            assertEquals(1, bidLevel.getOrderCount());
            assertEquals(new BigDecimal("10"), bidLevel.getTotalQuantity());
            assertEquals(testPrice, bidLevel.getPrice());
            assertTrue(bidLevel.getFirstOrder().isPresent());
            assertEquals("order1", bidLevel.getFirstOrder().get().getId());
        }

        @Test
        @DisplayName("Should maintain time priority within level")
        void should_maintain_time_priority_within_level() {
            // Given: Empty bid level

            // When: Adding orders in sequence
            bidLevel.addOrder(buyOrder1); // Earlier time
            bidLevel.addOrder(buyOrder2); // Later time

            // Then: First order should be at front of queue
            assertEquals("order1", bidLevel.getFirstOrder().get().getId());
            assertEquals(2, bidLevel.getOrderCount());
            assertEquals(new BigDecimal("15"), bidLevel.getTotalQuantity());

            // And: Orders should be in time order
            assertEquals("order1", bidLevel.getOrders().get(0).getId());
            assertEquals("order2", bidLevel.getOrders().get(1).getId());
        }

        @Test
        @DisplayName("Should aggregate quantities correctly")
        void should_aggregate_quantities_correctly() {
            // Given: Multiple orders

            // When: Adding orders
            bidLevel.addOrder(buyOrder1); // 10
            bidLevel.addOrder(buyOrder2); // 5

            // Then: Should aggregate total quantity
            assertEquals(new BigDecimal("15"), bidLevel.getTotalQuantity());
            assertEquals(2, bidLevel.getOrderCount());
        }

        @Test
        @DisplayName("Should reject order with wrong price")
        void should_reject_order_with_wrong_price() {
            // Given: Order with different price
            IBuyOrder wrongPriceOrder = mock(IBuyOrder.class);
            when(wrongPriceOrder.getPrice()).thenReturn(Money.of("99.00", Currency.USD));

            // When/Then: Should throw exception
            assertThrows(IllegalArgumentException.class, () ->
                    bidLevel.addOrder(wrongPriceOrder));
        }

        @Test
        @DisplayName("Should reject null order")
        void should_reject_null_order() {
            // When/Then: Should throw exception
            assertThrows(NullPointerException.class, () ->
                    bidLevel.addOrder(null));
        }
    }

    @Nested
    @DisplayName("Order Removal Tests")
    class OrderRemovalTests {

        @Test
        @DisplayName("Should remove order and update metrics")
        void should_remove_order_and_update_metrics() {
            // Given: Level with order
            bidLevel.addOrder(buyOrder1);
            assertEquals(1, bidLevel.getOrderCount());
            assertEquals(new BigDecimal("10"), bidLevel.getTotalQuantity());

            // When: Removing order
            boolean removed = bidLevel.removeOrder(buyOrder1);

            // Then: Should remove successfully and update metrics
            assertTrue(removed);
            assertEquals(0, bidLevel.getOrderCount());
            assertEquals(BigDecimal.ZERO, bidLevel.getTotalQuantity());
            assertTrue(bidLevel.isEmpty());
            assertFalse(bidLevel.getFirstOrder().isPresent());
        }

        @Test
        @DisplayName("Should remove correct order from multiple orders")
        void should_remove_correct_order_from_multiple_orders() {
            // Given: Level with multiple orders
            bidLevel.addOrder(buyOrder1);
            bidLevel.addOrder(buyOrder2);

            // When: Removing second order
            boolean removed = bidLevel.removeOrder(buyOrder2);

            // Then: Should remove only the specified order
            assertTrue(removed);
            assertEquals(1, bidLevel.getOrderCount());
            assertEquals(new BigDecimal("10"), bidLevel.getTotalQuantity());
            assertEquals("order1", bidLevel.getFirstOrder().get().getId());
        }

        @Test
        @DisplayName("Should return false when removing non-existent order")
        void should_return_false_when_removing_non_existent_order() {
            // Given: Level with one order
            bidLevel.addOrder(buyOrder1);

            // When: Removing different order
            boolean removed = bidLevel.removeOrder(buyOrder2);

            // Then: Should return false and not affect level
            assertFalse(removed);
            assertEquals(1, bidLevel.getOrderCount());
            assertEquals(new BigDecimal("10"), bidLevel.getTotalQuantity());
        }

        @Test
        @DisplayName("Should handle removing order from empty level")
        void should_handle_removing_order_from_empty_level() {
            // Given: Empty level
            assertTrue(bidLevel.isEmpty());

            // When: Removing order
            boolean removed = bidLevel.removeOrder(buyOrder1);

            // Then: Should return false
            assertFalse(removed);
            assertTrue(bidLevel.isEmpty());
        }
    }

    @Nested
    @DisplayName("State Query Tests")
    class StateQueryTests {

        @Test
        @DisplayName("Should return empty state correctly")
        void should_return_empty_state_correctly() {
            // Given: Empty level

            // When/Then: Should be empty
            assertTrue(bidLevel.isEmpty());
            assertEquals(0, bidLevel.getOrderCount());
            assertEquals(BigDecimal.ZERO, bidLevel.getTotalQuantity());
            assertFalse(bidLevel.getFirstOrder().isPresent());
            assertTrue(bidLevel.getOrders().isEmpty());
        }

        @Test
        @DisplayName("Should return non-empty state correctly")
        void should_return_non_empty_state_correctly() {
            // Given: Level with order
            bidLevel.addOrder(buyOrder1);

            // When/Then: Should not be empty
            assertFalse(bidLevel.isEmpty());
            assertEquals(1, bidLevel.getOrderCount());
            assertEquals(new BigDecimal("10"), bidLevel.getTotalQuantity());
            assertTrue(bidLevel.getFirstOrder().isPresent());
            assertFalse(bidLevel.getOrders().isEmpty());
        }

        @Test
        @DisplayName("Should provide defensive copy of orders")
        void should_provide_defensive_copy_of_orders() {
            // Given: Level with orders
            bidLevel.addOrder(buyOrder1);
            bidLevel.addOrder(buyOrder2);

            // When: Getting orders list
            var orders = bidLevel.getOrders();

            // Then: Should be defensive copy
            assertEquals(2, orders.size());

            // And: Modifying returned list should not affect level
            orders.clear();
            assertEquals(2, bidLevel.getOrderCount()); // Should remain unchanged
        }
    }
}