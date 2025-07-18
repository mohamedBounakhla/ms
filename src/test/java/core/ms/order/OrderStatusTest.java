package core.ms.order;

import core.ms.order.domain.value_objects.OrderStatus;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


@DisplayName("Order Status State Pattern Tests")
public class OrderStatusTest {

    private OrderStatus orderStatus;

    @BeforeEach
    void setUp() {
        orderStatus = new OrderStatus();
    }

    @Test
    @DisplayName("Should start in PENDING state")
    public void shouldStartInPendingState() {
        assertEquals(OrderStatusEnum.PENDING, orderStatus.getStatus());
        assertFalse(orderStatus.isTerminal());
    }

    @Test
    @DisplayName("Should transition from PENDING to PARTIAL")
    public void shouldTransitionFromPendingToPartial() {
        orderStatus.fillPartialOrder();
        assertEquals(OrderStatusEnum.PARTIAL, orderStatus.getStatus());
        assertFalse(orderStatus.isTerminal());
    }

    @Test
    @DisplayName("Should transition from PENDING to FILLED")
    public void shouldTransitionFromPendingToFilled() {
        orderStatus.completeOrder();
        assertEquals(OrderStatusEnum.FILLED, orderStatus.getStatus());
        assertTrue(orderStatus.isTerminal());
    }

    @Test
    @DisplayName("Should transition from PENDING to CANCELLED")
    public void shouldTransitionFromPendingToCancelled() {
        orderStatus.cancelOrder();
        assertEquals(OrderStatusEnum.CANCELLED, orderStatus.getStatus());
        assertTrue(orderStatus.isTerminal());
    }

    @Test
    @DisplayName("Should transition from PARTIAL to FILLED")
    public void shouldTransitionFromPartialToFilled() {
        orderStatus.fillPartialOrder(); // PENDING → PARTIAL
        orderStatus.completeOrder();    // PARTIAL → FILLED
        assertEquals(OrderStatusEnum.FILLED, orderStatus.getStatus());
        assertTrue(orderStatus.isTerminal());
    }

    @Test
    @DisplayName("Should transition from PARTIAL to CANCELLED")
    public void shouldTransitionFromPartialToCancelled() {
        orderStatus.fillPartialOrder(); // PENDING → PARTIAL
        orderStatus.cancelOrder();      // PARTIAL → CANCELLED
        assertEquals(OrderStatusEnum.CANCELLED, orderStatus.getStatus());
        assertTrue(orderStatus.isTerminal());
    }

    @Test
    @DisplayName("Should allow partial cancellation in PENDING state")
    public void shouldAllowPartialCancellationInPending() {
        assertDoesNotThrow(() -> orderStatus.cancelPartialOrder());
        assertEquals(OrderStatusEnum.PENDING, orderStatus.getStatus());
    }

    @Test
    @DisplayName("Should allow partial cancellation in PARTIAL state")
    public void shouldAllowPartialCancellationInPartial() {
        orderStatus.fillPartialOrder(); // PENDING → PARTIAL
        assertDoesNotThrow(() -> orderStatus.cancelPartialOrder());
        assertEquals(OrderStatusEnum.PARTIAL, orderStatus.getStatus());
    }

    @Test
    @DisplayName("Should throw exception when trying to modify FILLED order")
    public void shouldThrowExceptionWhenModifyingFilledOrder() {
        orderStatus.completeOrder(); // PENDING → FILLED

        assertThrows(IllegalStateException.class, () -> orderStatus.cancelOrder());
        assertThrows(IllegalStateException.class, () -> orderStatus.fillPartialOrder());
        assertThrows(IllegalStateException.class, () -> orderStatus.completeOrder());
        assertThrows(IllegalStateException.class, () -> orderStatus.cancelPartialOrder());
    }

    @Test
    @DisplayName("Should throw exception when trying to modify CANCELLED order")
    public void shouldThrowExceptionWhenModifyingCancelledOrder() {
        orderStatus.cancelOrder(); // PENDING → CANCELLED

        assertThrows(IllegalStateException.class, () -> orderStatus.fillPartialOrder());
        assertThrows(IllegalStateException.class, () -> orderStatus.completeOrder());
        assertThrows(IllegalStateException.class, () -> orderStatus.cancelOrder());
        assertThrows(IllegalStateException.class, () -> orderStatus.cancelPartialOrder());
    }

}