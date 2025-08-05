package core.ms.shared.domain;

import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Money Value Object Tests")
class MoneyTest {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create money with BigDecimal amount")
        void shouldCreateMoneyWithBigDecimal() {
            // Given
            BigDecimal amount = new BigDecimal("100.50");
            Currency currency = Currency.USD;

            // When
            Money money = new Money(amount, currency);

            // Then
            assertEquals(amount, money.getAmount());
            assertEquals(currency, money.getCurrency());
        }

        @Test
        @DisplayName("Should create money with String amount")
        void shouldCreateMoneyWithString() {
            // Given
            String amount = "100.50";
            Currency currency = Currency.USD;

            // When
            Money money = new Money(amount, currency);

            // Then
            assertEquals(new BigDecimal("100.50"), money.getAmount());
            assertEquals(currency, money.getCurrency());
        }

        @Test
        @DisplayName("Should preserve full precision without rounding")
        void shouldPreserveFullPrecision() {
            // Given
            String highPrecisionAmount = "123.123456789012345";
            Currency currency = Currency.USD;

            // When
            Money money = new Money(highPrecisionAmount, currency);

            // Then
            assertEquals(new BigDecimal("123.123456789012345"), money.getAmount());
        }

        @Test
        @DisplayName("Should throw exception for null amount")
        void shouldThrowExceptionForNullAmount() {
            // Given
            BigDecimal amount = null;
            Currency currency = Currency.USD;

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Money(amount, currency)
            );
            assertEquals("Amount cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for null currency")
        void shouldThrowExceptionForNullCurrency() {
            // Given
            BigDecimal amount = new BigDecimal("100");
            Currency currency = null;

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Money(amount, currency)
            );
            assertEquals("Currency cannot be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Arithmetic Operations Tests")
    class ArithmeticOperationsTests {

        @Test
        @DisplayName("Should add money with same currency")
        void shouldAddMoneyWithSameCurrency() {
            // Given
            Money money1 = new Money("100.50", Currency.USD);
            Money money2 = new Money("50.25", Currency.USD);

            // When
            Money result = money1.add(money2);

            // Then
            assertEquals(new BigDecimal("150.75"), result.getAmount());
            assertEquals(Currency.USD, result.getCurrency());
        }

        @Test
        @DisplayName("Should subtract money with same currency")
        void shouldSubtractMoneyWithSameCurrency() {
            // Given
            Money money1 = new Money("100.50", Currency.USD);
            Money money2 = new Money("50.25", Currency.USD);

            // When
            Money result = money1.subtract(money2);

            // Then
            assertEquals(new BigDecimal("50.25"), result.getAmount());
            assertEquals(Currency.USD, result.getCurrency());
        }

        @Test
        @DisplayName("Should multiply money by BigDecimal scalar")
        void shouldMultiplyByBigDecimalScalar() {
            // Given
            Money money = new Money("100.00", Currency.USD);
            BigDecimal scalar = new BigDecimal("2.5");

            // When
            Money result = money.multiply(scalar);

            // Then
            assertEquals(new BigDecimal("250.00"), result.getAmount());
            assertEquals(Currency.USD, result.getCurrency());
        }

        @Test
        @DisplayName("Should multiply money by double scalar")
        void shouldMultiplyByDoubleScalar() {
            // Given
            Money money = new Money("100.00", Currency.USD);
            double scalar = 2.5;

            // When
            Money result = money.multiply(scalar);

            // Then
            assertEquals(new BigDecimal("250.00"), result.getAmount());
            assertEquals(Currency.USD, result.getCurrency());
        }

        @Test
        @DisplayName("Should divide money by BigDecimal")
        void shouldDivideByBigDecimal() {
            // Given
            Money money = new Money("100.00", Currency.USD);
            BigDecimal divisor = new BigDecimal("4");

            // When
            Money result = money.divide(divisor);

            // Then
            assertEquals(new BigDecimal("25.00"), result.getAmount());
            assertEquals(Currency.USD, result.getCurrency());
        }

