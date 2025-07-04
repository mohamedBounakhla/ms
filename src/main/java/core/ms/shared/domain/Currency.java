package core.ms.shared.domain;

import java.math.BigDecimal;

public enum Currency {
    USD("$", 2, "US Dollar", ArithmeticStrategy.ROUND_TO_CURRENCY_PRECISION),
    EUR("€", 2, "Euro", ArithmeticStrategy.ROUND_TO_CURRENCY_PRECISION),
    GBP("£", 2, "British Pound", ArithmeticStrategy.ROUND_TO_CURRENCY_PRECISION),
    JPY("¥", 0, "Japanese Yen", ArithmeticStrategy.ROUND_TO_WHOLE_NUMBERS),
    BTC("₿", 8, "Bitcoin", ArithmeticStrategy.PRESERVE_FULL_PRECISION),
    ETH("Ξ", 18, "Ethereum", ArithmeticStrategy.PRESERVE_FULL_PRECISION);

    private final String symbol;
    private final int decimalPlaces; // For display formatting only
    private final String displayName;
    private final ArithmeticStrategy arithmeticStrategy;
    Currency(String symbol, int decimalPlaces, String displayName, ArithmeticStrategy arithmeticStrategy) {
        this.symbol = symbol;
        this.decimalPlaces = decimalPlaces;
        this.displayName = displayName;
        this.arithmeticStrategy =arithmeticStrategy;
    }
    public BigDecimal processArithmeticResult(BigDecimal result) {
        return arithmeticStrategy.processResult(result, this);
    }
    public String getSymbol() {
        return symbol;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isFiat() {
        return this == USD || this == EUR || this == GBP || this == JPY;
    }

    public boolean isCrypto() {
        return this == BTC || this == ETH;
    }

    @Override
    public String toString() {
        return this.name();
    }
}