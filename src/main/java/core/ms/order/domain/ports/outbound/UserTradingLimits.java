package core.ms.order.domain.ports.outbound;

import core.ms.shared.domain.Money;
import java.math.BigDecimal;

/**
 * Value object representing user trading limits
 */
public class UserTradingLimits {
    private final Money maxOrderValue;
    private final BigDecimal maxOrderQuantity;
    private final Money dailyTradingLimit;
    private final int maxActiveOrders;

    public UserTradingLimits(Money maxOrderValue, BigDecimal maxOrderQuantity,
                             Money dailyTradingLimit, int maxActiveOrders) {
        this.maxOrderValue = maxOrderValue;
        this.maxOrderQuantity = maxOrderQuantity;
        this.dailyTradingLimit = dailyTradingLimit;
        this.maxActiveOrders = maxActiveOrders;
    }

    // Getters
    public Money getMaxOrderValue() { return maxOrderValue; }
    public BigDecimal getMaxOrderQuantity() { return maxOrderQuantity; }
    public Money getDailyTradingLimit() { return dailyTradingLimit; }
    public int getMaxActiveOrders() { return maxActiveOrders; }
}
