package core.ms.portfolio.domain;

import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Position Tests")
class PositionTest {

    private String positionId;
    private String portfolioId;
    private Symbol testSymbol;
    private BigDecimal initialQuantity;
    private Money initialPrice;
    private Position position;

    @BeforeEach
    void setUp() {
        positionId = "pos-123";
        portfolioId = "portfolio-456";
        testSymbol = Symbol.btcUsd();
        initialQuantity = new BigDecimal("1.0");
        initialPrice = Money.of("50000.00", Currency.USD);
        position = new Position(positionId, portfolioId, testSymbol, initialQuantity, initialPrice);
    }

    @Nested
    @DisplayName("Position Creation Tests")
    class PositionCreationTests {

        @Test
        @DisplayName("Should create position successfully")
        void shouldCreatePositionSuccessfully() {
            // Then
            assertEquals(positionId, position.getId());
            assertEquals(portfolioId, position.getPortfolioId());
            assertEquals(testSymbol, position.getSymbol());
            assertEquals(initialQuantity, position.getQuantity());
            assertEquals(initialPrice, position.getAveragePrice());
            assertNotNull(position.getCreatedAt());
            assertNotNull(position.getUpdatedAt());
            assertEquals("BTC", position.getSymbolCode());
        }

        @Test
        @DisplayName("Should throw exception for null required fields")
        void shouldThrowExceptionForNullRequiredFields() {
            // When & Then
            assertThrows(NullPointerException.class, () ->
                    new Position(null, portfolioId, testSymbol, initialQuantity, initialPrice));

            assertThrows(NullPointerException.class, () ->
                    new Position(positionId, null, testSymbol, initialQuantity, initialPrice));

            assertThrows(NullPointerException.class, () ->
                    new Position(positionId, portfolioId, null, initialQuantity, initialPrice));

            assertThrows(NullPointerException.class, () ->
                    new Position(positionId, portfolioId, testSymbol, null, initialPrice));

            assertThrows(NullPointerException.class, () ->
                    new Position(positionId, portfolioId, testSymbol, initialQuantity, null));
        }

        @Test
        @DisplayName("Should validate quantity and price constraints")
        void shouldValidateQuantityAndPriceConstraints() {
            // When & Then - Negative quantity should throw
            assertThrows(IllegalArgumentException.class, () ->
                    new Position(positionId, portfolioId, testSymbol, new BigDecimal("-1.0"), initialPrice));

            // When & Then - Negative price should throw
            Money negativePrice = Money.of("-100.00", Currency.USD);
            assertThrows(IllegalArgumentException.class, () ->
                    new Position(positionId, portfolioId, testSymbol, initialQuantity, negativePrice));
        }

        @Test
        @DisplayName("Should allow zero quantity position")
        void shouldAllowZeroQuantityPosition() {
            // When
            Position zeroPosition = new Position(positionId, portfolioId, testSymbol, BigDecimal.ZERO, initialPrice);

            // Then
            assertEquals(BigDecimal.ZERO, zeroPosition.getQuantity());
            assertTrue(zeroPosition.isEmpty());
        }
    }

    @Nested
    @DisplayName("Value Calculation Tests")
    class ValueCalculationTests {

        @Test
        @DisplayName("Should calculate current value correctly")
        void shouldCalculateCurrentValueCorrectly() {
            // Given
            Money currentPrice = Money.of("60000.00", Currency.USD);

            // When
            Money currentValue = position.getCurrentValue(currentPrice);

            // Then
            Money expectedValue = currentPrice.multiply(initialQuantity);
            assertEquals(expectedValue, currentValue);
        }

        @Test
        @DisplayName("Should calculate cost basis correctly")
        void shouldCalculateCostBasisCorrectly() {
            // When
            Money costBasis = position.getCostBasis();

            // Then
            Money expectedCostBasis = initialPrice.multiply(initialQuantity);
            assertEquals(expectedCostBasis, costBasis);
        }

