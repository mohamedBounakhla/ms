package core.ms.order;

import core.ms.order.domain.entities.*;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("5. Transaction Ordering & Timestamps Tests")
class TransactionOrderingTest {

    private Symbol btcUsd;
    private Money price;

    @BeforeEach
    void setUp() {
        btcUsd = Symbol.btcUsd();
        price = Money.of("45000", Currency.USD);
    }

    @Test
    @DisplayName("Should create transactions with proper timestamps")
    void shouldCreateTransactionsWithProperTimestamps() {
        // Given
        BigDecimal quantity = new BigDecimal("1.0");
        IBuyOrder buyOrder = new BuyOrder("buy-1", btcUsd, price, quantity);
        ISellOrder sellOrder = new SellOrder("sell-1", btcUsd, price, quantity);

        LocalDateTime beforeTransaction = LocalDateTime.now();

        // When
        Transaction transaction = new Transaction("tx-1", btcUsd, buyOrder, sellOrder, price, quantity);

        LocalDateTime afterTransaction = LocalDateTime.now();

        // Then
        assertNotNull(transaction.getCreatedAt());
        assertTrue(transaction.getCreatedAt().isAfter(beforeTransaction) ||
                transaction.getCreatedAt().isEqual(beforeTransaction));
        assertTrue(transaction.getCreatedAt().isBefore(afterTransaction) ||
                transaction.getCreatedAt().isEqual(afterTransaction));
    }

    @Test
    @DisplayName("Should maintain transaction sequence order")
    void shouldMaintainTransactionSequenceOrder() {
        // Given: Order with sufficient quantity
        BigDecimal orderQuantity = new BigDecimal("3.0");
        IBuyOrder buyOrder = new BuyOrder("buy-1", btcUsd, price, orderQuantity);
        ISellOrder sellOrder = new SellOrder("sell-1", btcUsd, price, orderQuantity);

        // When: Creating transactions in sequence
        Transaction tx1 = new Transaction("tx-1", btcUsd, buyOrder, sellOrder, price, new BigDecimal("1.0"));
        // Small delay to ensure different timestamps
        try { Thread.sleep(1); } catch (InterruptedException e) {}

        Transaction tx2 = new Transaction("tx-2", btcUsd, buyOrder, sellOrder, price, new BigDecimal("1.0"));
        try { Thread.sleep(1); } catch (InterruptedException e) {}

        Transaction tx3 = new Transaction("tx-3", btcUsd, buyOrder, sellOrder, price, new BigDecimal("1.0"));

        // Then: Transactions should be in chronological order
        assertTrue(tx1.getCreatedAt().isBefore(tx2.getCreatedAt()));
        assertTrue(tx2.getCreatedAt().isBefore(tx3.getCreatedAt()));

        // And: Order should maintain transaction sequence
        List<ITransaction> transactions = buyOrder.getTransactions();
        assertEquals(tx1, transactions.get(0));
        assertEquals(tx2, transactions.get(1));
        assertEquals(tx3, transactions.get(2));
    }

    @Test
    @DisplayName("Should prevent concurrent transaction creation on same order")
    void shouldPreventConcurrentTransactionCreationOnSameOrder() {
        // Given: Order with limited remaining quantity
        BigDecimal orderQuantity = new BigDecimal("1.0");
        IBuyOrder buyOrder = new BuyOrder("buy-1", btcUsd, price, orderQuantity);
        ISellOrder sellOrder = new SellOrder("sell-1", btcUsd, price, orderQuantity);

        // When: Attempting concurrent transactions that would exceed remaining quantity
        // This simulates a race condition where two transactions try to execute simultaneously

        // First transaction should succeed
        Transaction tx1 = new Transaction("tx-1", btcUsd, buyOrder, sellOrder, price, new BigDecimal("0.8"));

        // Second transaction should fail (only 0.2 remaining, but trying to execute 0.5)
        assertThrows(IllegalArgumentException.class,
                () -> new Transaction("tx-2", btcUsd, buyOrder, sellOrder, price, new BigDecimal("0.5")));
    }

    @Test
    @DisplayName("Should generate unique transaction IDs")
    void shouldGenerateUniqueTransactionIDs() {
        // Given
        BigDecimal quantity = new BigDecimal("0.1");
        IBuyOrder buyOrder1 = new BuyOrder("buy-1", btcUsd, price, new BigDecimal("1.0"));
        ISellOrder sellOrder1 = new SellOrder("sell-1", btcUsd, price, new BigDecimal("1.0"));
        IBuyOrder buyOrder2 = new BuyOrder("buy-2", btcUsd, price, new BigDecimal("1.0"));
        ISellOrder sellOrder2 = new SellOrder("sell-2", btcUsd, price, new BigDecimal("1.0"));

        // When: Creating multiple transactions
        Transaction tx1 = new Transaction("tx-1", btcUsd, buyOrder1, sellOrder1, price, quantity);
        Transaction tx2 = new Transaction("tx-2", btcUsd, buyOrder2, sellOrder2, price, quantity);

        // Then: Transaction IDs should be unique
        assertNotEquals(tx1.getId(), tx2.getId());
    }

    @Test
    @DisplayName("Should track transaction execution sequence within order")
    void shouldTrackTransactionExecutionSequenceWithinOrder() {
        // Given: Order with sufficient quantity
        BigDecimal orderQuantity = new BigDecimal("3.0");
        IBuyOrder buyOrder = new BuyOrder("buy-1", btcUsd, price, orderQuantity);
        ISellOrder sellOrder = new SellOrder("sell-1", btcUsd, price, orderQuantity);

        // When: Multiple transactions execute
        Transaction tx1 = new Transaction("tx-1", btcUsd, buyOrder, sellOrder, price, new BigDecimal("1.0"));
        Transaction tx2 = new Transaction("tx-2", btcUsd, buyOrder, sellOrder, price, new BigDecimal("1.0"));
        Transaction tx3 = new Transaction("tx-3", btcUsd, buyOrder, sellOrder, price, new BigDecimal("1.0"));

        // Then: Order should track execution sequence
        assertEquals(1, buyOrder.getTransactionSequence(tx1));
        assertEquals(2, buyOrder.getTransactionSequence(tx2));
        assertEquals(3, buyOrder.getTransactionSequence(tx3));
    }
}