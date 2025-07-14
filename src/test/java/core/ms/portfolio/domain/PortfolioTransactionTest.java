package core.ms.portfolio.domain;

import core.ms.portfolio.domain.value.TransactionType;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Portfolio Transaction Tests")
class PortfolioTransactionTest {

    private String transactionId;
    private String portfolioId;
    private Symbol testSymbol;
    private Money testPrice;
    private Money totalAmount;

    @BeforeEach
    void setUp() {
        transactionId = "tx-123";
        portfolioId = "portfolio-456";
        testSymbol = Symbol.btcUsd();
        testPrice = Money.of("50000.00", Currency.USD);
        totalAmount = Money.of("5000.00", Currency.USD);
    }

    @Nested
    @DisplayName("Asset Transaction Tests")
    class AssetTransactionTests {

        @Test
        @DisplayName("Should create buy transaction successfully")
        void shouldCreateBuyTransactionSuccessfully() {
            // Given
            BigDecimal quantity = new BigDecimal("0.1");
            String orderId = "order-789";

            // When
            PortfolioTransaction transaction = PortfolioTransaction.createBuyTransaction(
                    transactionId, portfolioId, testSymbol, quantity, testPrice, orderId);

            // Then
            assertEquals(transactionId, transaction.getId());
            assertEquals(portfolioId, transaction.getPortfolioId());
            assertEquals(TransactionType.BUY, transaction.getType());
            assertEquals(testSymbol, transaction.getSymbol());
            assertEquals(quantity, transaction.getQuantity());
            assertEquals(testPrice, transaction.getPrice());
            assertEquals(testPrice.multiply(quantity), transaction.getTotalAmount());
            assertEquals(orderId, transaction.getRelatedOrderId());
            assertNotNull(transaction.getTimestamp());

            assertTrue(transaction.isAssetTransaction());
            assertFalse(transaction.isCashTransaction());
            assertTrue(transaction.affectsCashBalance());
            assertTrue(transaction.affectsPosition());
        }

        @Test
        @DisplayName("Should create sell transaction successfully")
        void shouldCreateSellTransactionSuccessfully() {
            // Given
            BigDecimal quantity = new BigDecimal("0.05");
            String orderId = "order-790";

            // When
            PortfolioTransaction transaction = PortfolioTransaction.createSellTransaction(
                    transactionId, portfolioId, testSymbol, quantity, testPrice, orderId);

            // Then
            assertEquals(transactionId, transaction.getId());
            assertEquals(portfolioId, transaction.getPortfolioId());
            assertEquals(TransactionType.SELL, transaction.getType());
            assertEquals(testSymbol, transaction.getSymbol());
            assertEquals(quantity, transaction.getQuantity());
            assertEquals(testPrice, transaction.getPrice());
            assertEquals(testPrice.multiply(quantity), transaction.getTotalAmount());
            assertEquals(orderId, transaction.getRelatedOrderId());

            assertTrue(transaction.isAssetTransaction());
            assertFalse(transaction.isCashTransaction());
            assertTrue(transaction.affectsCashBalance());
            assertTrue(transaction.affectsPosition());
        }

        @Test
        @DisplayName("Should get symbol code correctly")
        void shouldGetSymbolCodeCorrectly() {
            // Given
            PortfolioTransaction transaction = PortfolioTransaction.createBuyTransaction(
                    transactionId, portfolioId, testSymbol, new BigDecimal("0.1"), testPrice, null);

            // When & Then
            assertEquals("BTC", transaction.getSymbolCode());
        }

