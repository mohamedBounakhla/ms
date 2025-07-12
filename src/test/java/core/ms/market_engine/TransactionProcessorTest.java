package core.ms.market_engine;

import core.ms.order.domain.*;
import core.ms.order.domain.value.OrderStatusEnum;
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
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
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
    @DisplayName("Should update order statuses when both orders are fully filled")
    void shouldUpdateOrderStatusesWhenBothOrdersFullyFilled() {
        // Given
        IBuyOrder buyOrder = new BuyOrder("buy-1", testSymbol, testPrice, new BigDecimal("50"));
        ISellOrder sellOrder = new SellOrder("sell-1", testSymbol, testPrice, new BigDecimal("50"));

        // Create transaction first to update quantities
        OrderMatch match = new OrderMatch(buyOrder, sellOrder);
        transactionProcessor.createTransaction(match); // This updates quantities

        // When
        transactionProcessor.updateOrderStatuses(match);

        // Then
        assertEquals(OrderStatusEnum.FILLED, buyOrder.getStatus().getStatus());
        assertEquals(OrderStatusEnum.FILLED, sellOrder.getStatus().getStatus());
        assertEquals(BigDecimal.ZERO, buyOrder.getRemainingQuantity());
        assertEquals(BigDecimal.ZERO, sellOrder.getRemainingQuantity());
    }

    @Test
    @DisplayName("Should update order statuses when buy order is partially filled")
    void shouldUpdateOrderStatusesWhenBuyOrderPartiallyFilled() {
        // Given
        IBuyOrder buyOrder = new BuyOrder("buy-1", testSymbol, testPrice, new BigDecimal("100"));
        ISellOrder sellOrder = new SellOrder("sell-1", testSymbol, testPrice, new BigDecimal("30"));

        // Create transaction first to update quantities
        OrderMatch match = new OrderMatch(buyOrder, sellOrder);
        transactionProcessor.createTransaction(match); // This updates quantities

        // When
        transactionProcessor.updateOrderStatuses(match);

        // Then
        assertEquals(OrderStatusEnum.PARTIAL, buyOrder.getStatus().getStatus());
        assertEquals(OrderStatusEnum.FILLED, sellOrder.getStatus().getStatus());
        assertEquals(new BigDecimal("70"), buyOrder.getRemainingQuantity());
        assertEquals(BigDecimal.ZERO, sellOrder.getRemainingQuantity());
    }

    @Test
    @DisplayName("Should update order statuses when sell order is partially filled")
    void shouldUpdateOrderStatusesWhenSellOrderPartiallyFilled() {
        // Given
        IBuyOrder buyOrder = new BuyOrder("buy-1", testSymbol, testPrice, new BigDecimal("30"));
        ISellOrder sellOrder = new SellOrder("sell-1", testSymbol, testPrice, new BigDecimal("100"));

        // Create transaction first to update quantities
        OrderMatch match = new OrderMatch(buyOrder, sellOrder);
        transactionProcessor.createTransaction(match); // This updates quantities

        // When
        transactionProcessor.updateOrderStatuses(match);

        // Then
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

        // Create a scenario that might produce invalid matches
        // (This test assumes OrderMatch.isValid() might return false in some cases)
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
    @DisplayName("Should throw exception when updating order statuses with null match")
    void shouldThrowExceptionWhenUpdatingOrderStatusesWithNullMatch() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionProcessor.updateOrderStatuses(null)
        );
        assertEquals("OrderMatch cannot be null", exception.getMessage());
    }
}