        @Test
        @DisplayName("Should calculate profit correctly")
        void shouldCalculateProfitCorrectly() {
            // Given
            Money currentPrice = Money.of("60000.00", Currency.USD); // Price increased

            // When
            Money profitLoss = position.getProfitLoss(currentPrice);

            // Then
            Money expectedProfit = Money.of("10000.00", Currency.USD); // (60000 - 50000) * 1.0
            assertEquals(expectedProfit, profitLoss);
            assertTrue(profitLoss.isPositive());
        }

        @Test
        @DisplayName("Should calculate loss correctly")
        void shouldCalculateLossCorrectly() {
            // Given
            Money currentPrice = Money.of("40000.00", Currency.USD); // Price decreased

            // When
            Money profitLoss = position.getProfitLoss(currentPrice);

            // Then
            Money expectedLoss = Money.of("-10000.00", Currency.USD); // (40000 - 50000) * 1.0
            assertEquals(expectedLoss, profitLoss);
            assertTrue(profitLoss.isNegative());
        }

        @Test
        @DisplayName("Should calculate profit loss percentage correctly")
        void shouldCalculateProfitLossPercentageCorrectly() {
            // Given
            Money currentPrice = Money.of("60000.00", Currency.USD); // 20% increase

            // When
            BigDecimal profitLossPercentage = position.getProfitLossPercentage(currentPrice);

            // Then
            assertEquals(new BigDecimal("20.0000"), profitLossPercentage);
        }

        @Test
        @DisplayName("Should handle zero cost basis for percentage calculation")
        void shouldHandleZeroCostBasisForPercentageCalculation() {
            // Given
            Position zeroPosition = new Position(positionId, portfolioId, testSymbol,
                    BigDecimal.ZERO, Money.zero(Currency.USD));
            Money currentPrice = Money.of("60000.00", Currency.USD);

            // When
            BigDecimal profitLossPercentage = zeroPosition.getProfitLossPercentage(currentPrice);

            // Then
            assertEquals(BigDecimal.ZERO, profitLossPercentage);
        }

        @Test
        @DisplayName("Should validate currency consistency in calculations")
        void shouldValidateCurrencyConsistencyInCalculations() {
            // Given
            Money wrongCurrencyPrice = Money.of("50000.00", Currency.EUR);

            // When & Then
            assertThrows(IllegalArgumentException.class, () ->
                    position.getCurrentValue(wrongCurrencyPrice));

            assertThrows(IllegalArgumentException.class, () ->
                    position.getProfitLoss(wrongCurrencyPrice));

            assertThrows(IllegalArgumentException.class, () ->
                    position.getProfitLossPercentage(wrongCurrencyPrice));
        }
    }

    @Nested
    @DisplayName("Quantity Management Tests")
    class QuantityManagementTests {

        @Test
        @DisplayName("Should add quantity and update average price correctly")
        void shouldAddQuantityAndUpdateAveragePriceCorrectly() {
            // Given
            BigDecimal additionalQuantity = new BigDecimal("1.0");
            Money newPrice = Money.of("60000.00", Currency.USD);

            // When
            position.addQuantity(additionalQuantity, newPrice);

            // Then
            assertEquals(new BigDecimal("2.0"), position.getQuantity());
            // Weighted average: ((1.0 * 50000) + (1.0 * 60000)) / 2.0 = 55000
            Money expectedAveragePrice = Money.of("55000.00", Currency.USD);
            assertEquals(expectedAveragePrice, position.getAveragePrice());
        }

        @Test
        @DisplayName("Should add quantity with complex weighted average")
        void shouldAddQuantityWithComplexWeightedAverage() {
            // Given - Start with 2.0 BTC at $50,000 avg
            position.addQuantity(new BigDecimal("1.0"), Money.of("50000.00", Currency.USD));

            // When - Add 1.0 BTC at $70,000
            position.addQuantity(new BigDecimal("1.0"), Money.of("70000.00", Currency.USD));

            // Then
            assertEquals(new BigDecimal("3.0"), position.getQuantity());
            // Weighted average: ((2.0 * 50000) + (1.0 * 70000)) / 3.0 = 56666.67
            Money expectedAveragePrice = Money.of("56666.67", Currency.USD);
            assertEquals(expectedAveragePrice.getAmount().setScale(2),
                    position.getAveragePrice().getAmount().setScale(2));
        }

