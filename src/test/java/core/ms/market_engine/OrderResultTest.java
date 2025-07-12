package core.ms.market_engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderResultTest {

    @Test
    @DisplayName("Should create accepted order result")
    void shouldCreateAcceptedOrderResult() {
        // When
        OrderResult result = OrderResult.accepted("order-123");

        // Then
        assertEquals("order-123", result.getOrderId());
        assertTrue(result.isSuccess());
        assertEquals("Order accepted", result.getMessage());
        assertTrue(result.getTransactionIds().isEmpty());
        assertNotNull(result.getTimestamp());
    }

    @Test
    @DisplayName("Should create accepted order result with transactions")
    void shouldCreateAcceptedOrderResultWithTransactions() {
        // Given
        List<String> transactionIds = Arrays.asList("tx-1", "tx-2", "tx-3");

        // When
        OrderResult result = OrderResult.acceptedWithTransactions("order-456", transactionIds);

        // Then
        assertEquals("order-456", result.getOrderId());
        assertTrue(result.isSuccess());
        assertEquals("Order accepted and executed", result.getMessage());
        assertEquals(3, result.getTransactionIds().size());
        assertEquals("tx-1", result.getTransactionIds().get(0));
        assertEquals("tx-2", result.getTransactionIds().get(1));
        assertEquals("tx-3", result.getTransactionIds().get(2));
        assertNotNull(result.getTimestamp());
    }

    @Test
    @DisplayName("Should create rejected order result")
    void shouldCreateRejectedOrderResult() {
        // When
        OrderResult result = OrderResult.rejected("order-789", "Insufficient funds");

        // Then
        assertEquals("order-789", result.getOrderId());
        assertFalse(result.isSuccess());
        assertEquals("Insufficient funds", result.getMessage());
        assertTrue(result.getTransactionIds().isEmpty());
        assertNotNull(result.getTimestamp());
    }

    @Test
    @DisplayName("Should handle null transaction list in acceptedWithTransactions")
    void shouldHandleNullTransactionListInAcceptedWithTransactions() {
        // When
        OrderResult result = OrderResult.acceptedWithTransactions("order-null", null);

        // Then
        assertEquals("order-null", result.getOrderId());
        assertTrue(result.isSuccess());
        assertEquals("Order accepted and executed", result.getMessage());
        assertTrue(result.getTransactionIds().isEmpty());
        assertNotNull(result.getTimestamp());
    }

    @Test
    @DisplayName("Should return defensive copy of transaction IDs")
    void shouldReturnDefensiveCopyOfTransactionIds() {
        // Given
        List<String> originalIds = Arrays.asList("tx-1", "tx-2");
        OrderResult result = OrderResult.acceptedWithTransactions("order-copy", originalIds);

        // When
        List<String> returnedIds = result.getTransactionIds();
        returnedIds.add("tx-3"); // Try to modify returned list

        // Then
        assertEquals(2, result.getTransactionIds().size()); // Original should be unchanged
        assertNotSame(originalIds, result.getTransactionIds()); // Should be different instances
    }

    @Test
    @DisplayName("Should throw exception when creating result with null order ID")
    void shouldThrowExceptionWhenCreatingResultWithNullOrderId() {
        // When & Then
        assertThrows(
                NullPointerException.class,
                () -> new OrderResult(null, true, "Success", Arrays.asList())
        );
    }

    @Test
    @DisplayName("Should throw exception when creating result with null message")
    void shouldThrowExceptionWhenCreatingResultWithNullMessage() {
        // When & Then
        assertThrows(
                NullPointerException.class,
                () -> new OrderResult("order-1", true, null, Arrays.asList())
        );
    }

    @Test
    @DisplayName("Should throw exception in factory methods with null order ID")
    void shouldThrowExceptionInFactoryMethodsWithNullOrderId() {
        // When & Then
        assertThrows(
                NullPointerException.class,
                () -> OrderResult.accepted(null)
        );

        assertThrows(
                NullPointerException.class,
                () -> OrderResult.acceptedWithTransactions(null, Arrays.asList("tx-1"))
        );

        assertThrows(
                NullPointerException.class,
                () -> OrderResult.rejected(null, "Some reason")
        );
    }

    @Test
    @DisplayName("Should have proper toString representation")
    void shouldHaveProperToStringRepresentation() {
        // Given
        List<String> transactionIds = Arrays.asList("tx-1", "tx-2");
        OrderResult result = OrderResult.acceptedWithTransactions("order-display", transactionIds);

        // When
        String toString = result.toString();

        // Then
        assertTrue(toString.contains("order-display"));
        assertTrue(toString.contains("success=true"));
        assertTrue(toString.contains("Order accepted and executed"));
        assertTrue(toString.contains("transactionIds=2"));
    }

    @Test
    @DisplayName("Should handle empty transaction list")
    void shouldHandleEmptyTransactionList() {
        // Given
        List<String> emptyList = Arrays.asList();

        // When
        OrderResult result = OrderResult.acceptedWithTransactions("order-empty", emptyList);

        // Then
        assertEquals("order-empty", result.getOrderId());
        assertTrue(result.isSuccess());
        assertTrue(result.getTransactionIds().isEmpty());
    }

    @Test
    @DisplayName("Should create result with custom constructor")
    void shouldCreateResultWithCustomConstructor() {
        // Given
        List<String> customTransactions = Arrays.asList("custom-tx-1");

        // When
        OrderResult result = new OrderResult("custom-order", false, "Custom failure reason", customTransactions);

        // Then
        assertEquals("custom-order", result.getOrderId());
        assertFalse(result.isSuccess());
        assertEquals("Custom failure reason", result.getMessage());
        assertEquals(1, result.getTransactionIds().size());
        assertEquals("custom-tx-1", result.getTransactionIds().get(0));
        assertNotNull(result.getTimestamp());
    }
}