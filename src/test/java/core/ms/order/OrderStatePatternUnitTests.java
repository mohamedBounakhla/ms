package core.ms.order;

import core.ms.order.domain.value_objects.OrderStatus;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Order State Pattern Unit Tests")
class OrderStateUnitTest {

    @Nested
    @DisplayName("Order Status Transitions Tests")
    class OrderStatusTransitionsTests {

        @Test
        @DisplayName("Should start in PENDING state")
        void shouldStartInPendingState() {
            // Given & When
            OrderStatus orderStatus = new OrderStatus();

            // Then
            assertThat(orderStatus.getStatus()).isEqualTo(OrderStatusEnum.PENDING);
            assertThat(orderStatus.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("Should transition from PENDING to PARTIAL")
        void shouldTransitionFromPendingToPartial() {
            // Given
            OrderStatus orderStatus = new OrderStatus();

            // When
            orderStatus.fillPartialOrder();

            // Then
            assertThat(orderStatus.getStatus()).isEqualTo(OrderStatusEnum.PARTIAL);
            assertThat(orderStatus.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("Should transition from PENDING to FILLED")
        void shouldTransitionFromPendingToFilled() {
            // Given
            OrderStatus orderStatus = new OrderStatus();

            // When
            orderStatus.completeOrder();

            // Then
            assertThat(orderStatus.getStatus()).isEqualTo(OrderStatusEnum.FILLED);
            assertThat(orderStatus.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("Should transition from PENDING to CANCELLED")
        void shouldTransitionFromPendingToCancelled() {
            // Given
            OrderStatus orderStatus = new OrderStatus();

            // When
            orderStatus.cancelOrder();

            // Then
            assertThat(orderStatus.getStatus()).isEqualTo(OrderStatusEnum.CANCELLED);
            assertThat(orderStatus.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("Should transition from PARTIAL to FILLED")
        void shouldTransitionFromPartialToFilled() {
            // Given
            OrderStatus orderStatus = new OrderStatus();
            orderStatus.fillPartialOrder();

            // When
            orderStatus.completeOrder();

            // Then
            assertThat(orderStatus.getStatus()).isEqualTo(OrderStatusEnum.FILLED);
            assertThat(orderStatus.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("Should transition from PARTIAL to CANCELLED")
        void shouldTransitionFromPartialToCancelled() {
            // Given
            OrderStatus orderStatus = new OrderStatus();
            orderStatus.fillPartialOrder();

            // When
            orderStatus.cancelOrder();

            // Then
            assertThat(orderStatus.getStatus()).isEqualTo(OrderStatusEnum.CANCELLED);
            assertThat(orderStatus.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("Should allow partial cancellation in PENDING state")
        void shouldAllowPartialCancellationInPendingState() {
            // Given
            OrderStatus orderStatus = new OrderStatus();

            // When
            orderStatus.cancelPartialOrder();

            // Then - Should remain in PENDING state
            assertThat(orderStatus.getStatus()).isEqualTo(OrderStatusEnum.PENDING);
            assertThat(orderStatus.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("Should allow partial cancellation in PARTIAL state")
        void shouldAllowPartialCancellationInPartialState() {
            // Given
            OrderStatus orderStatus = new OrderStatus();
            orderStatus.fillPartialOrder();

            // When
            orderStatus.cancelPartialOrder();

            // Then - Should remain in PARTIAL state
            assertThat(orderStatus.getStatus()).isEqualTo(OrderStatusEnum.PARTIAL);
            assertThat(orderStatus.isTerminal()).isFalse();
        }
    }

    @Nested
    @DisplayName("Terminal State Restrictions Tests")
    class TerminalStateRestrictionsTests {

        @Test
        @DisplayName("Should not allow cancellation of filled order")
        void shouldNotAllowCancellationOfFilledOrder() {
            // Given
            OrderStatus orderStatus = new OrderStatus();
            orderStatus.completeOrder();

            // When & Then
            assertThatThrownBy(() -> orderStatus.cancelOrder())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel a filled order");
        }

        @Test
        @DisplayName("Should not allow partial cancellation of filled order")
        void shouldNotAllowPartialCancellationOfFilledOrder() {
            // Given
            OrderStatus orderStatus = new OrderStatus();
            orderStatus.completeOrder();

            // When & Then
            assertThatThrownBy(() -> orderStatus.cancelPartialOrder())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel a filled order");
        }

        @Test
        @DisplayName("Should not allow partial fill of filled order")
        void shouldNotAllowPartialFillOfFilledOrder() {
            // Given
            OrderStatus orderStatus = new OrderStatus();
            orderStatus.completeOrder();

            // When & Then
            assertThatThrownBy(() -> orderStatus.fillPartialOrder())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Order is already filled");
        }

        @Test
        @DisplayName("Should not allow completion of filled order")
        void shouldNotAllowCompletionOfFilledOrder() {
            // Given
            OrderStatus orderStatus = new OrderStatus();
            orderStatus.completeOrder();

            // When & Then
            assertThatThrownBy(() -> orderStatus.completeOrder())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Order is already filled");
        }

        @Test
        @DisplayName("Should not allow cancellation of cancelled order")
        void shouldNotAllowCancellationOfCancelledOrder() {
            // Given
            OrderStatus orderStatus = new OrderStatus();
            orderStatus.cancelOrder();

            // When & Then
            assertThatThrownBy(() -> orderStatus.cancelOrder())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Order is already cancelled");
        }

        @Test
        @DisplayName("Should not allow partial cancellation of cancelled order")
        void shouldNotAllowPartialCancellationOfCancelledOrder() {
            // Given
            OrderStatus orderStatus = new OrderStatus();
            orderStatus.cancelOrder();

            // When & Then
            assertThatThrownBy(() -> orderStatus.cancelPartialOrder())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Order is already cancelled");
        }

        @Test
        @DisplayName("Should not allow partial fill of cancelled order")
        void shouldNotAllowPartialFillOfCancelledOrder() {
            // Given
            OrderStatus orderStatus = new OrderStatus();
            orderStatus.cancelOrder();

            // When & Then
            assertThatThrownBy(() -> orderStatus.fillPartialOrder())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot fill a cancelled order");
        }

        @Test
        @DisplayName("Should not allow completion of cancelled order")
        void shouldNotAllowCompletionOfCancelledOrder() {
            // Given
            OrderStatus orderStatus = new OrderStatus();
            orderStatus.cancelOrder();

            // When & Then
            assertThatThrownBy(() -> orderStatus.completeOrder())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot complete a cancelled order");
        }
    }

    @Nested
    @DisplayName("State Pattern Implementation Tests")
    class StatePatternImplementationTests {

        @Test
        @DisplayName("Should have correct string representation")
        void shouldHaveCorrectStringRepresentation() {
            // Given
            OrderStatus orderStatus = new OrderStatus();

            // When & Then
            assertThat(orderStatus.toString()).isEqualTo("PENDING");

            // When - Change to PARTIAL
            orderStatus.fillPartialOrder();
            // Then
            assertThat(orderStatus.toString()).isEqualTo("PARTIAL");

            // When - Change to FILLED
            orderStatus.completeOrder();
            // Then
            assertThat(orderStatus.toString()).isEqualTo("FILLED");
        }

        @Test
        @DisplayName("Should maintain state consistency after transitions")
        void shouldMaintainStateConsistencyAfterTransitions() {
            // Given
            OrderStatus orderStatus = new OrderStatus();

            // When - Multiple transitions
            orderStatus.fillPartialOrder();
            orderStatus.cancelPartialOrder(); // Should remain in PARTIAL
            orderStatus.completeOrder();

            // Then
            assertThat(orderStatus.getStatus()).isEqualTo(OrderStatusEnum.FILLED);
            assertThat(orderStatus.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("Should handle state transitions in complex scenarios")
        void shouldHandleStateTransitionsInComplexScenarios() {
            // Given
            OrderStatus orderStatus = new OrderStatus();

            // When - Start with partial fill
            orderStatus.fillPartialOrder();
            assertThat(orderStatus.getStatus()).isEqualTo(OrderStatusEnum.PARTIAL);

            // When - Try partial cancellation (should remain PARTIAL)
            orderStatus.cancelPartialOrder();
            assertThat(orderStatus.getStatus()).isEqualTo(OrderStatusEnum.PARTIAL);

            // When - Cancel completely
            orderStatus.cancelOrder();

            // Then - Should be CANCELLED
            assertThat(orderStatus.getStatus()).isEqualTo(OrderStatusEnum.CANCELLED);
            assertThat(orderStatus.isTerminal()).isTrue();
        }
    }

    @Nested
    @DisplayName("Order Status Enum Tests")
    class OrderStatusEnumTests {

        @Test
        @DisplayName("Should have all required status values")
        void shouldHaveAllRequiredStatusValues() {
            // Given & When
            OrderStatusEnum[] statuses = OrderStatusEnum.values();

            // Then
            assertThat(statuses).hasSize(4);
            assertThat(statuses).contains(
                    OrderStatusEnum.PENDING,
                    OrderStatusEnum.PARTIAL,
                    OrderStatusEnum.FILLED,
                    OrderStatusEnum.CANCELLED
            );
        }

        @Test
        @DisplayName("Should maintain enum value consistency")
        void shouldMaintainEnumValueConsistency() {
            // Given & When & Then
            assertThat(OrderStatusEnum.valueOf("PENDING")).isEqualTo(OrderStatusEnum.PENDING);
            assertThat(OrderStatusEnum.valueOf("PARTIAL")).isEqualTo(OrderStatusEnum.PARTIAL);
            assertThat(OrderStatusEnum.valueOf("FILLED")).isEqualTo(OrderStatusEnum.FILLED);
            assertThat(OrderStatusEnum.valueOf("CANCELLED")).isEqualTo(OrderStatusEnum.CANCELLED);
        }

        @Test
        @DisplayName("Should handle enum serialization")
        void shouldHandleEnumSerialization() {
            // Given & When & Then
            assertThat(OrderStatusEnum.PENDING.name()).isEqualTo("PENDING");
            assertThat(OrderStatusEnum.PARTIAL.name()).isEqualTo("PARTIAL");
            assertThat(OrderStatusEnum.FILLED.name()).isEqualTo("FILLED");
            assertThat(OrderStatusEnum.CANCELLED.name()).isEqualTo("CANCELLED");
        }
    }
}