        @Test
        @DisplayName("Should remove quantity correctly")
        void shouldRemoveQuantityCorrectly() {
            // Given
            BigDecimal quantityToRemove = new BigDecimal("0.5");

            // When
            position.removeQuantity(quantityToRemove);

            // Then
            assertEquals(new BigDecimal("0.5"), position.getQuantity());
            assertEquals(initialPrice, position.getAveragePrice()); // Average price unchanged
            assertFalse(position.isEmpty());
        }

        @Test
        @DisplayName("Should remove all quantity and become empty")
        void shouldRemoveAllQuantityAndBecomeEmpty() {
            // When
            position.removeQuantity(initialQuantity);

            // Then
            assertEquals(BigDecimal.ZERO, position.getQuantity());
            assertTrue(position.isEmpty());
        }

        @Test
        @DisplayName("Should validate quantity addition constraints")
        void shouldValidateQuantityAdditionConstraints() {
            // When & Then - Null quantity
            assertThrows(NullPointerException.class, () ->
                    position.addQuantity(null, initialPrice));

            // When & Then - Null price
            assertThrows(NullPointerException.class, () ->
                    position.addQuantity(new BigDecimal("1.0"), null));

            // When & Then - Zero quantity
            assertThrows(IllegalArgumentException.class, () ->
                    position.addQuantity(BigDecimal.ZERO, initialPrice));

            // When & Then - Negative quantity
            assertThrows(IllegalArgumentException.class, () ->
                    position.addQuantity(new BigDecimal("-0.5"), initialPrice));

            // When & Then - Wrong currency
            Money wrongCurrencyPrice = Money.of("60000.00", Currency.EUR);
            assertThrows(IllegalArgumentException.class, () ->
                    position.addQuantity(new BigDecimal("1.0"), wrongCurrencyPrice));
        }

        @Test
        @DisplayName("Should validate quantity removal constraints")
        void shouldValidateQuantityRemovalConstraints() {
            // When & Then - Null quantity
            assertThrows(NullPointerException.class, () ->
                    position.removeQuantity(null));

            // When & Then - Zero quantity
            assertThrows(IllegalArgumentException.class, () ->
                    position.removeQuantity(BigDecimal.ZERO));

            // When & Then - Negative quantity
            assertThrows(IllegalArgumentException.class, () ->
                    position.removeQuantity(new BigDecimal("-0.5")));

            // When & Then - Insufficient quantity
            assertThrows(IllegalArgumentException.class, () ->
                    position.removeQuantity(new BigDecimal("2.0"))); // Only have 1.0
        }

        @Test
        @DisplayName("Should check sufficient quantity correctly")
        void shouldCheckSufficientQuantityCorrectly() {
            // Then
            assertTrue(position.hasSufficientQuantity(new BigDecimal("0.5")));
            assertTrue(position.hasSufficientQuantity(new BigDecimal("1.0")));
            assertFalse(position.hasSufficientQuantity(new BigDecimal("1.5")));
        }
    }

    @Nested
    @DisplayName("Equality and String Representation Tests")
    class EqualityAndStringTests {

        @Test
        @DisplayName("Should have proper equality based on ID")
        void shouldHaveProperEqualityBasedOnId() {
            // Given
            Position position1 = new Position(positionId, portfolioId, testSymbol, initialQuantity, initialPrice);
            Position position2 = new Position(positionId, "different-portfolio", testSymbol, initialQuantity, initialPrice);
            Position position3 = new Position("different-id", portfolioId, testSymbol, initialQuantity, initialPrice);

            // When & Then
            assertEquals(position1, position2); // Same ID
            assertNotEquals(position1, position3); // Different ID
            assertEquals(position1.hashCode(), position2.hashCode());
        }

        @Test
        @DisplayName("Should have proper string representation")
        void shouldHaveProperStringRepresentation() {
            // When
            String result = position.toString();

            // Then
            assertTrue(result.contains(positionId));
            assertTrue(result.contains("1"));
            assertTrue(result.contains("BTC"));
            assertTrue(result.contains(initialPrice.toString()));
        }
    }
}