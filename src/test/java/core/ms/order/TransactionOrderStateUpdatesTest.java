package core.ms.order;

import core.ms.order.domain.entities.*;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("1. Transaction Order State Updates Tests")
class TransactionOrderStateUpdatesTest {

    private Symbol btcUsd;
    private Money price;

    @BeforeEach
    void setUp() {
        btcUsd = Symbol.btcUsd();
        price = Money.of("45000", Currency.USD);
    }

    @Test
    @DisplayName("Should update orders to FILLED when transaction quantity equals order quantity")
    void shouldUpdateOrdersToFilledWhenTransactionQuantityEqualsOrderQuantity() {
        // Given: Orders with 1.0 BTC quantity
        BigDecimal orderQuantity = new BigDecimal("1.0");
        IBuyOrder buyOrder = new BuyOrder("buy-1", btcUsd, price, orderQuantity);
        ISellOrder sellOrder = new SellOrder("sell-1", btcUsd, price, orderQuantity);

        assertEquals(OrderStatusEnum.PENDING, buyOrder.getStatus().getStatus());
        assertEquals(OrderStatusEnum.PENDING, sellOrder.getStatus().getStatus());

        // When: Transaction created with full quantity (1.0 BTC)
        Transaction transaction = new Transaction("tx-1", btcUsd, buyOrder, sellOrder, price, orderQuantity);

        // Then: Orders should be FILLED
        assertEquals(OrderStatusEnum.FILLED, buyOrder.getStatus().getStatus());
        assertEquals(OrderStatusEnum.FILLED, sellOrder.getStatus().getStatus());
    }

    @Test
    @DisplayName("Should update orders to PARTIAL when transaction quantity is less than order quantity")
    void shouldUpdateOrdersToPartialWhenTransactionQuantityIsLessThanOrderQuantity() {
        // Given: Orders with 2.0 BTC quantity
        BigDecimal orderQuantity = new BigDecimal("2.0");
        BigDecimal transactionQuantity = new BigDecimal("0.5");

        IBuyOrder buyOrder = new BuyOrder("buy-1", btcUsd, price, orderQuantity);
        ISellOrder sellOrder = new SellOrder("sell-1", btcUsd, price, orderQuantity);

        // When: Transaction created with partial quantity (0.5 BTC)
        Transaction transaction = new Transaction("tx-1", btcUsd, buyOrder, sellOrder, price, transactionQuantity);

        // Then: Orders should be PARTIAL
        assertEquals(OrderStatusEnum.PARTIAL, buyOrder.getStatus().getStatus());
        assertEquals(OrderStatusEnum.PARTIAL, sellOrder.getStatus().getStatus());
    }

