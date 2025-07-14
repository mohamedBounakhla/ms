package core.ms.portfolio.domain;

import core.ms.portfolio.domain.value.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Transaction Type Tests")
class TransactionTypeTest {

    @Test
    @DisplayName("Should correctly identify cash affecting transactions")
    void shouldCorrectlyIdentifyCashAffectingTransactions() {
        // All transaction types affect cash
        assertTrue(TransactionType.BUY.affectsCash());
        assertTrue(TransactionType.SELL.affectsCash());
        assertTrue(TransactionType.DEPOSIT.affectsCash());
        assertTrue(TransactionType.WITHDRAWAL.affectsCash());
    }

    @Test
    @DisplayName("Should correctly identify position affecting transactions")
    void shouldCorrectlyIdentifyPositionAffectingTransactions() {
        // Only BUY and SELL affect positions
        assertTrue(TransactionType.BUY.affectsPosition());
        assertTrue(TransactionType.SELL.affectsPosition());
        assertFalse(TransactionType.DEPOSIT.affectsPosition());
        assertFalse(TransactionType.WITHDRAWAL.affectsPosition());
    }

    @Test
    @DisplayName("Should correctly identify asset transactions")
    void shouldCorrectlyIdentifyAssetTransactions() {
        // BUY and SELL are asset transactions
        assertTrue(TransactionType.BUY.isAssetTransaction());
        assertTrue(TransactionType.SELL.isAssetTransaction());
        assertFalse(TransactionType.DEPOSIT.isAssetTransaction());
        assertFalse(TransactionType.WITHDRAWAL.isAssetTransaction());
    }

    @Test
    @DisplayName("Should correctly identify cash transactions")
    void shouldCorrectlyIdentifyCashTransactions() {
        // DEPOSIT and WITHDRAWAL are cash-only transactions
        assertFalse(TransactionType.BUY.isCashTransaction());
        assertFalse(TransactionType.SELL.isCashTransaction());
        assertTrue(TransactionType.DEPOSIT.isCashTransaction());
        assertTrue(TransactionType.WITHDRAWAL.isCashTransaction());
    }

    @Test
    @DisplayName("Should correctly identify cash increasing transactions")
    void shouldCorrectlyIdentifyCashIncreasingTransactions() {
        // SELL and DEPOSIT increase cash
        assertFalse(TransactionType.BUY.increasesCash());
        assertTrue(TransactionType.SELL.increasesCash());
        assertTrue(TransactionType.DEPOSIT.increasesCash());
        assertFalse(TransactionType.WITHDRAWAL.increasesCash());
    }

    @Test
    @DisplayName("Should correctly identify cash decreasing transactions")
    void shouldCorrectlyIdentifyCashDecreasingTransactions() {
        // BUY and WITHDRAWAL decrease cash
        assertTrue(TransactionType.BUY.decreasesCash());
        assertFalse(TransactionType.SELL.decreasesCash());
        assertFalse(TransactionType.DEPOSIT.decreasesCash());
        assertTrue(TransactionType.WITHDRAWAL.decreasesCash());
    }

    @Test
    @DisplayName("Should have proper descriptions")
    void shouldHaveProperDescriptions() {
        assertEquals("Asset Purchase", TransactionType.BUY.getDescription());
        assertEquals("Asset Sale", TransactionType.SELL.getDescription());
        assertEquals("Cash Deposit", TransactionType.DEPOSIT.getDescription());
        assertEquals("Cash Withdrawal", TransactionType.WITHDRAWAL.getDescription());
    }

    @Test
    @DisplayName("Should have proper toString representation")
    void shouldHaveProperToStringRepresentation() {
        assertEquals("Asset Purchase", TransactionType.BUY.toString());
        assertEquals("Asset Sale", TransactionType.SELL.toString());
        assertEquals("Cash Deposit", TransactionType.DEPOSIT.toString());
        assertEquals("Cash Withdrawal", TransactionType.WITHDRAWAL.toString());
    }

    @Test
    @DisplayName("Should have all expected enum values")
    void shouldHaveAllExpectedEnumValues() {
        TransactionType[] values = TransactionType.values();
        assertEquals(4, values.length);

        assertTrue(java.util.Arrays.asList(values).contains(TransactionType.BUY));
        assertTrue(java.util.Arrays.asList(values).contains(TransactionType.SELL));
        assertTrue(java.util.Arrays.asList(values).contains(TransactionType.DEPOSIT));
        assertTrue(java.util.Arrays.asList(values).contains(TransactionType.WITHDRAWAL));
    }
}