package core.ms.order;

import core.ms.order.domain.entities.*;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Order Domain Complete Test Suite")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderDomainTestSuite {

    @Nested
    @Order(1)
    @DisplayName("🏗️ Domain Entity Creation Tests")
    class DomainEntityCreationTests {

        @Test
        @DisplayName("Should create complete order lifecycle")
        void shouldCreateCompleteOrderLifecycle() {
            // Given - Create a buy order
            BuyOrder buyOrder = new BuyOrder(
                    "BUY_ORDER_001",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("1.0")
            );

            // And - Create a sell order
            SellOrder sellOrder = new SellOrder(
                    "SELL_ORDER_001",
                    Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD),
                    new BigDecimal("0.5")
            );

            // When - Orders are created
            // Then - Verify buy order
            assertThat(buyOrder.getId()).isEqualTo("BUY_ORDER_001");
            assertThat(buyOrder.getSymbol()).isEqualTo(Symbol.btcUsd());
            assertThat(buyOrder.getPrice()).isEqualTo(Money.of("45000.00", Currency.USD));
            assertThat(buyOrder.getQuantity()).isEqualTo(new BigDecimal("1.0"));
            assertThat(buyOrder.getStatus().getStatus()).isEqualTo(OrderStatusEnum.PENDING);
            assertThat(buyOrder.isActive()).isTrue();

            // And - Verify sell order
            assertThat(sellOrder.getId()).isEqualTo("SELL_ORDER_001");
            assertThat(sellOrder.getSymbol()).isEqualTo(Symbol.btcUsd());
            assertThat(sellOrder.getPrice()).isEqualTo(Money.of("44000.00", Currency.USD));
            assertThat(sellOrder.getQuantity()).isEqualTo(new BigDecimal("0.5"));
            assertThat(sellOrder.getStatus().getStatus()).isEqualTo(OrderStatusEnum.PENDING);
            assertThat(sellOrder.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should create transaction between orders")
        void shouldCreateTransactionBetweenOrders() {
            // Given - Create matching orders
            IBuyOrder buyOrder = new BuyOrder(
                    "BUY_ORDER_002",
                    Symbol.ethUsd(),
                    Money.of("3000.00", Currency.USD),
                    new BigDecimal("2.0")
            );

            ISellOrder sellOrder = new SellOrder(
                    "SELL_ORDER_002",
                    Symbol.ethUsd(),
                    Money.of("2950.00", Currency.USD),
                    new BigDecimal("1.5")
            );

            // When - Create transaction
            Money executionPrice = Money.of("2975.00", Currency.USD);
            BigDecimal transactionQuantity = new BigDecimal("1.5");

            Transaction transaction = new Transaction(
                    "TXN_001",
                    Symbol.ethUsd(),
                    buyOrder,
                    sellOrder,
                    executionPrice,
                    transactionQuantity
            );

            // Then - Verify transaction
            assertThat(transaction.getId()).isEqualTo("TXN_001");
            assertThat(transaction.getSymbol()).isEqualTo(Symbol.ethUsd());
            assertThat(transaction.getBuyOrder()).isEqualTo(buyOrder);
            assertThat(transaction.getSellOrder()).isEqualTo(sellOrder);
            assertThat(transaction.getPrice()).isEqualTo(executionPrice);
            assertThat(transaction.getQuantity()).isEqualTo(transactionQuantity);
            assertThat(transaction.getTotalValue()).isEqualTo(Money.of("4462.50", Currency.USD));
        }
    }

    @Nested
    @Order(2)
    @DisplayName("🔄 Order Lifecycle Management Tests")
    class OrderLifecycleTests {

        @Test
        @DisplayName("Should handle complete order execution lifecycle")
        void shouldHandleCompleteOrderExecutionLifecycle() {
            // Given - Create a buy order
            BuyOrder order = new BuyOrder(
                    "LIFECYCLE_ORDER_001",
                    Symbol.btcUsd(),
                    Money.of("50000.00", Currency.USD),
                    new BigDecimal("1.0")
            );

            // When & Then - Phase 1: Initial state
            assertThat(order.getStatus().getStatus()).isEqualTo(OrderStatusEnum.PENDING);
            assertThat(order.getExecutedQuantity()).isEqualTo(BigDecimal.ZERO);
            assertThat(order.getRemainingQuantity()).isEqualTo(new BigDecimal("1.0"));
            assertThat(order.isActive()).isTrue();

            // When & Then - Phase 2: First partial execution
            order.updateExecution(new BigDecimal("0.3"));
            assertThat(order.getStatus().getStatus()).isEqualTo(OrderStatusEnum.PARTIAL);
            assertThat(order.getExecutedQuantity()).isEqualTo(new BigDecimal("0.3"));
            assertThat(order.getRemainingQuantity()).isEqualTo(new BigDecimal("0.7"));
            assertThat(order.isActive()).isTrue();

            // When & Then - Phase 3: Second partial execution
            order.updateExecution(new BigDecimal("0.4"));
            assertThat(order.getStatus().getStatus()).isEqualTo(OrderStatusEnum.PARTIAL);
            assertThat(order.getExecutedQuantity()).isEqualTo(new BigDecimal("0.7"));
            assertThat(order.getRemainingQuantity()).isEqualTo(new BigDecimal("0.3"));
            assertThat(order.isActive()).isTrue();

            // When & Then - Phase 4: Final execution
            order.updateExecution(new BigDecimal("0.3"));
            assertThat(order.getStatus().getStatus()).isEqualTo(OrderStatusEnum.FILLED);
            assertThat(order.getExecutedQuantity()).isEqualTo(new BigDecimal("1.0"));
            assertThat(order.getRemainingQuantity()).isEqualTo(BigDecimal.ZERO);
            assertThat(order.isActive()).isFalse();
        }

        @Test
        @DisplayName("Should handle order cancellation lifecycle")
        void shouldHandleOrderCancellationLifecycle() {
            // Given - Create a sell order
            SellOrder order = new SellOrder(
                    "CANCEL_ORDER_001",
                    Symbol.ethUsd(),
                    Money.of("3000.00", Currency.USD),
                    new BigDecimal("2.0")
            );

            // When & Then - Phase 1: Partial execution
            order.updateExecution(new BigDecimal("0.8"));
            assertThat(order.getStatus().getStatus()).isEqualTo(OrderStatusEnum.PARTIAL);
            assertThat(order.getExecutedQuantity()).isEqualTo(new BigDecimal("0.8"));
            assertThat(order.getRemainingQuantity()).isEqualTo(new BigDecimal("1.2"));
            assertThat(order.isActive()).isTrue();

            // When & Then - Phase 2: Partial cancellation (should remain PARTIAL)
            order.cancelPartial();
            assertThat(order.getStatus().getStatus()).isEqualTo(OrderStatusEnum.PARTIAL);
            assertThat(order.isActive()).isTrue();

            // When & Then - Phase 3: Complete cancellation
            order.cancel();
            assertThat(order.getStatus().getStatus()).isEqualTo(OrderStatusEnum.CANCELLED);
            assertThat(order.isActive()).isFalse();
        }

        @Test
        @DisplayName("Should handle order price updates")
        void shouldHandleOrderPriceUpdates() {
            // Given - Create an order
            BuyOrder order = new BuyOrder(
                    "UPDATE_ORDER_001",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("0.5")
            );

            LocalDateTime initialUpdatedAt = order.getUpdatedAt();

            // When - Update price
            Money newPrice = Money.of("46000.00", Currency.USD);
            order.updatePrice(newPrice);

            // Then - Verify price update
            assertThat(order.getPrice()).isEqualTo(newPrice);
            assertThat(order.getUpdatedAt()).isAfter(initialUpdatedAt);
            assertThat(order.getStatus().getStatus()).isEqualTo(OrderStatusEnum.PENDING);
        }
    }

    @Nested
    @Order(3)
    @DisplayName("⚡ Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should calculate business values correctly")
        void shouldCalculateBusinessValuesCorrectly() {
            // Given - Create orders
            BuyOrder buyOrder = new BuyOrder(
                    "BUSINESS_BUY_001",
                    Symbol.btcUsd(),
                    Money.of("50000.00", Currency.USD),
                    new BigDecimal("0.5")
            );

            SellOrder sellOrder = new SellOrder(
                    "BUSINESS_SELL_001",
                    Symbol.ethUsd(),
                    Money.of("3000.00", Currency.USD),
                    new BigDecimal("2.0")
            );

            // When - Execute partial amounts
            buyOrder.updateExecution(new BigDecimal("0.3"));
            sellOrder.updateExecution(new BigDecimal("1.5"));

            // Then - Verify calculations
            assertThat(buyOrder.getTotalValue()).isEqualTo(Money.of("25000.00", Currency.USD));
            assertThat(buyOrder.getCostBasis()).isEqualTo(Money.of("15000.00", Currency.USD));

            assertThat(sellOrder.getTotalValue()).isEqualTo(Money.of("6000.00", Currency.USD));
            assertThat(sellOrder.getProceeds()).isEqualTo(Money.of("4500.00", Currency.USD));
        }

        @Test
        @DisplayName("Should handle order matching logic")
        void shouldHandleOrderMatchingLogic() {
            // Given - Create matching orders
            IBuyOrder buyOrder = new BuyOrder(
                    "MATCH_BUY_001",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("1.0")
            );

            ISellOrder sellOrder = new SellOrder(
                    "MATCH_SELL_001",
                    Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD),
                    new BigDecimal("0.8")
            );

            // When - Determine execution price
            Money executionPrice = AbstractTransaction.determineExecutionPrice(buyOrder, sellOrder);

            // Then - Verify optimal pricing
            assertThat(executionPrice).isEqualTo(Money.of("44500.00", Currency.USD));

            // When - Create transaction
            Transaction transaction = new Transaction(
                    "MATCH_TXN_001",
                    Symbol.btcUsd(),
                    buyOrder,
                    sellOrder,
                    executionPrice,
                    new BigDecimal("0.8")
            );

            // Then - Verify transaction
            assertThat(transaction.getTotalValue()).isEqualTo(Money.of("35600.00", Currency.USD));
        }

        @Test
        @DisplayName("Should validate domain constraints")
        void shouldValidateDomainConstraints() {
            // Given - Valid parameters
            Symbol symbol = Symbol.btcUsd();
            Money price = Money.of("45000.00", Currency.USD);
            BigDecimal quantity = new BigDecimal("0.5");

            // When - Create order (should succeed)
            BuyOrder order = new BuyOrder("CONSTRAINT_001", symbol, price, quantity);

            // Then - Verify creation
            assertThat(order).isNotNull();

            // When & Then - Test constraint violations
            assertThatThrownBy(() -> new BuyOrder(
                    "CONSTRAINT_002",
                    symbol,
                    Money.of("45000.00", Currency.EUR), // Wrong currency
                    quantity
            )).isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new BuyOrder(
                    "CONSTRAINT_003",
                    symbol,
                    price,
                    new BigDecimal("-0.1") // Negative quantity
            )).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @Order(4)
    @DisplayName("🛡️ Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle invalid order operations")
        void shouldHandleInvalidOrderOperations() {
            // Given - Create an order and fill it
            BuyOrder order = new BuyOrder(
                    "ERROR_ORDER_001",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("1.0")
            );
            order.updateExecution(new BigDecimal("1.0")); // Fill completely

            // When & Then - Test invalid operations on filled order
            assertThatThrownBy(() -> order.updateExecution(new BigDecimal("0.1")))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> order.updatePrice(Money.of("46000.00", Currency.USD)))
                    .isInstanceOf(IllegalStateException.class);

            assertThatThrownBy(() -> order.cancel())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Should handle invalid transaction creation")
        void shouldHandleInvalidTransactionCreation() {
            // Given - Create orders with different symbols
            IBuyOrder buyOrder = new BuyOrder(
                    "ERROR_BUY_001",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("1.0")
            );

            ISellOrder sellOrder = new SellOrder(
                    "ERROR_SELL_001",
                    Symbol.ethUsd(), // Different symbol
                    Money.of("3000.00", Currency.USD),
                    new BigDecimal("1.0")
            );

            // When & Then - Test invalid transaction creation
            assertThatThrownBy(() -> new Transaction(
                    "ERROR_TXN_001",
                    Symbol.btcUsd(),
                    buyOrder,
                    sellOrder,
                    Money.of("44000.00", Currency.USD),
                    new BigDecimal("1.0")
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("All orders must have the same symbol");
        }

        @Test
        @DisplayName("Should handle edge cases in execution")
        void shouldHandleEdgeCasesInExecution() {
            // Given - Create an order
            BuyOrder order = new BuyOrder(
                    "EDGE_ORDER_001",
                    Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD),
                    new BigDecimal("1.0")
            );

            // When & Then - Test edge cases
            assertThatThrownBy(() -> order.updateExecution(BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Executed amount must be positive");

            assertThatThrownBy(() -> order.updateExecution(new BigDecimal("1.1")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Executed amount exceeds remaining quantity");

            assertThatThrownBy(() -> order.updateExecution(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Executed amount cannot be null");
        }
    }

    @Nested
    @Order(5)
    @DisplayName("🧪 Integration Scenarios Tests")
    class IntegrationScenariosTests {

        @Test
        @DisplayName("Should handle complete trading scenario")
        void shouldHandleCompleteTradingScenario() {
            // Given - Market setup
            BuyOrder buyOrder1 = new BuyOrder("BUY_001", Symbol.btcUsd(), Money.of("45000.00", Currency.USD), new BigDecimal("1.0"));
            BuyOrder buyOrder2 = new BuyOrder("BUY_002", Symbol.btcUsd(), Money.of("44500.00", Currency.USD), new BigDecimal("0.5"));

            SellOrder sellOrder1 = new SellOrder("SELL_001", Symbol.btcUsd(), Money.of("44000.00", Currency.USD), new BigDecimal("0.8"));
            SellOrder sellOrder2 = new SellOrder("SELL_002", Symbol.btcUsd(), Money.of("43500.00", Currency.USD), new BigDecimal("1.2"));

            // When - Execute transactions
            Transaction txn1 = new Transaction("TXN_001", Symbol.btcUsd(), buyOrder1, sellOrder1,
                    Money.of("44500.00", Currency.USD), new BigDecimal("0.8"));

            Transaction txn2 = new Transaction("TXN_002", Symbol.btcUsd(), buyOrder2, sellOrder2,
                    Money.of("44000.00", Currency.USD), new BigDecimal("0.5"));

            // Then - Verify scenario results
            assertThat(txn1.getTotalValue()).isEqualTo(Money.of("35600.00", Currency.USD));
            assertThat(txn2.getTotalValue()).isEqualTo(Money.of("22000.00", Currency.USD));

            // Verify orders have correct symbols
            assertThat(buyOrder1.getSymbolCode()).isEqualTo("BTC");
            assertThat(sellOrder1.getSymbolCode()).isEqualTo("BTC");
        }

        @Test
        @DisplayName("Should handle multi-currency scenarios")
        void shouldHandleMultiCurrencyScenarios() {
            // Given - Orders in different symbols
            BuyOrder btcOrder = new BuyOrder("BTC_BUY_001", Symbol.btcUsd(), Money.of("45000.00", Currency.USD), new BigDecimal("1.0"));
            SellOrder ethOrder = new SellOrder("ETH_SELL_001", Symbol.ethUsd(), Money.of("3000.00", Currency.USD), new BigDecimal("2.0"));
            BuyOrder forexOrder = new BuyOrder("FOREX_BUY_001", Symbol.eurUsd(), Money.of("1.10", Currency.USD), new BigDecimal("1000.0"));

            // When & Then - Verify different symbols work
            assertThat(btcOrder.getSymbol()).isEqualTo(Symbol.btcUsd());
            assertThat(ethOrder.getSymbol()).isEqualTo(Symbol.ethUsd());
            assertThat(forexOrder.getSymbol()).isEqualTo(Symbol.eurUsd());

            // Verify calculations
            assertThat(btcOrder.getTotalValue()).isEqualTo(Money.of("45000.00", Currency.USD));
            assertThat(ethOrder.getTotalValue()).isEqualTo(Money.of("6000.00", Currency.USD));
            assertThat(forexOrder.getTotalValue()).isEqualTo(Money.of("1100.00", Currency.USD));
        }

        @Test
        @DisplayName("Should handle timestamp consistency")
        void shouldHandleTimestampConsistency() {
            // Given - Record time before creation
            LocalDateTime beforeCreation = LocalDateTime.now();

            // When - Create order and transaction
            BuyOrder order = new BuyOrder("TIME_BUY_001", Symbol.btcUsd(), Money.of("45000.00", Currency.USD), new BigDecimal("1.0"));
            SellOrder sellOrder = new SellOrder("TIME_SELL_001", Symbol.btcUsd(), Money.of("44000.00", Currency.USD), new BigDecimal("1.0"));

            Transaction transaction = new Transaction("TIME_TXN_001", Symbol.btcUsd(), order, sellOrder,
                    Money.of("44500.00", Currency.USD), new BigDecimal("1.0"));

            LocalDateTime afterCreation = LocalDateTime.now();

            // Then - Verify timestamps
            assertThat(order.getCreatedAt()).isAfter(beforeCreation);
            assertThat(order.getCreatedAt()).isBefore(afterCreation);
            assertThat(order.getUpdatedAt()).isAfter(beforeCreation);
            assertThat(order.getUpdatedAt()).isBefore(afterCreation);

            assertThat(transaction.getCreatedAt()).isAfter(beforeCreation);
            assertThat(transaction.getCreatedAt()).isBefore(afterCreation);
        }
    }
}