        @Test
        @DisplayName("Should negate money amount")
        void shouldNegateMoneyAmount() {
            // Given
            Money money = new Money("100.50", Currency.USD);

            // When
            Money result = money.negate();

            // Then
            assertEquals(new BigDecimal("-100.50"), result.getAmount());
            assertEquals(Currency.USD, result.getCurrency());
        }

        @Test
        @DisplayName("Should throw exception when adding different currencies")
        void shouldThrowExceptionWhenAddingDifferentCurrencies() {
            // Given
            Money usdMoney = new Money("100.00", Currency.USD);
            Money eurMoney = new Money("100.00", Currency.EUR);

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> usdMoney.add(eurMoney)
            );
            assertTrue(exception.getMessage().contains("Cannot operate on different currencies"));
        }

        @Test
        @DisplayName("Should throw exception when dividing by zero")
        void shouldThrowExceptionWhenDividingByZero() {
            // Given
            Money money = new Money("100.00", Currency.USD);
            BigDecimal zero = BigDecimal.ZERO;

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> money.divide(zero)
            );
            assertEquals("Cannot divide by zero", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Comparison Operations Tests")
    class ComparisonOperationsTests {

        @Test
        @DisplayName("Should correctly compare greater than")
        void shouldCorrectlyCompareGreaterThan() {
            // Given
            Money money1 = new Money("100.00", Currency.USD);
            Money money2 = new Money("50.00", Currency.USD);

            // When & Then
            assertTrue(money1.isGreaterThan(money2));
            assertFalse(money2.isGreaterThan(money1));
        }

        @Test
        @DisplayName("Should correctly compare less than")
        void shouldCorrectlyCompareLessThan() {
            // Given
            Money money1 = new Money("50.00", Currency.USD);
            Money money2 = new Money("100.00", Currency.USD);

            // When & Then
            assertTrue(money1.isLessThan(money2));
            assertFalse(money2.isLessThan(money1));
        }

        @Test
        @DisplayName("Should correctly identify zero amount")
        void shouldCorrectlyIdentifyZeroAmount() {
            // Given
            Money zeroMoney = new Money("0.00", Currency.USD);
            Money nonZeroMoney = new Money("1.00", Currency.USD);

            // When & Then
            assertTrue(zeroMoney.isZero());
            assertFalse(nonZeroMoney.isZero());
        }

        @Test
        @DisplayName("Should correctly identify positive amount")
        void shouldCorrectlyIdentifyPositiveAmount() {
            // Given
            Money positiveMoney = new Money("100.00", Currency.USD);
            Money negativeMoney = new Money("-100.00", Currency.USD);
            Money zeroMoney = new Money("0.00", Currency.USD);

            // When & Then
            assertTrue(positiveMoney.isPositive());
            assertFalse(negativeMoney.isPositive());
            assertFalse(zeroMoney.isPositive());
        }

        @Test
        @DisplayName("Should correctly identify negative amount")
        void shouldCorrectlyIdentifyNegativeAmount() {
            // Given
            Money positiveMoney = new Money("100.00", Currency.USD);
            Money negativeMoney = new Money("-100.00", Currency.USD);
            Money zeroMoney = new Money("0.00", Currency.USD);

            // When & Then
            assertFalse(positiveMoney.isNegative());
            assertTrue(negativeMoney.isNegative());
            assertFalse(zeroMoney.isNegative());
        }
    }

    @Nested
    @DisplayName("Factory Methods Tests")
    class FactoryMethodsTests {

        @Test
        @DisplayName("Should create zero money")
        void shouldCreateZeroMoney() {
            // When
            Money zeroUsd = Money.zero(Currency.USD);

            // Then
            assertTrue(zeroUsd.isZero());
            assertEquals(Currency.USD, zeroUsd.getCurrency());
        }

        @Test
        @DisplayName("Should create money using of() factory with string")
        void shouldCreateMoneyUsingOfFactoryWithString() {
            // When
            Money money = Money.of("123.45", Currency.EUR);

            // Then
            assertEquals(new BigDecimal("123.45"), money.getAmount());
            assertEquals(Currency.EUR, money.getCurrency());
        }

        @Test
        @DisplayName("Should create USD money using convenience method")
        void shouldCreateUsdMoneyUsingConvenienceMethod() {
            // When
            Money money = Money.usd("100.50");

            // Then
            assertEquals(new BigDecimal("100.50"), money.getAmount());
            assertEquals(Currency.USD, money.getCurrency());
        }

        @Test
        @DisplayName("Should create EUR money using convenience method")
        void shouldCreateEurMoneyUsingConvenienceMethod() {
            // When
            Money money = Money.eur("200.75");

            // Then
            assertEquals(new BigDecimal("200.75"), money.getAmount());
            assertEquals(Currency.EUR, money.getCurrency());
        }
    }

    @Nested
    @DisplayName("Formatting Tests")
    class FormattingTests {

        @Test
        @DisplayName("Should format USD for display with 2 decimal places")
        void shouldFormatUsdForDisplay() {
            // Given
            Money money = new Money("123.456789", Currency.USD);

            // When
            String displayString = money.toDisplayString();

            // Then
            assertEquals("$123.46", displayString);
        }

        @Test
        @DisplayName("Should format JPY for display with 0 decimal places")
        void shouldFormatJpyForDisplay() {
            // Given
            Money money = new Money("123.456789", Currency.JPY);

            // When
            String displayString = money.toDisplayString();

            // Then
            assertEquals("¥123", displayString);
        }

        @Test
        @DisplayName("Should format BTC for display with 8 decimal places")
        void shouldFormatBtcForDisplay() {
            // Given
            Money money = new Money("1.123456789012345", Currency.BTC);

            // When
            String displayString = money.toDisplayString();

            // Then
            assertEquals("₿1.12345679", displayString);
        }

        @Test
        @DisplayName("Should create plain string representation")
        void shouldCreatePlainStringRepresentation() {
            // Given
            Money money = new Money("123.456789", Currency.USD);

            // When
            String plainString = money.toPlainString();

            // Then
            assertEquals("123.456789 USD", plainString);
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when amount and currency are same")
        void shouldBeEqualWhenAmountAndCurrencyAreSame() {
            // Given
            Money money1 = new Money("100.00", Currency.USD);
            Money money2 = new Money("100.00", Currency.USD);

            // When & Then
            assertEquals(money1, money2);
            assertEquals(money1.hashCode(), money2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when amounts differ")
        void shouldNotBeEqualWhenAmountsDiffer() {
            // Given
            Money money1 = new Money("100.00", Currency.USD);
            Money money2 = new Money("200.00", Currency.USD);

            // When & Then
            assertNotEquals(money1, money2);
        }

        @Test
        @DisplayName("Should not be equal when currencies differ")
        void shouldNotBeEqualWhenCurrenciesDiffer() {
            // Given
            Money money1 = new Money("100.00", Currency.USD);
            Money money2 = new Money("100.00", Currency.EUR);

            // When & Then
            assertNotEquals(money1, money2);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            // Given
            Money money = new Money("100.00", Currency.USD);

            // When & Then
            assertNotEquals(money, null);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            // Given
            Money money = new Money("100.00", Currency.USD);
            String notMoney = "100.00 USD";

            // When & Then
            assertNotEquals(money, notMoney);
        }
        @Test
        @DisplayName("Should handle BigDecimal scale differences in Money equals")
        void shouldHandleBigDecimalScaleDifferencesInMoneyEquals() {
            Money money1 = Money.of("45000", Currency.USD);     // scale 0
            Money money2 = Money.of("45000.0", Currency.USD);   // scale 1
            Money money3 = Money.of("45000.00", Currency.USD);  // scale 2

            // All should be equal despite different scales
            assertEquals(money1, money2);
            assertEquals(money2, money3);
            assertEquals(money1, money3);

            // Hash codes should also be equal
            assertEquals(money1.hashCode(), money2.hashCode());
            assertEquals(money2.hashCode(), money3.hashCode());
        }
    }
}