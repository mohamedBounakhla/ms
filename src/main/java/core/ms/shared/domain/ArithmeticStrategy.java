package core.ms.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum ArithmeticStrategy {
    ROUND_TO_CURRENCY_PRECISION {
        @Override
        public BigDecimal processResult(BigDecimal result, Currency currency) {
            return result.setScale(currency.getDecimalPlaces(), RoundingMode.HALF_UP);
        }
    },
    PRESERVE_FULL_PRECISION {
        @Override
        public BigDecimal processResult(BigDecimal result, Currency currency) {
            return result; // Keep all decimal places
        }
    },
    ROUND_TO_WHOLE_NUMBERS {
        @Override
        public BigDecimal processResult(BigDecimal result, Currency currency) {
            return result.setScale(0, RoundingMode.HALF_UP);
        }
    };

    public abstract BigDecimal processResult(BigDecimal result, Currency currency);
}