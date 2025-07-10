package core.ms.order;

import core.ms.order.domain.*;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("2. Quantity Tracking Tests")
class QuantityTrackingTest {

    private Symbol btcUsd;
    private Money price;

    @BeforeEach
    void setUp() {
        btcUsd = Symbol.btcUsd();
        price = Money.of("45000", Currency.USD);
    }

    @Test
    @DisplayName("Should track executed quantity after transaction")
    void shouldTrackExecutedQuantityAfterTransaction() {
        // Given: Order with 2.0 BTC
        BigDecimal orderQuantity = new BigDecimal("2.0");
        BigDecimal transactionQuantity = new BigDecimal("0.5");

        IBuyOrder buyOrder = new BuyOrder("buy-1", btcUsd, price, orderQuantity);
        ISellOrder sellOrder = new SellOrder("sell-1", btcUsd, price, orderQuantity);

        // When: Transaction executes 0.5 BTC
        Transaction transaction = new Transaction("tx-1", btcUsd, buyOrder, sellOrder, price, transactionQuantity);

        // Then: Orders should track executed quantity
        assertEquals(transactionQuantity, buyOrder.getExecutedQuantity());
        assertEquals(transactionQuantity, sellOrder.getExecutedQuantity());
    }

    @Test
    @DisplayName("Should track remaining quantity after transaction")
    void shouldTrackRemainingQuantityAfterTransaction() {
        // Given: Order with 2.0 BTC
        BigDecimal orderQuantity = new BigDecimal("2.0");
        BigDecimal transactionQuantity = new BigDecimal("0.5");
        BigDecimal expectedRemaining = new BigDecimal("1.5");

        IBuyOrder buyOrder = new BuyOrder("buy-1", btcUsd, price, orderQuantity);
        ISellOrder sellOrder = new SellOrder("sell-1", btcUsd, price, orderQuantity);

        // When: Transaction executes 0.5 BTC
        Transaction transaction = new Transaction("tx-1", btcUsd, buyOrder, sellOrder, price, transactionQuantity);

        // Then: Orders should track remaining quantity
        assertEquals(expectedRemaining, buyOrder.getRemainingQuantity());
        assertEquals(expectedRemaining, sellOrder.getRemainingQuantity());
    }

    @Test
    @DisplayName("Should accumulate executed quantity across multiple transactions")
    void shouldAccumulateExecutedQuantityAcrossMultipleTransactions() {
        // Given: Order with 3.0 BTC
        BigDecimal orderQuantity = new BigDecimal("3.0");
        BigDecimal firstTransaction = new BigDecimal("1.0");
        BigDecimal secondTransaction = new BigDecimal("0.5");
        BigDecimal expectedExecuted = new BigDecimal("1.5");
        BigDecimal expectedRemaining = new BigDecimal("1.5");

        IBuyOrder buyOrder = new BuyOrder("buy-1", btcUsd, price, orderQuantity);
        ISellOrder sellOrder = new SellOrder("sell-1", btcUsd, price, orderQuantity);

        // When: Multiple transactions execute
        Transaction tx1 = new Transaction("tx-1", btcUsd, buyOrder, sellOrder, price, firstTransaction);
        Transaction tx2 = new Transaction("tx-2", btcUsd, buyOrder, sellOrder, price, secondTransaction);

        // Then: Should accumulate executed quantities
        assertEquals(expectedExecuted, buyOrder.getExecutedQuantity());
        assertEquals(expectedExecuted, sellOrder.getExecutedQuantity());
        assertEquals(expectedRemaining, buyOrder.getRemainingQuantity());
        assertEquals(expectedRemaining, sellOrder.getRemainingQuantity());
    }

    @Test
    @DisplayName("Should have zero remaining quantity when order is fully executed")
    void shouldHaveZeroRemainingQuantityWhenOrderIsFullyExecuted() {
        // Given: Order with 1.0 BTC
        BigDecimal orderQuantity = new BigDecimal("1.0");

        IBuyOrder buyOrder = new BuyOrder("buy-1", btcUsd, price, orderQuantity);
        ISellOrder sellOrder = new SellOrder("sell-1", btcUsd, price, orderQuantity);

        // When: Transaction executes full quantity
        Transaction transaction = new Transaction("tx-1", btcUsd, buyOrder, sellOrder, price, orderQuantity);

        // Then: Remaining quantity should be zero
        assertEquals(BigDecimal.ZERO, buyOrder.getRemainingQuantity());
        assertEquals(BigDecimal.ZERO, sellOrder.getRemainingQuantity());
        assertEquals(orderQuantity, buyOrder.getExecutedQuantity());
        assertEquals(orderQuantity, sellOrder.getExecutedQuantity());
    }
}