    @Test
    @DisplayName("Should update PARTIAL orders to FILLED when remaining quantity is fully executed")
    void shouldUpdatePartialOrdersToFilledWhenRemainingQuantityIsFullyExecuted() {
        // Given: Orders with 2.0 BTC quantity
        BigDecimal orderQuantity = new BigDecimal("2.0");

        IBuyOrder buyOrder = new BuyOrder("buy-1", btcUsd, price, orderQuantity);
        ISellOrder sellOrder = new SellOrder("sell-1", btcUsd, price, orderQuantity);

        // ===== FIX: PROPERLY SIMULATE PARTIAL EXECUTION =====
        // First transaction: Execute 0.5 BTC to make orders PARTIAL
        BigDecimal firstTransactionQuantity = new BigDecimal("0.5");
        Transaction firstTransaction = new Transaction("tx-1", btcUsd, buyOrder, sellOrder, price, firstTransactionQuantity);

        // Verify orders are now PARTIAL with 1.5 BTC remaining
        assertEquals(OrderStatusEnum.PARTIAL, buyOrder.getStatus().getStatus());
        assertEquals(OrderStatusEnum.PARTIAL, sellOrder.getStatus().getStatus());
        assertEquals(new BigDecimal("0.5"), buyOrder.getExecutedQuantity());
        assertEquals(new BigDecimal("1.5"), buyOrder.getRemainingQuantity());

        // When: Second transaction executes the remaining 1.5 BTC
        BigDecimal remainingQuantity = new BigDecimal("1.5");
        Transaction secondTransaction = new Transaction("tx-2", btcUsd, buyOrder, sellOrder, price, remainingQuantity);

        // Then: Orders should now be FILLED
        assertEquals(OrderStatusEnum.FILLED, buyOrder.getStatus().getStatus());
        assertEquals(OrderStatusEnum.FILLED, sellOrder.getStatus().getStatus());
        assertEquals(orderQuantity, buyOrder.getExecutedQuantity()); // 2.0 total
        assertEquals(BigDecimal.ZERO, buyOrder.getRemainingQuantity()); // 0 remaining
    }
    @Test
    @DisplayName("Should track order state transitions through multiple transactions")
    void shouldTrackOrderStateTransitionsThroughMultipleTransactions() {
        // Given: Orders with 3.0 BTC quantity
        BigDecimal orderQuantity = new BigDecimal("3.0");

        IBuyOrder buyOrder = new BuyOrder("buy-1", btcUsd, price, orderQuantity);
        ISellOrder sellOrder = new SellOrder("sell-1", btcUsd, price, orderQuantity);

        // Initial state: PENDING
        assertEquals(OrderStatusEnum.PENDING, buyOrder.getStatus().getStatus());
        assertEquals(BigDecimal.ZERO, buyOrder.getExecutedQuantity());
        assertEquals(orderQuantity, buyOrder.getRemainingQuantity());

        // Transaction 1: Execute 1.0 BTC (PENDING → PARTIAL)
        Transaction tx1 = new Transaction("tx-1", btcUsd, buyOrder, sellOrder, price, new BigDecimal("1.0"));
        assertEquals(OrderStatusEnum.PARTIAL, buyOrder.getStatus().getStatus());
        assertEquals(new BigDecimal("1.0"), buyOrder.getExecutedQuantity());
        assertEquals(new BigDecimal("2.0"), buyOrder.getRemainingQuantity());

        // Transaction 2: Execute 1.0 BTC (PARTIAL → PARTIAL)
        Transaction tx2 = new Transaction("tx-2", btcUsd, buyOrder, sellOrder, price, new BigDecimal("1.0"));
        assertEquals(OrderStatusEnum.PARTIAL, buyOrder.getStatus().getStatus());
        assertEquals(new BigDecimal("2.0"), buyOrder.getExecutedQuantity());
        assertEquals(new BigDecimal("1.0"), buyOrder.getRemainingQuantity());

        // Transaction 3: Execute remaining 1.0 BTC (PARTIAL → FILLED)
        Transaction tx3 = new Transaction("tx-3", btcUsd, buyOrder, sellOrder, price, new BigDecimal("1.0"));
        assertEquals(OrderStatusEnum.FILLED, buyOrder.getStatus().getStatus());
        assertEquals(new BigDecimal("3.0"), buyOrder.getExecutedQuantity());
        assertEquals(BigDecimal.ZERO, buyOrder.getRemainingQuantity());
    }
    @Test
    @DisplayName("Should handle exact remaining quantity execution")
    void shouldHandleExactRemainingQuantityExecution() {
        // Given: Order with 1.0 BTC
        BigDecimal orderQuantity = new BigDecimal("1.0");

        IBuyOrder buyOrder = new BuyOrder("buy-1", btcUsd, price, orderQuantity);
        ISellOrder sellOrder = new SellOrder("sell-1", btcUsd, price, orderQuantity);

        // First: Execute 0.3 BTC (PENDING → PARTIAL)
        Transaction tx1 = new Transaction("tx-1", btcUsd, buyOrder, sellOrder, price, new BigDecimal("0.3"));
        assertEquals(OrderStatusEnum.PARTIAL, buyOrder.getStatus().getStatus());
        assertEquals(new BigDecimal("0.7"), buyOrder.getRemainingQuantity());

        // Then: Execute exactly the remaining 0.7 BTC (PARTIAL → FILLED)
        Transaction tx2 = new Transaction("tx-2", btcUsd, buyOrder, sellOrder, price, new BigDecimal("0.7"));
        assertEquals(OrderStatusEnum.FILLED, buyOrder.getStatus().getStatus());
        assertEquals(BigDecimal.ZERO, buyOrder.getRemainingQuantity());
    }
}
