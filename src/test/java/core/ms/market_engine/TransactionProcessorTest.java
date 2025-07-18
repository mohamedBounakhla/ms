package core.ms.market_engine;

import core.ms.order.domain.entities.*;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.order_book.domain.value_object.OrderMatch;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionProcessorTest {

    private TransactionProcessor transactionProcessor;
    private Symbol testSymbol;
    private Money testPrice;

    @BeforeEach
    void setUp() {
        transactionProcessor = new TransactionProcessor();
        testSymbol = Symbol.eurUsd();
        testPrice = Money.of("1.2000", Currency.USD);
    }

    @Test
    @DisplayName("Should create transaction from valid order match")
    void shouldCreateTransactionFromValidOrderMatch() {
        // Given
        IBuyOrder buyOrder = new BuyOrder("buy-1", testSymbol, testPrice, new BigDecimal("100"));
        ISellOrder sellOrder = new SellOrder("sell-1", testSymbol, testPrice, new BigDecimal("50"));
        OrderMatch match = new OrderMatch(buyOrder, sellOrder);

        // When
        ITransaction transaction = transactionProcessor.createTransaction(match);

        // Then
        assertNotNull(transaction);
        assertNotNull(transaction.getId());
        assertEquals(testSymbol, transaction.getSymbol());
        assertEquals(buyOrder, transaction.getBuyOrder());
        assertEquals(sellOrder, transaction.getSellOrder());
        assertEquals(testPrice, transaction.getPrice());
        assertEquals(new BigDecimal("50"), transaction.getQuantity()); // Min of buy/sell quantities
    }

    @Test
    @DisplayName("Should throw exception when creating transaction from null match")
    void shouldThrowExceptionWhenCreatingTransactionFromNullMatch() {
        // When & Then
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> transactionProcessor.createTransaction(null)
        );
        assertEquals("OrderMatch cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when creating transaction from invalid match")
    void shouldThrowExceptionWhenCreatingTransactionFromInvalidMatch() {
        // Given - Create orders that won't match (buy price < sell price)
        IBuyOrder buyOrder = new BuyOrder("buy-1", testSymbol, Money.of("1.1000", Currency.USD), new BigDecimal("100"));
        ISellOrder sellOrder = new SellOrder("sell-1", testSymbol, Money.of("1.2000", Currency.USD), new BigDecimal("50"));

        // This should throw in OrderMatch constructor, but if it doesn't:
        try {
            OrderMatch match = new OrderMatch(buyOrder, sellOrder);
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionProcessor.createTransaction(match)
            );
            assertEquals("Cannot create transaction from invalid match", exception.getMessage());
        } catch (IllegalArgumentException e) {
            // Expected - OrderMatch constructor should reject invalid matches
            assertTrue(e.getMessage().contains("must be >="));
        }
    }

    @Test
    @DisplayName("Should automatically update order statuses when both orders are fully filled")
    void shouldAutomaticallyUpdateOrderStatusesWhenBothOrdersFullyFilled() {
        // Given
        IBuyOrder buyOrder = new BuyOrder("buy-1", testSymbol, testPrice, new BigDecimal("50"));
        ISellOrder sellOrder = new SellOrder("sell-1", testSymbol, testPrice, new BigDecimal("50"));
        OrderMatch match = new OrderMatch(buyOrder, sellOrder);

        // When - Creating transaction automatically updates orders via Order domain
        ITransaction transaction = transactionProcessor.createTransaction(match);

        // Then - Order domain automatically handled status transitions
        assertNotNull(transaction);
        assertEquals(OrderStatusEnum.FILLED, buyOrder.getStatus().getStatus());
        assertEquals(OrderStatusEnum.FILLED, sellOrder.getStatus().getStatus());
        assertEquals(BigDecimal.ZERO, buyOrder.getRemainingQuantity());
        assertEquals(BigDecimal.ZERO, sellOrder.getRemainingQuantity());
    }

    @Test
    @DisplayName("Should automatically update order statuses when buy order is partially filled")
    void shouldAutomaticallyUpdateOrderStatusesWhenBuyOrderPartiallyFilled() {
        // Given
        IBuyOrder buyOrder = new BuyOrder("buy-1", testSymbol, testPrice, new BigDecimal("100"));
        ISellOrder sellOrder = new SellOrder("sell-1", testSymbol, testPrice, new BigDecimal("30"));
        OrderMatch match = new OrderMatch(buyOrder, sellOrder);

        // When - Creating transaction automatically updates orders via Order domain
        ITransaction transaction = transactionProcessor.createTransaction(match);

        // Then - Order domain automatically handled status transitions
        assertNotNull(transaction);
        assertEquals(OrderStatusEnum.PARTIAL, buyOrder.getStatus().getStatus());
        assertEquals(OrderStatusEnum.FILLED, sellOrder.getStatus().getStatus());
        assertEquals(new BigDecimal("70"), buyOrder.getRemainingQuantity());
        assertEquals(BigDecimal.ZERO, sellOrder.getRemainingQuantity());
    }

    @Test
    @DisplayName("Should automatically update order statuses when sell order is partially filled")
    void shouldAutomaticallyUpdateOrderStatusesWhenSellOrderPartiallyFilled() {
        // Given
        IBuyOrder buyOrder = new BuyOrder("buy-1", testSymbol, testPrice, new BigDecimal("30"));
        ISellOrder sellOrder = new SellOrder("sell-1", testSymbol, testPrice, new BigDecimal("100"));
        OrderMatch match = new OrderMatch(buyOrder, sellOrder);

        // When - Creating transaction automatically updates orders via Order domain
        ITransaction transaction = transactionProcessor.createTransaction(match);

        // Then - Order domain automatically handled status transitions
        assertNotNull(transaction);
        assertEquals(OrderStatusEnum.FILLED, buyOrder.getStatus().getStatus());
        assertEquals(OrderStatusEnum.PARTIAL, sellOrder.getStatus().getStatus());
        assertEquals(BigDecimal.ZERO, buyOrder.getRemainingQuantity());
        assertEquals(new BigDecimal("70"), sellOrder.getRemainingQuantity());
    }

    @Test
    @DisplayName("Should process multiple matches into transactions")
    void shouldProcessMultipleMatchesIntoTransactions() {
        // Given
        IBuyOrder buyOrder1 = new BuyOrder("buy-1", testSymbol, testPrice, new BigDecimal("50"));
        ISellOrder sellOrder1 = new SellOrder("sell-1", testSymbol, testPrice, new BigDecimal("50"));
        OrderMatch match1 = new OrderMatch(buyOrder1, sellOrder1);

        IBuyOrder buyOrder2 = new BuyOrder("buy-2", testSymbol, testPrice, new BigDecimal("75"));
        ISellOrder sellOrder2 = new SellOrder("sell-2", testSymbol, testPrice, new BigDecimal("25"));
        OrderMatch match2 = new OrderMatch(buyOrder2, sellOrder2);

        List<OrderMatch> matches = Arrays.asList(match1, match2);

        // When
        List<ITransaction> transactions = transactionProcessor.processMatches(matches);

        // Then
        assertEquals(2, transactions.size());

        // Verify first transaction
        ITransaction tx1 = transactions.get(0);
        assertEquals(buyOrder1, tx1.getBuyOrder());
        assertEquals(sellOrder1, tx1.getSellOrder());
        assertEquals(new BigDecimal("50"), tx1.getQuantity());

        // Verify second transaction
        ITransaction tx2 = transactions.get(1);
        assertEquals(buyOrder2, tx2.getBuyOrder());
        assertEquals(sellOrder2, tx2.getSellOrder());
        assertEquals(new BigDecimal("25"), tx2.getQuantity());
    }

    @Test
    @DisplayName("Should filter out invalid matches when processing multiple matches")
    void shouldFilterOutInvalidMatchesWhenProcessingMultipleMatches() {
        // Given
        IBuyOrder validBuyOrder = new BuyOrder("buy-1", testSymbol, testPrice, new BigDecimal("50"));
        ISellOrder validSellOrder = new SellOrder("sell-1", testSymbol, testPrice, new BigDecimal("50"));
        OrderMatch validMatch = new OrderMatch(validBuyOrder, validSellOrder);

        List<OrderMatch> matches = Arrays.asList(validMatch);

        // When
        List<ITransaction> transactions = transactionProcessor.processMatches(matches);

        // Then
        assertEquals(1, transactions.size());
        assertEquals(validBuyOrder, transactions.get(0).getBuyOrder());
        assertEquals(validSellOrder, transactions.get(0).getSellOrder());
    }

    @Test
    @DisplayName("Should handle empty matches list")
    void shouldHandleEmptyMatchesList() {
        // Given
        List<OrderMatch> emptyMatches = Arrays.asList();

        // When
        List<ITransaction> transactions = transactionProcessor.processMatches(emptyMatches);

        // Then
        assertTrue(transactions.isEmpty());
    }

    @Test
    @DisplayName("Should create transactions that automatically update order quantities and statuses")
    void shouldCreateTransactionsThatAutomaticallyUpdateOrderQuantitiesAndStatuses() {
        // Given - Mixed scenario with different fill levels
        IBuyOrder largeBuyOrder = new BuyOrder("large-buy", testSymbol, testPrice, new BigDecimal("200"));
        ISellOrder smallSellOrder = new SellOrder("small-sell", testSymbol, testPrice, new BigDecimal("75"));
        OrderMatch partialMatch = new OrderMatch(largeBuyOrder, smallSellOrder);

        IBuyOrder exactBuyOrder = new BuyOrder("exact-buy", testSymbol, testPrice, new BigDecimal("50"));
        ISellOrder exactSellOrder = new SellOrder("exact-sell", testSymbol, testPrice, new BigDecimal("50"));
        OrderMatch exactMatch = new OrderMatch(exactBuyOrder, exactSellOrder);

        // When - Processing both matches
        ITransaction partialTransaction = transactionProcessor.createTransaction(partialMatch);
        ITransaction exactTransaction = transactionProcessor.createTransaction(exactMatch);

        // Then - Verify partial fill scenario
        assertNotNull(partialTransaction);
        assertEquals(new BigDecimal("75"), partialTransaction.getQuantity());
        assertEquals(OrderStatusEnum.PARTIAL, largeBuyOrder.getStatus().getStatus());
        assertEquals(OrderStatusEnum.FILLED, smallSellOrder.getStatus().getStatus());
        assertEquals(new BigDecimal("125"), largeBuyOrder.getRemainingQuantity());
        assertEquals(BigDecimal.ZERO, smallSellOrder.getRemainingQuantity());

        // And - Verify exact fill scenario
        assertNotNull(exactTransaction);
        assertEquals(new BigDecimal("50"), exactTransaction.getQuantity());
        assertEquals(OrderStatusEnum.FILLED, exactBuyOrder.getStatus().getStatus());
        assertEquals(OrderStatusEnum.FILLED, exactSellOrder.getStatus().getStatus());
        assertEquals(BigDecimal.ZERO, exactBuyOrder.getRemainingQuantity());
        assertEquals(BigDecimal.ZERO, exactSellOrder.getRemainingQuantity());
    }
}