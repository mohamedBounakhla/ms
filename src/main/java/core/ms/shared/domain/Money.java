package core.ms.shared.domain;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class Money {
    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        this.amount = amount; // Keep full precision - no rounding
        this.currency = currency;
    }

    public Money(String amount, Currency currency) {
        this(new BigDecimal(amount), currency);
    }

    // ===== ARITHMETIC OPERATIONS =====

    public Money add(Money other) {
        validateSameCurrency(other);
        BigDecimal result = amount.add(other.amount);
        BigDecimal processedResult = currency.processArithmeticResult(result);
        return new Money(processedResult, currency);
    }

    public Money subtract(Money other) {
        validateSameCurrency(other);
        BigDecimal result = amount.subtract(other.amount);
        BigDecimal processedResult = currency.processArithmeticResult(result);
        return new Money(processedResult, currency);
    }

    public Money multiply(BigDecimal scalar) {
        if (scalar == null) {
            throw new IllegalArgumentException("Scalar cannot be null");
        }
        BigDecimal result = amount.multiply(scalar);
        BigDecimal processedResult = currency.processArithmeticResult(result);
        return new Money(processedResult, currency);
    }

    public Money multiply(double scalar) {
        return multiply(BigDecimal.valueOf(scalar));
    }

    public Money divide(BigDecimal divisor) {
        if (divisor == null) {
            throw new IllegalArgumentException("Divisor cannot be null");
        }
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Cannot divide by zero");
        }
        // First perform division with high precision
        BigDecimal result = amount.divide(divisor, 20, RoundingMode.HALF_UP);
        // Then apply currency-specific processing
        BigDecimal processedResult = currency.processArithmeticResult(result);
        return new Money(processedResult, currency);
    }

    public Money negate() {
        BigDecimal result = amount.negate();
        BigDecimal processedResult = currency.processArithmeticResult(result);
        return new Money(processedResult, currency);
    }

    // ===== COMPARISON OPERATIONS =====

    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return amount.compareTo(other.amount) > 0;
    }

    public boolean isLessThan(Money other) {
        validateSameCurrency(other);
        return amount.compareTo(other.amount) < 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        validateSameCurrency(other);
        return amount.compareTo(other.amount) >= 0;
    }

    public boolean isLessThanOrEqual(Money other) {
        validateSameCurrency(other);
        return amount.compareTo(other.amount) <= 0;
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    // ===== GETTERS =====

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    // ===== FORMATTING =====

    public String toDisplayString() {
        BigDecimal displayAmount = amount.setScale(currency.getDecimalPlaces(), RoundingMode.HALF_UP);
        return currency.getSymbol() + displayAmount.toPlainString();
    }

    public String toPlainString() {
        return amount.toPlainString() + " " + currency.name();
    }

    // ===== FACTORY METHODS =====

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public static Money of(String amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money usd(String amount) {
        return new Money(amount, Currency.USD);
    }

    public static Money eur(String amount) {
        return new Money(amount, Currency.EUR);
    }

    // ===== PRIVATE METHODS =====

    private void validateSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    String.format("Cannot operate on different currencies: %s and %s",
                            currency, other.currency));
        }
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Money money = (Money) obj;
        return amount.compareTo(money.amount) == 0 && currency == money.currency;
    }

    @Override
    public int hashCode() {
        BigDecimal normalizedAmount = amount.stripTrailingZeros();
        return Objects.hash(normalizedAmount, currency);
    }

    @Override
    public String toString() {
        return toDisplayString();
    }

    public Money abs() {
        if (amount.compareTo(BigDecimal.ZERO) >= 0) {
            return this;
        }
        return new Money(amount.abs(), currency);
    }
}
