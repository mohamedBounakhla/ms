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
        this.suggestedPrice = calculateSuggestedPrice();
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

    private Money calculateSuggestedPrice() {
        // Mid-point pricing for fair execution
        Money buyPrice = buyOrder.getPrice();
        Money sellPrice = sellOrder.getPrice();
        return buyPrice.add(sellPrice).divide(new BigDecimal("2"));
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
