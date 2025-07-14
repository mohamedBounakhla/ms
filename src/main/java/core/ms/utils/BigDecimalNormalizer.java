package core.ms.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

public class BigDecimalNormalizer {

    /**
     * Normalizes a BigDecimal by ensuring zero values are represented as BigDecimal.ZERO,
     * but preserves the original scale/precision for all non-zero values
     */
    public static BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        // If the value is zero, always return BigDecimal.ZERO (scale = 0)
        // This ensures "0.0", "0.00", etc. all display as "0"
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // For non-zero values, return as-is to preserve original precision
        return value;
    }

    /**
     * Normalizes and rounds a BigDecimal to a specific scale if needed
     * Only affects non-zero values
     */
    public static BigDecimal normalize(BigDecimal value, int maxScale) {
        // First check if it's zero
        if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // For non-zero values, only apply scaling if the current scale exceeds maxScale
        if (value.scale() > maxScale) {
            return value.setScale(maxScale, RoundingMode.HALF_UP);
        }

        return value;
    }

    /**
     * Creates a normalizing function that can be used in streams or method references
     */
    public static Function<BigDecimal, BigDecimal> normalizer() {
        return BigDecimalNormalizer::normalize;
    }

    /**
     * Creates a normalizing function with max scale
     */
    public static Function<BigDecimal, BigDecimal> normalizer(int maxScale) {
        return value -> normalize(value, maxScale);
    }

    /**
     * Normalizes arithmetic operations result
     * Handles both zero normalization and floating-point precision errors
     */
    public static BigDecimal normalizeArithmetic(BigDecimal result) {
        if (result == null) {
            return BigDecimal.ZERO;
        }

        // For very small values close to zero due to floating-point errors, consider them as zero
        BigDecimal threshold = new BigDecimal("0.00000001"); // 1e-8
        if (result.abs().compareTo(threshold) < 0) {
            return BigDecimal.ZERO;
        }

        // If the result is exactly zero, normalize it
        if (result.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // For non-zero values, preserve original precision
        return result;
    }

    /**
     * Normalizes only trailing zeros while preserving meaningful precision
     * This method strips trailing zeros but keeps the value's significant digits
     */
    public static BigDecimal normalizeTrailingZeros(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return value.stripTrailingZeros();
    }
}

