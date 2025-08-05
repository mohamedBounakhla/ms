package core.ms.order;

import core.ms.order.domain.entities.BuyOrder;
import core.ms.order.domain.entities.SellOrder;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DisplayName("Order Domain Unit Tests")
class OrderDomainUnitTest {

    @Nested
    @DisplayName("BuyOrder Tests")
    class BuyOrderTests {

        @Test
        @DisplayName("Should create a valid BuyOrder")
        void shouldCreateValidBuyOrder() {
            // Given
            String orderId = "ORDER_123";
            Symbol symbol = Symbol.btcUsd();
            Money price = Money.of("45000.00", Currency.USD);
            BigDecimal quantity = new BigDecimal("0.1");

            // When
            BuyOrder buyOrder = new BuyOrder(orderId, symbol, price, quantity);

            // Then
            assertThat(buyOrder.getId()).isEqualTo(orderId);
            assertThat(buyOrder.getSymbol()).isEqualTo(symbol);
            assertThat(buyOrder.getPrice()).isEqualTo(price);
            assertThat(buyOrder.getQuantity()).isEqualTo(quantity);
            assertThat(buyOrder.getStatus().getStatus()).isEqualTo(OrderStatusEnum.PENDING);
            assertThat(buyOrder.getExecutedQuantity()).isEqualTo(BigDecimal.ZERO);
            assertThat(buyOrder.getRemainingQuantity()).isEqualTo(quantity);
            assertThat(buyOrder.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should calculate cost basis correctly")
        void shouldCalculateCostBasis() {
            // Given
            BuyOrder buyOrder = new BuyOrder(
                    "ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.1")
            );

            // When
            buyOrder.updateExecution(new BigDecimal("0.05"));

            // Then
            Money expectedCostBasis = Money.of("2250.00", Currency.USD); // 45000 * 0.05
            assertThat(buyOrder.getCostBasis()).isEqualTo(expectedCostBasis);
        }

        @Test
        @DisplayName("Should throw exception for invalid price currency")
        void shouldThrowExceptionForInvalidPriceCurrency() {
            // Given
            Symbol btcUsd = Symbol.btcUsd();
            Money eurPrice = Money.of("40000.00", Currency.EUR); // Wrong currency

            // When & Then
            assertThatThrownBy(() -> new BuyOrder("ORDER_123", btcUsd, eurPrice, new BigDecimal("0.1")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Price currency EUR does not match symbol quote currency USD");
        }

        @Test
        @DisplayName("Should throw exception for negative quantity")
        void shouldThrowExceptionForNegativeQuantity() {
            // Given
            Symbol symbol = Symbol.btcUsd();
            Money price = Money.of("45000.00", Currency.USD);
            BigDecimal negativeQuantity = new BigDecimal("-0.1");

            // When & Then
            assertThatThrownBy(() -> new BuyOrder("ORDER_123", symbol, price, negativeQuantity))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quantity must be positive");
        }
    }

    @Nested
    @DisplayName("SellOrder Tests")
    class SellOrderTests {

        @Test
        @DisplayName("Should create a valid SellOrder")
        void shouldCreateValidSellOrder() {
            // Given
            String orderId = "ORDER_456";
            Symbol symbol = Symbol.ethUsd();
            Money price = Money.of("3000.00", Currency.USD);
            BigDecimal quantity = new BigDecimal("1.5");

            // When
            SellOrder sellOrder = new SellOrder(orderId, symbol, price, quantity);

            // Then
            assertThat(sellOrder.getId()).isEqualTo(orderId);
            assertThat(sellOrder.getSymbol()).isEqualTo(symbol);
            assertThat(sellOrder.getPrice()).isEqualTo(price);
            assertThat(sellOrder.getQuantity()).isEqualTo(quantity);
            assertThat(sellOrder.getStatus().getStatus()).isEqualTo(OrderStatusEnum.PENDING);
            assertThat(sellOrder.getExecutedQuantity()).isEqualTo(BigDecimal.ZERO);
            assertThat(sellOrder.getRemainingQuantity()).isEqualTo(quantity);
            assertThat(sellOrder.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should calculate proceeds correctly")
        void shouldCalculateProceeds() {
            // Given
            SellOrder sellOrder = new SellOrder(
                    "ORDER_456",
                    Symbol.ethUsd(),
                    Money.of("3000.00", Currency.USD),
                    new BigDecimal("1.5")
            );

            // When
            sellOrder.updateExecution(new BigDecimal("0.5"));

            // Then
            Money expectedProceeds = Money.of("1500.00", Currency.USD); // 3000 * 0.5
            assertThat(sellOrder.getProceeds()).isEqualTo(expectedProceeds);
        }
    }

    @Nested
    @DisplayName("Order Status Management Tests")
    class OrderStatusTests {

        @Test
        @DisplayName("Should update order status through execution")
        void shouldUpdateOrderStatusThroughExecution() {
            // Given
            BuyOrder order = new BuyOrder(
                    "ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("1.0")
            );

            // When & Then - Initial state
            assertThat(order.getStatus().getStatus()).isEqualTo(OrderStatusEnum.PENDING);
            assertThat(order.isActive()).isTrue();

            // When - Partial execution
            order.updateExecution(new BigDecimal("0.5"));

            // Then - Should be partially filled
            assertThat(order.getStatus().getStatus()).isEqualTo(OrderStatusEnum.PARTIAL);
            assertThat(order.getExecutedQuantity()).isEqualTo(new BigDecimal("0.5"));
            assertThat(order.getRemainingQuantity()).isEqualTo(new BigDecimal("0.5"));
            assertThat(order.isActive()).isTrue();

            // When - Complete execution
            order.updateExecution(new BigDecimal("0.5"));

            // Then - Should be fully filled
            assertThat(order.getStatus().getStatus()).isEqualTo(OrderStatusEnum.FILLED);
            assertThat(order.getExecutedQuantity()).isEqualTo(new BigDecimal("1.0"));
            assertThat(order.getRemainingQuantity()).isEqualTo(BigDecimal.ZERO);
            assertThat(order.isActive()).isFalse();
        }

        @Test
        @DisplayName("Should cancel order correctly")
        void shouldCancelOrder() {
            // Given
            BuyOrder order = new BuyOrder(
                    "ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("1.0")
            );

            // When
            order.cancel();

            // Then
            assertThat(order.getStatus().getStatus()).isEqualTo(OrderStatusEnum.CANCELLED);
            assertThat(order.isActive()).isFalse();
        }

        @Test
        @DisplayName("Should not allow execution after cancellation")
        void shouldNotAllowExecutionAfterCancellation() {
            // Given
            BuyOrder order = new BuyOrder(
                    "ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("1.0")
            );
            order.cancel();

            // When & Then
            assertThatThrownBy(() -> order.updateExecution(new BigDecimal("0.5")))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Should update price for active order")
        void shouldUpdatePriceForActiveOrder() {
            // Given
            BuyOrder order = new BuyOrder(
                    "ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("1.0")
            );
            Money newPrice = Money.of("46000.00", Currency.USD);

            // When
            order.updatePrice(newPrice);

            // Then
            assertThat(order.getPrice()).isEqualTo(newPrice);
        }

        @Test
        @DisplayName("Should not allow price update for cancelled order")
        void shouldNotAllowPriceUpdateForCancelledOrder() {
            // Given
            BuyOrder order = new BuyOrder(
                    "ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("1.0")
            );
            order.cancel();
            Money newPrice = Money.of("46000.00", Currency.USD);

            // When & Then
            assertThatThrownBy(() -> order.updatePrice(newPrice))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot update price of terminal order");
        }
    }

    @Nested
    @DisplayName("Order Execution Logic Tests")
    class OrderExecutionTests {

        @Test
        @DisplayName("Should not allow execution exceeding remaining quantity")
        void shouldNotAllowExecutionExceedingRemainingQuantity() {
            // Given
            BuyOrder order = new BuyOrder(
                    "ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("1.0")
            );

            // When & Then
            assertThatThrownBy(() -> order.updateExecution(new BigDecimal("1.5")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Executed amount exceeds remaining quantity");
        }

        @Test
        @DisplayName("Should handle multiple partial executions")
        void shouldHandleMultiplePartialExecutions() {
            // Given
            BuyOrder order = new BuyOrder(
                    "ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("1.0")
            );

            // When - First execution
            order.updateExecution(new BigDecimal("0.3"));

            // Then
            assertThat(order.getExecutedQuantity()).isEqualTo(new BigDecimal("0.3"));
            assertThat(order.getRemainingQuantity()).isEqualTo(new BigDecimal("0.7"));
            assertThat(order.getStatus().getStatus()).isEqualTo(OrderStatusEnum.PARTIAL);

            // When - Second execution
            order.updateExecution(new BigDecimal("0.4"));

            // Then
            assertThat(order.getExecutedQuantity()).isEqualTo(new BigDecimal("0.7"));
            assertThat(order.getRemainingQuantity()).isEqualTo(new BigDecimal("0.3"));
            assertThat(order.getStatus().getStatus()).isEqualTo(OrderStatusEnum.PARTIAL);

            // When - Final execution
            order.updateExecution(new BigDecimal("0.3"));

            // Then
            assertThat(order.getExecutedQuantity()).isEqualTo(new BigDecimal("1.0"));
            assertThat(order.getRemainingQuantity()).isEqualTo(BigDecimal.ZERO);
            assertThat(order.getStatus().getStatus()).isEqualTo(OrderStatusEnum.FILLED);
        }

        @Test
        @DisplayName("Should calculate total value correctly")
        void shouldCalculateTotalValueCorrectly() {
            // Given
            BuyOrder order = new BuyOrder(
                    "ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.5")
            );

            // When
            Money totalValue = order.getTotalValue();

            // Then
            Money expectedValue = Money.of("22500.00", Currency.USD); // 45000 * 0.5
            assertThat(totalValue).isEqualTo(expectedValue);
        }
    }

    @Nested
    @DisplayName("Order Business Logic Tests")
    class OrderBusinessLogicTests {

        @Test
        @DisplayName("Should correctly identify order symbols")
        void shouldCorrectlyIdentifyOrderSymbols() {
            // Given
            BuyOrder btcOrder = new BuyOrder(
                    "ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.1")
            );

            SellOrder ethOrder = new SellOrder(
                    "ORDER_456",
                    Symbol.ethUsd(),
                    Money.of("3000.00", Currency.USD),
                    new BigDecimal("1.5")
            );

            // When & Then
            assertThat(btcOrder.getSymbolCode()).isEqualTo("BTC");
            assertThat(ethOrder.getSymbolCode()).isEqualTo("ETH");
        }

        @Test
        @DisplayName("Should maintain order timestamps")
        void shouldMaintainOrderTimestamps() {
            // Given
            LocalDateTime beforeCreation = LocalDateTime.now();

            // When
            BuyOrder order = new BuyOrder(
                    "ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.1")
            );

            LocalDateTime afterCreation = LocalDateTime.now();

            // Then
            assertThat(order.getCreatedAt()).isAfter(beforeCreation);
            assertThat(order.getCreatedAt()).isBefore(afterCreation);
            assertThat(order.getUpdatedAt()).isAfter(beforeCreation);
            assertThat(order.getUpdatedAt()).isBefore(afterCreation);
        }

        @Test
        @DisplayName("Should update timestamp on order modifications")
        void shouldUpdateTimestampOnOrderModifications() {
            // Given
            BuyOrder order = new BuyOrder(
                    "ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("1.0")
            );
            LocalDateTime initialUpdatedAt = order.getUpdatedAt();

            // Wait a bit to ensure timestamp difference
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When
            order.updateExecution(new BigDecimal("0.5"));

            // Then
            assertThat(order.getUpdatedAt()).isAfter(initialUpdatedAt);
        }

        @Test
        @DisplayName("Should handle order equality correctly")
        void shouldHandleOrderEqualityCorrectly() {
            // Given
            BuyOrder order1 = new BuyOrder(
                    "ORDER_123",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.1")
            );

            BuyOrder order2 = new BuyOrder(
                    "ORDER_123", // Same ID
                    Symbol.ethUsd(),
                    Money.of("3000.00", Currency.USD),
                    new BigDecimal("1.0")
            );

            BuyOrder order3 = new BuyOrder(
                    "ORDER_456", // Different ID
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.1")
            );

            // When & Then
            assertThat(order1).isEqualTo(order2); // Same ID
            assertThat(order1).isNotEqualTo(order3); // Different ID
            assertThat(order1.hashCode()).isEqualTo(order2.hashCode());
        }
    }
}