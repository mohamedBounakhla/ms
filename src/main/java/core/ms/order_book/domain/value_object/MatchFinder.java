package core.ms.order_book.domain.value_object;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order_book.domain.OrderBook;
import core.ms.shared.domain.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class MatchFinder {

    public List<OrderMatch> findMatches(OrderBook orderBook) {
        Objects.requireNonNull(orderBook, "OrderBook cannot be null");

        List<OrderMatch> matches = new ArrayList<>();

        Optional<Money> bestBidPrice = orderBook.getBestBid();
        Optional<Money> bestAskPrice = orderBook.getBestAsk();

        if (bestBidPrice.isPresent() && bestAskPrice.isPresent()) {
            if (bestBidPrice.get().isGreaterThanOrEqual(bestAskPrice.get())) {
                Optional<IBuyOrder> bestBuyOrder = orderBook.getBestBuyOrder();
                Optional<ISellOrder> bestSellOrder = orderBook.getBestSellOrder();

                if (bestBuyOrder.isPresent() && bestSellOrder.isPresent()) {
                    if (canMatch(bestBuyOrder.get(), bestSellOrder.get())) {
                        matches.add(new OrderMatch(bestBuyOrder.get(), bestSellOrder.get()));
                    }
                }
            }
        }

        return matches;
    }

    public boolean canMatch(IBuyOrder buyOrder, ISellOrder sellOrder) {
        Objects.requireNonNull(buyOrder, "Buy order cannot be null");
        Objects.requireNonNull(sellOrder, "Sell order cannot be null");

        return buyOrder.getSymbol().equals(sellOrder.getSymbol()) &&
                buyOrder.getPrice().isGreaterThanOrEqual(sellOrder.getPrice()) &&
                buyOrder.isActive() &&
                sellOrder.isActive() &&
                buyOrder.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0 &&
                sellOrder.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0;
    }

    public Money calculateMatchPrice(Money buyPrice, Money sellPrice) {
        Objects.requireNonNull(buyPrice, "Buy price cannot be null");
        Objects.requireNonNull(sellPrice, "Sell price cannot be null");

        if (buyPrice.isLessThan(sellPrice)) {
            throw new IllegalArgumentException("Buy price must be >= sell price for matching");
        }

        return buyPrice.add(sellPrice).divide(new BigDecimal("2"));
    }

    public BigDecimal calculateMatchQuantity(BigDecimal buyQuantity, BigDecimal sellQuantity) {
        Objects.requireNonNull(buyQuantity, "Buy quantity cannot be null");
        Objects.requireNonNull(sellQuantity, "Sell quantity cannot be null");

        if (buyQuantity.compareTo(BigDecimal.ZERO) <= 0 || sellQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantities must be positive");
        }

        return buyQuantity.min(sellQuantity);
    }
}