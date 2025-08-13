package core.ms.portfolio.domain;

import core.ms.portfolio.domain.aggregates.Portfolio;
import core.ms.portfolio.domain.entities.Position;
import core.ms.portfolio.domain.value.TransactionType;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Portfolio Tests")
class PortfolioTest {

    private String portfolioId;
    private String portfolioName;
    private String userId;
    private Money initialCash;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        portfolioId = "portfolio-123";
        portfolioName = "My Investment Portfolio";
        userId = "user-456";
        initialCash = Money.of("10000.00", Currency.USD);
        portfolio = new Portfolio(portfolioId, portfolioName, userId, initialCash);
    }

    @Nested
    @DisplayName("Portfolio Creation Tests")
    class PortfolioCreationTests {

        @Test
        @DisplayName("Should create portfolio successfully")
        void shouldCreatePortfolioSuccessfully() {
            // Then
            assertEquals(portfolioId, portfolio.getId());
            assertEquals(portfolioName, portfolio.getName());
            assertEquals(userId, portfolio.getUserId());
            assertEquals(initialCash, portfolio.getCashBalance());
            assertEquals(Currency.USD, portfolio.getBaseCurrency());
            assertNotNull(portfolio.getCreatedAt());
            assertNotNull(portfolio.getUpdatedAt());
            assertTrue(portfolio.getPositions().isEmpty());
            assertTrue(portfolio.getTransactions().isEmpty());
        }

        @Test
        @DisplayName("Should throw exception for null required fields")
        void shouldThrowExceptionForNullRequiredFields() {
            // When & Then
            assertThrows(NullPointerException.class, () ->
                    new Portfolio(null, portfolioName, userId, initialCash));

            assertThrows(NullPointerException.class, () ->
                    new Portfolio(portfolioId, null, userId, initialCash));

            assertThrows(NullPointerException.class, () ->
                    new Portfolio(portfolioId, portfolioName, null, initialCash));

            assertThrows(NullPointerException.class, () ->
                    new Portfolio(portfolioId, portfolioName, userId, null));
        }

        @Test
        @DisplayName("Should validate portfolio constraints")
        void shouldValidatePortfolioConstraints() {
            // When & Then - Empty name should throw
            assertThrows(IllegalArgumentException.class, () ->
                    new Portfolio(portfolioId, "", userId, initialCash));

            assertThrows(IllegalArgumentException.class, () ->
                    new Portfolio(portfolioId, "   ", userId, initialCash));

            // When & Then - Negative initial cash should throw
            Money negativeCash = Money.of("-100.00", Currency.USD);
            assertThrows(IllegalArgumentException.class, () ->
                    new Portfolio(portfolioId, portfolioName, userId, negativeCash));
        }
    }

    @Nested
    @DisplayName("Cash Management Tests")
    class CashManagementTests {

        @Test
        @DisplayName("Should deposit cash successfully")
        void shouldDepositCashSuccessfully() {
            // Given
            Money depositAmount = Money.of("5000.00", Currency.USD);
            Money expectedBalance = Money.of("15000.00", Currency.USD);

            // When
            PortfolioTransaction transaction = portfolio.depositCash(depositAmount);

            // Then
            assertEquals(expectedBalance, portfolio.getCashBalance());
            assertNotNull(transaction);
            assertEquals(TransactionType.DEPOSIT, transaction.getType());
            assertEquals(depositAmount, transaction.getTotalAmount());
            assertEquals(1, portfolio.getTransactions().size());
        }

        @Test
        @DisplayName("Should withdraw cash successfully")
        void shouldWithdrawCashSuccessfully() {
            // Given
            Money withdrawalAmount = Money.of("3000.00", Currency.USD);
            Money expectedBalance = Money.of("7000.00", Currency.USD);

            // When
            PortfolioTransaction transaction = portfolio.withdrawCash(withdrawalAmount);

            // Then
            assertEquals(expectedBalance, portfolio.getCashBalance());
            assertNotNull(transaction);
            assertEquals(TransactionType.WITHDRAWAL, transaction.getType());
            assertEquals(withdrawalAmount, transaction.getTotalAmount());
            assertEquals(1, portfolio.getTransactions().size());
        }

        @Test
        @DisplayName("Should validate cash deposit constraints")
        void shouldValidateCashDepositConstraints() {
            // When & Then - Null amount
            assertThrows(NullPointerException.class, () ->
                    portfolio.depositCash(null));

            // When & Then - Zero amount
            assertThrows(IllegalArgumentException.class, () ->
                    portfolio.depositCash(Money.zero(Currency.USD)));

            // When & Then - Negative amount
            assertThrows(IllegalArgumentException.class, () ->
                    portfolio.depositCash(Money.of("-100.00", Currency.USD)));

            // When & Then - Wrong currency
            assertThrows(IllegalArgumentException.class, () ->
                    portfolio.depositCash(Money.of("1000.00", Currency.EUR)));
        }

        @Test
        @DisplayName("Should validate cash withdrawal constraints")
        void shouldValidateCashWithdrawalConstraints() {
            // When & Then - Insufficient funds
            Money largeAmount = Money.of("15000.00", Currency.USD);
            assertThrows(IllegalArgumentException.class, () ->
                    portfolio.withdrawCash(largeAmount));

            // When & Then - Wrong currency
            assertThrows(IllegalArgumentException.class, () ->
                    portfolio.withdrawCash(Money.of("1000.00", Currency.EUR)));
        }

        @Test
        @DisplayName("Should check affordability correctly")
        void shouldCheckAffordabilityCorrectly() {
            // Then
            assertTrue(portfolio.canAfford(Money.of("5000.00", Currency.USD)));
            assertTrue(portfolio.canAfford(Money.of("10000.00", Currency.USD)));
            assertFalse(portfolio.canAfford(Money.of("15000.00", Currency.USD)));
        }
    }

    @Nested
    @DisplayName("Asset Trading Tests")
    class AssetTradingTests {

        @Test
        @DisplayName("Should buy asset successfully")
        void shouldBuyAssetSuccessfully() {
            // Given
            Symbol btcSymbol = Symbol.btcUsd();
            BigDecimal quantity = new BigDecimal("0.2");
            Money price = Money.of("50000.00", Currency.USD);
            String orderId = "order-123";
            Money expectedCost = Money.of("10000.00", Currency.USD);
            Money expectedCashBalance = Money.zero(Currency.USD); // 10000 - 10000

            // When
            PortfolioTransaction transaction = portfolio.buyAsset(btcSymbol, quantity, price, orderId);

            // Then
            assertEquals(expectedCashBalance, portfolio.getCashBalance());
            assertEquals(1, portfolio.getPositions().size());

            Optional<Position> position = portfolio.getPosition(btcSymbol);
            assertTrue(position.isPresent());
            assertEquals(quantity, position.get().getQuantity());
            assertEquals(price, position.get().getAveragePrice());

            assertNotNull(transaction);
            assertEquals(TransactionType.BUY, transaction.getType());
            assertEquals(expectedCost, transaction.getTotalAmount());
            assertEquals(orderId, transaction.getRelatedOrderId());
            assertEquals(1, portfolio.getTransactions().size());
        }

        @Test
        @DisplayName("Should sell asset successfully")
        void shouldSellAssetSuccessfully() {
            // Given - First buy some assets
            Symbol btcSymbol = Symbol.btcUsd();
            BigDecimal buyQuantity = new BigDecimal("0.2");
            Money buyPrice = Money.of("50000.00", Currency.USD);
            portfolio.buyAsset(btcSymbol, buyQuantity, buyPrice, "buy-order");

            // When - Sell half
            BigDecimal sellQuantity = new BigDecimal("0.1");
            Money sellPrice = Money.of("60000.00", Currency.USD);
            String sellOrderId = "sell-order";
            Money expectedProceeds = Money.of("6000.00", Currency.USD);
            Money expectedCashBalance = Money.of("6000.00", Currency.USD); // 0 + 6000

            PortfolioTransaction transaction = portfolio.sellAsset(btcSymbol, sellQuantity, sellPrice, sellOrderId);

            // Then
            assertEquals(expectedCashBalance, portfolio.getCashBalance());

            Optional<Position> position = portfolio.getPosition(btcSymbol);
            assertTrue(position.isPresent());
            assertEquals(new BigDecimal("0.1"), position.get().getQuantity());
            assertEquals(buyPrice, position.get().getAveragePrice()); // Average price unchanged

            assertNotNull(transaction);
            assertEquals(TransactionType.SELL, transaction.getType());
            assertEquals(expectedProceeds, transaction.getTotalAmount());
            assertEquals(sellOrderId, transaction.getRelatedOrderId());
            assertEquals(2, portfolio.getTransactions().size());
        }

        @Test
        @DisplayName("Should sell entire position and remove it")
        void shouldSellEntirePositionAndRemoveIt() {
            // Given - Buy some assets
            Symbol btcSymbol = Symbol.btcUsd();
            BigDecimal quantity = new BigDecimal("0.2");
            Money price = Money.of("50000.00", Currency.USD);
            portfolio.buyAsset(btcSymbol, quantity, price, "buy-order");

            // When - Sell entire position
            portfolio.sellAsset(btcSymbol, quantity, price, "sell-order");

            // Then
            assertTrue(portfolio.getPosition(btcSymbol).isEmpty());
            assertEquals(0, portfolio.getPositions().size());
        }

        @Test
        @DisplayName("Should handle multiple positions correctly")
        void shouldHandleMultiplePositionsCorrectly() {
            // Given
            Symbol btcSymbol = Symbol.btcUsd();
            Symbol ethSymbol = Symbol.ethUsd();

            // When - CORRECTED AMOUNTS to fit within $10,000 budget
            portfolio.buyAsset(btcSymbol, new BigDecimal("0.1"), Money.of("50000.00", Currency.USD), null);  // $5,000
            portfolio.buyAsset(ethSymbol, new BigDecimal("1.5"), Money.of("3000.00", Currency.USD), null);   // $4,500

            // Then
            assertEquals(2, portfolio.getPositions().size());
            assertTrue(portfolio.getPosition(btcSymbol).isPresent());
            assertTrue(portfolio.getPosition(ethSymbol).isPresent());
            assertEquals(Money.of("500.00", Currency.USD), portfolio.getCashBalance()); // 10000 - 5000 - 4500 = 500
        }

        @Test
        @DisplayName("Should validate asset trading constraints")
        void shouldValidateAssetTradingConstraints() {
            Symbol btcSymbol = Symbol.btcUsd();

            // When & Then - Insufficient cash for buying
            Money expensivePrice = Money.of("100000.00", Currency.USD);
            assertThrows(IllegalArgumentException.class, () ->
                    portfolio.buyAsset(btcSymbol, new BigDecimal("0.2"), expensivePrice, null));

            // When & Then - Selling non-existent position
            assertThrows(IllegalArgumentException.class, () ->
                    portfolio.sellAsset(btcSymbol, new BigDecimal("0.1"), Money.of("50000.00", Currency.USD), null));

            // Given - Buy some assets
            portfolio.buyAsset(btcSymbol, new BigDecimal("0.1"), Money.of("50000.00", Currency.USD), null);

            // When & Then - Selling more than available
            assertThrows(IllegalArgumentException.class, () ->
                    portfolio.sellAsset(btcSymbol, new BigDecimal("0.2"), Money.of("50000.00", Currency.USD), null));
        }
    }

    @Nested
    @DisplayName("Portfolio Valuation Tests")
    class PortfolioValuationTests {

        @Test
        @DisplayName("Should calculate total value correctly")
        void shouldCalculateTotalValueCorrectly() {
            // Given
            Symbol btcSymbol = Symbol.btcUsd();
            Symbol ethSymbol = Symbol.ethUsd();

            // CORRECTED AMOUNTS to fit within budget
            portfolio.buyAsset(btcSymbol, new BigDecimal("0.1"), Money.of("50000.00", Currency.USD), null);   // $5,000
            portfolio.buyAsset(ethSymbol, new BigDecimal("1.5"), Money.of("3000.00", Currency.USD), null);    // $4,500

            Map<Symbol, Money> currentPrices = new HashMap<>();
            currentPrices.put(btcSymbol, Money.of("60000.00", Currency.USD)); // BTC up 20%
            currentPrices.put(ethSymbol, Money.of("2500.00", Currency.USD));  // ETH down 16.67%

            // When
            Money totalValue = portfolio.getTotalValue(currentPrices);

            // Then
            // Cash: 500 (10000 - 5000 - 4500)
            // BTC: 0.1 * 60000 = 6000
            // ETH: 1.5 * 2500 = 3750
            // Total: 500 + 6000 + 3750 = 10250
            Money expectedTotalValue = Money.of("10250.00", Currency.USD);
            assertEquals(expectedTotalValue, totalValue);
        }

        @Test
        @DisplayName("Should calculate total profit loss correctly")
        void shouldCalculateTotalProfitLossCorrectly() {
            // Given
            Symbol btcSymbol = Symbol.btcUsd();
            portfolio.buyAsset(btcSymbol, new BigDecimal("0.1"), Money.of("50000.00", Currency.USD), null);

            Map<Symbol, Money> currentPrices = new HashMap<>();
            currentPrices.put(btcSymbol, Money.of("60000.00", Currency.USD)); // 20% gain

            // When
            Money totalProfitLoss = portfolio.getTotalProfitLoss(currentPrices);

            // Then
            Money expectedProfitLoss = Money.of("1000.00", Currency.USD); // (60000 - 50000) * 0.1
            assertEquals(expectedProfitLoss, totalProfitLoss);
        }

        @Test
        @DisplayName("Should calculate total invested correctly")
        void shouldCalculateTotalInvestedCorrectly() {
            // Given
            Symbol btcSymbol = Symbol.btcUsd();
            Symbol ethSymbol = Symbol.ethUsd();

            // CORRECTED AMOUNTS
            portfolio.buyAsset(btcSymbol, new BigDecimal("0.1"), Money.of("50000.00", Currency.USD), null);   // $5,000
            portfolio.buyAsset(ethSymbol, new BigDecimal("1.5"), Money.of("3000.00", Currency.USD), null);    // $4,500

            // When
            Money totalInvested = portfolio.getTotalInvested();

            // Then
            Money expectedInvested = Money.of("9500.00", Currency.USD); // 5000 + 4500
            assertEquals(expectedInvested, totalInvested);
        }
    }

    @Nested
    @DisplayName("Transaction History Tests")
    class TransactionHistoryTests {

        @Test
        @DisplayName("Should get transactions by type correctly")
        void shouldGetTransactionsByTypeCorrectly() {
            // Given
            portfolio.depositCash(Money.of("5000.00", Currency.USD));
            portfolio.withdrawCash(Money.of("1000.00", Currency.USD));
            portfolio.buyAsset(Symbol.btcUsd(), new BigDecimal("0.1"), Money.of("50000.00", Currency.USD), null);

            // When
            List<PortfolioTransaction> cashTransactions = portfolio.getTransactionsByType(TransactionType.DEPOSIT);
            List<PortfolioTransaction> buyTransactions = portfolio.getTransactionsByType(TransactionType.BUY);

            // Then
            assertEquals(1, cashTransactions.size());
            assertEquals(TransactionType.DEPOSIT, cashTransactions.get(0).getType());

            assertEquals(1, buyTransactions.size());
            assertEquals(TransactionType.BUY, buyTransactions.get(0).getType());
        }

        @Test
        @DisplayName("Should get transactions for symbol correctly")
        void shouldGetTransactionsForSymbolCorrectly() {
            // Given
            Symbol btcSymbol = Symbol.btcUsd();
            Symbol ethSymbol = Symbol.ethUsd();

            // CORRECTED AMOUNTS to fit within budget
            portfolio.buyAsset(btcSymbol, new BigDecimal("0.08"), Money.of("50000.00", Currency.USD), null);  // $4,000
            portfolio.buyAsset(ethSymbol, new BigDecimal("1.0"), Money.of("3000.00", Currency.USD), null);    // $3,000
            portfolio.buyAsset(btcSymbol, new BigDecimal("0.05"), Money.of("55000.00", Currency.USD), null);  // $2,750

            // Total spent: $4,000 + $3,000 + $2,750 = $9,750 (within $10,000 budget)

            // When
            List<PortfolioTransaction> btcTransactions = portfolio.getTransactionsForSymbol(btcSymbol);
            List<PortfolioTransaction> ethTransactions = portfolio.getTransactionsForSymbol(ethSymbol);

            // Then
            assertEquals(2, btcTransactions.size());
            assertEquals(1, ethTransactions.size());

            btcTransactions.forEach(tx -> assertEquals(btcSymbol, tx.getSymbol()));
            ethTransactions.forEach(tx -> assertEquals(ethSymbol, tx.getSymbol()));

            // Verify cash balance
            assertEquals(Money.of("250.00", Currency.USD), portfolio.getCashBalance()); // 10000 - 9750
        }
    }

    @Nested
    @DisplayName("Equality and String Representation Tests")
    class EqualityAndStringTests {

        @Test
        @DisplayName("Should have proper equality based on ID")
        void shouldHaveProperEqualityBasedOnId() {
            // Given
            Portfolio portfolio1 = new Portfolio(portfolioId, portfolioName, userId, initialCash);
            Portfolio portfolio2 = new Portfolio(portfolioId, "Different Name", "different-user", initialCash);
            Portfolio portfolio3 = new Portfolio("different-id", portfolioName, userId, initialCash);

            // When & Then
            assertEquals(portfolio1, portfolio2); // Same ID
            assertNotEquals(portfolio1, portfolio3); // Different ID
            assertEquals(portfolio1.hashCode(), portfolio2.hashCode());
        }

        @Test
        @DisplayName("Should have proper string representation")
        void shouldHaveProperStringRepresentation() {
            // When
            String result = portfolio.toString();

            // Then
            assertTrue(result.contains(portfolioId));
            assertTrue(result.contains(portfolioName));
            assertTrue(result.contains(initialCash.toString()));
        }
    }
}