        @Test
        @DisplayName("Should validate asset transaction requirements")
        void shouldValidateAssetTransactionRequirements() {
            // When & Then - Missing symbol should throw
            assertThrows(IllegalArgumentException.class, () ->
                    new PortfolioTransaction(transactionId, portfolioId, TransactionType.BUY,
                            null, new BigDecimal("0.1"), testPrice, totalAmount, null));

            // When & Then - Missing price should throw
            assertThrows(IllegalArgumentException.class, () ->
                    new PortfolioTransaction(transactionId, portfolioId, TransactionType.BUY,
                            testSymbol, new BigDecimal("0.1"), null, totalAmount, null));

            // When & Then - Zero quantity should throw
            assertThrows(IllegalArgumentException.class, () ->
                    new PortfolioTransaction(transactionId, portfolioId, TransactionType.BUY,
                            testSymbol, BigDecimal.ZERO, testPrice, totalAmount, null));

            // When & Then - Negative quantity should throw
            assertThrows(IllegalArgumentException.class, () ->
                    new PortfolioTransaction(transactionId, portfolioId, TransactionType.BUY,
                            testSymbol, new BigDecimal("-0.1"), testPrice, totalAmount, null));
        }
    }

    @Nested
    @DisplayName("Cash Transaction Tests")
    class CashTransactionTests {

        @Test
        @DisplayName("Should create deposit transaction successfully")
        void shouldCreateDepositTransactionSuccessfully() {
            // Given
            Money depositAmount = Money.of("1000.00", Currency.USD);

            // When
            PortfolioTransaction transaction = PortfolioTransaction.createDepositTransaction(
                    transactionId, portfolioId, depositAmount);

            // Then
            assertEquals(transactionId, transaction.getId());
            assertEquals(portfolioId, transaction.getPortfolioId());
            assertEquals(TransactionType.DEPOSIT, transaction.getType());
            assertNull(transaction.getSymbol());
            assertEquals(BigDecimal.ZERO, transaction.getQuantity());
            assertNull(transaction.getPrice());
            assertEquals(depositAmount, transaction.getTotalAmount());
            assertNull(transaction.getRelatedOrderId());

            assertFalse(transaction.isAssetTransaction());
            assertTrue(transaction.isCashTransaction());
            assertTrue(transaction.affectsCashBalance());
            assertFalse(transaction.affectsPosition());
        }

        @Test
        @DisplayName("Should create withdrawal transaction successfully")
        void shouldCreateWithdrawalTransactionSuccessfully() {
            // Given
            Money withdrawalAmount = Money.of("500.00", Currency.USD);

            // When
            PortfolioTransaction transaction = PortfolioTransaction.createWithdrawalTransaction(
                    transactionId, portfolioId, withdrawalAmount);

            // Then
            assertEquals(transactionId, transaction.getId());
            assertEquals(portfolioId, transaction.getPortfolioId());
            assertEquals(TransactionType.WITHDRAWAL, transaction.getType());
            assertNull(transaction.getSymbol());
            assertEquals(BigDecimal.ZERO, transaction.getQuantity());
            assertNull(transaction.getPrice());
            assertEquals(withdrawalAmount, transaction.getTotalAmount());
            assertNull(transaction.getRelatedOrderId());

            assertFalse(transaction.isAssetTransaction());
            assertTrue(transaction.isCashTransaction());
            assertTrue(transaction.affectsCashBalance());
            assertFalse(transaction.affectsPosition());
        }

        @Test
        @DisplayName("Should return null symbol code for cash transactions")
        void shouldReturnNullSymbolCodeForCashTransactions() {
            // Given
            PortfolioTransaction transaction = PortfolioTransaction.createDepositTransaction(
                    transactionId, portfolioId, totalAmount);

            // When & Then
            assertNull(transaction.getSymbolCode());
        }

