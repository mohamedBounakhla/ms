package core.ms.order_book;

import core.ms.order.domain.ISellOrder;
import core.ms.order_book.domain.value_object.AskPriceLevel;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AskPriceLevelTest {

    private AskPriceLevel askLevel;
    private Money testPrice;
    private ISellOrder sellOrder1;
    private ISellOrder sellOrder2;
    private LocalDateTime earlierTime;
    private LocalDateTime laterTime;

    @BeforeEach
    void setUp() {
        testPrice = Money.of("100.00", Currency.USD);
        askLevel = new AskPriceLevel(testPrice);
        earlierTime = LocalDateTime.of(2024, 1, 1, 10, 0);
        laterTime = LocalDateTime.of(2024, 1, 1, 10, 5);

        sellOrder1 = mock(ISellOrder.class);
        sellOrder2 = mock(ISellOrder.class);

        // Setup common mock behavior
        when(sellOrder1.getPrice()).thenReturn(testPrice);
        when(sellOrder1.getRemainingQuantity()).thenReturn(new BigDecimal("10"));
        when(sellOrder1.getCreatedAt()).thenReturn(earlierTime);
        when(sellOrder1.getId()).thenReturn("order1");

        when(sellOrder2.getPrice()).thenReturn(testPrice);
        when(sellOrder2.getRemainingQuantity()).thenReturn(new BigDecimal("5"));
        when(sellOrder2.getCreatedAt()).thenReturn(laterTime);
        when(sellOrder2.getId()).thenReturn("order2");
    }

    @Test
    @DisplayName("Should add sell order successfully")
    void should_add_sell_order_successfully() {
        // Given: Empty ask level
        assertTrue(askLevel.isEmpty());

        // When: Adding order
        askLevel.addOrder(sellOrder1);

        // Then: Level should contain order and update metrics
        assertFalse(askLevel.isEmpty());
        assertEquals(1, askLevel.getOrderCount());
        assertEquals(new BigDecimal("10"), askLevel.getTotalQuantity());
        assertEquals(testPrice, askLevel.getPrice());
        assertTrue(askLevel.getFirstOrder().isPresent());
        assertEquals("order1", askLevel.getFirstOrder().get().getId());
    }

    @Test
    @DisplayName("Should maintain time priority within level")
    void should_maintain_time_priority_within_level() {
        // Given: Empty ask level

        // When: Adding orders in sequence
        askLevel.addOrder(sellOrder1); // Earlier time
        askLevel.addOrder(sellOrder2); // Later time

        // Then: First order should be at front of queue
        assertEquals("order1", askLevel.getFirstOrder().get().getId());
        assertEquals(2, askLevel.getOrderCount());
        assertEquals(new BigDecimal("15"), askLevel.getTotalQuantity());

        // And: Orders should be in time order
        assertEquals("order1", askLevel.getOrders().get(0).getId());
        assertEquals("order2", askLevel.getOrders().get(1).getId());
    }

    @Test
    @DisplayName("Should reject order with wrong price")
    void should_reject_order_with_wrong_price() {
        // Given: Order with different price
        ISellOrder wrongPriceOrder = mock(ISellOrder.class);
        when(wrongPriceOrder.getPrice()).thenReturn(Money.of("101.00", Currency.USD));

        // When/Then: Should throw exception
        assertThrows(IllegalArgumentException.class, () ->
                askLevel.addOrder(wrongPriceOrder));
    }
}