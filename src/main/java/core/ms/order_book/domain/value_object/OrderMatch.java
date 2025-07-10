package core.ms.order_book.domain.value_object;

import core.ms.order.domain.IBuyOrder;
import core.ms.order.domain.ISellOrder;
import core.ms.shared.domain.Money;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class OrderMatch {
    private final IBuyOrder buyOrder;
    private final ISellOrder sellOrder;
    private final BigDecimal matchableQuantity;
    private final Money suggestedPrice;
    private final LocalDateTime timestamp;

    public OrderMatch(IBuyOrder buyOrder, ISellOrder sellOrder) {
        this.buyOrder = Objects.requireNonNull(buyOrder, "Buy order cannot be null");
        this.sellOrder = Objects.requireNonNull(sellOrder, "Sell order cannot be null");

        validateMatchCompatibility();

        this.matchableQuantity = calculateMatchableQuantity();
        this.suggestedPrice = calculatePriceTimePriority(); // Changed to use price-time priority
        this.timestamp = LocalDateTime.now();
    }

    public IBuyOrder getBuyOrder() {
        return buyOrder;
    }

    public ISellOrder getSellOrder() {
        return sellOrder;
    }

    public BigDecimal getMatchableQuantity() {
        return matchableQuantity;
    }

    public Money getSuggestedPrice() {
        return suggestedPrice;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean isValid() {
        return matchableQuantity.compareTo(BigDecimal.ZERO) > 0 &&
                buyOrder.isActive() &&
                sellOrder.isActive();
    }

    public Money getTotalValue() {
        return suggestedPrice.multiply(matchableQuantity);
    }

    private BigDecimal calculateMatchableQuantity() {
        BigDecimal buyRemaining = buyOrder.getRemainingQuantity();
        BigDecimal sellRemaining = sellOrder.getRemainingQuantity();
        return buyRemaining.min(sellRemaining);
    }

    /**
     * Calculates execution price using midpoint pricing for fair execution.
     * This method provides equal price improvement to both buyer and seller.
     *
     * @return midpoint price between buy and sell orders
     */
    private Money calculateMidpointPrice() {
        Money buyPrice = buyOrder.getPrice();
        Money sellPrice = sellOrder.getPrice();
        return buyPrice.add(sellPrice).divide(new BigDecimal("2"));
    }

    /**
     * Calculates execution price using price-time priority rules.
     * The order that arrived first (resting order) gets filled at their preferred price.
     * This reflects real-world market behavior where:
     * - Resting orders get price protection
     * - Aggressive orders pay the market price
     * - Time priority rewards early orders
     *
     * @return execution price based on which order has time priority
     */
    private Money calculatePriceTimePriority() {
        LocalDateTime buyOrderTime = buyOrder.getCreatedAt();
        LocalDateTime sellOrderTime = sellOrder.getCreatedAt();

        // Whoever was there first gets their preferred price
        if (buyOrderTime.isBefore(sellOrderTime)) {
            // Buy order was resting, seller is aggressive
            // Seller accepts buyer's price
            return buyOrder.getPrice();
        } else if (sellOrderTime.isBefore(buyOrderTime)) {
            // Sell order was resting, buyer is aggressive
            // Buyer pays seller's price
            return sellOrder.getPrice();
        } else {
            // Same timestamp (rare edge case) - fall back to midpoint for fairness
            return calculateMidpointPrice();
        }
    }

    /**
     * Alternative price-time priority that considers order aggressiveness.
     * An aggressive order is one that crosses the spread to make a trade happen.
     *
     * @param buyOrderIsAggressive whether the buy order crossed the spread
     * @return execution price based on aggressor pays principle
     */
    public Money calculatePriceWithAggressor(boolean buyOrderIsAggressive) {
        if (buyOrderIsAggressive) {
            // Buyer is aggressive (lifted the ask), pays seller's price
            return sellOrder.getPrice();
        } else {
            // Seller is aggressive (hit the bid), accepts buyer's price
            return buyOrder.getPrice();
        }
    }

    private void validateMatchCompatibility() {
        if (!buyOrder.getSymbol().equals(sellOrder.getSymbol())) {
            throw new IllegalArgumentException("Orders must have the same symbol");
        }

        if (buyOrder.getPrice().isLessThan(sellOrder.getPrice())) {
            throw new IllegalArgumentException(
                    "Buy price " + buyOrder.getPrice() + " must be >= sell price " + sellOrder.getPrice());
        }

        if (!buyOrder.isActive() || !sellOrder.isActive()) {
            throw new IllegalArgumentException("Both orders must be active");
        }
    }
}