        @Test
        @DisplayName("Should validate cash transaction requirements")
        void shouldValidateCashTransactionRequirements() {
            // When & Then - Symbol should be null for cash transactions
            assertThrows(IllegalArgumentException.class, () ->
                    new PortfolioTransaction(transactionId, portfolioId, TransactionType.DEPOSIT,
                            testSymbol, BigDecimal.ZERO, null, totalAmount, null));

            // When & Then - Price should be null for cash transactions
            assertThrows(IllegalArgumentException.class, () ->
                    new PortfolioTransaction(transactionId, portfolioId, TransactionType.DEPOSIT,
                            null, BigDecimal.ZERO, testPrice, totalAmount, null));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw exception for null required fields")
        void shouldThrowExceptionForNullRequiredFields() {
            // When & Then - Null transaction ID
            assertThrows(NullPointerException.class, () ->
                    new PortfolioTransaction(null, portfolioId, TransactionType.DEPOSIT,
                            null, BigDecimal.ZERO, null, totalAmount, null));

            // When & Then - Null portfolio ID
            assertThrows(NullPointerException.class, () ->
                    new PortfolioTransaction(transactionId, null, TransactionType.DEPOSIT,
                            null, BigDecimal.ZERO, null, totalAmount, null));

            // When & Then - Null transaction type
            assertThrows(NullPointerException.class, () ->
                    new PortfolioTransaction(transactionId, portfolioId, null,
                            null, BigDecimal.ZERO, null, totalAmount, null));

            // When & Then - Null total amount
            assertThrows(NullPointerException.class, () ->
                    new PortfolioTransaction(transactionId, portfolioId, TransactionType.DEPOSIT,
                            null, BigDecimal.ZERO, null, null, null));
        }

        @Test
        @DisplayName("Should throw exception for negative total amount")
        void shouldThrowExceptionForNegativeTotalAmount() {
            // Given
            Money negativeAmount = Money.of("-100.00", Currency.USD);

            // When & Then
            assertThrows(IllegalArgumentException.class, () ->
                    new PortfolioTransaction(transactionId, portfolioId, TransactionType.DEPOSIT,
                            null, BigDecimal.ZERO, null, negativeAmount, null));
        }
    }

    @Nested
    @DisplayName("Equality and String Representation Tests")
    class EqualityAndStringTests {

        @Test
        @DisplayName("Should have proper equality based on ID")
        void shouldHaveProperEqualityBasedOnId() {
            // Given
            PortfolioTransaction transaction1 = PortfolioTransaction.createDepositTransaction(
                    transactionId, portfolioId, totalAmount);
            PortfolioTransaction transaction2 = PortfolioTransaction.createDepositTransaction(
                    transactionId, "different-portfolio", totalAmount);
            PortfolioTransaction transaction3 = PortfolioTransaction.createDepositTransaction(
                    "different-id", portfolioId, totalAmount);

            // When & Then
            assertEquals(transaction1, transaction2); // Same ID
            assertNotEquals(transaction1, transaction3); // Different ID
            assertEquals(transaction1.hashCode(), transaction2.hashCode());
        }

        @Test
        @DisplayName("Should have proper string representation for asset transactions")
        void shouldHaveProperStringRepresentationForAssetTransactions() {
            // Given
            PortfolioTransaction transaction = PortfolioTransaction.createBuyTransaction(
                    transactionId, portfolioId, testSymbol, new BigDecimal("0.1"), testPrice, "order-123");

            // When
            String result = transaction.toString();

            // Then
            assertTrue(result.contains(transactionId));
            assertTrue(result.contains("Asset Purchase")); // Changed from "BUY" to the actual description
            assertTrue(result.contains("0.1"));
            assertTrue(result.contains("BTC"));
            assertTrue(result.contains(testPrice.toString()));

            // Debug output to see actual format
            System.out.println("Asset transaction toString: " + result);
        }

        @Test
        @DisplayName("Should have proper string representation for cash transactions")
        void shouldHaveProperStringRepresentationForCashTransactions() {
            // Given
            PortfolioTransaction transaction = PortfolioTransaction.createDepositTransaction(
                    transactionId, portfolioId, totalAmount);

            // When
            String result = transaction.toString();

            // Then
            assertTrue(result.contains(transactionId));
            assertTrue(result.contains("Cash Deposit")); // Changed from "DEPOSIT" to the actual description
            assertTrue(result.contains(totalAmount.toString()));

            // Debug output to see actual format
            System.out.println("Cash transaction toString: " + result);
        }
    }
}