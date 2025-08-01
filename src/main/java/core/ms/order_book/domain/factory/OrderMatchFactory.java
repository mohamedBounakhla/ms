package core.ms.order_book.domain.factory;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order_book.domain.entities.OrderBook;
import core.ms.order_book.domain.value_object.*;
import core.ms.shared.domain.Money;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class OrderMatchFactory {

    private static final MatchingStrategy DEFAULT_STRATEGY = new PriceTimePriorityMatching();

    /**
     * Finds matches using bid and ask managers with default strategy.
     */
    public static List<OrderMatch> findMatches(BidSideManager bidSide, AskSideManager askSide) {
        return findMatches(bidSide, askSide, DEFAULT_STRATEGY);
    }

    /**
     * Finds matches using bid and ask managers with specific strategy.
     */
    public static List<OrderMatch> findMatches(BidSideManager bidSide, AskSideManager askSide, MatchingStrategy strategy) {
        Objects.requireNonNull(bidSide, "BidSideManager cannot be null");
        Objects.requireNonNull(askSide, "AskSideManager cannot be null");
        Objects.requireNonNull(strategy, "Strategy cannot be null");

        // Get best orders from each side
        Optional<IBuyOrder> bestBuy = bidSide.getBestOrder();
        Optional<ISellOrder> bestSell = askSide.getBestOrder();

        if (bestBuy.isPresent() && bestSell.isPresent()) {
            // Use strategy to find match candidates
            List<? extends MatchCandidateExtractor> candidates = strategy.findMatchCandidates(bestBuy.get(), bestSell.get());

            // Convert valid candidates to OrderMatch instances
            return candidates.stream()
                    .filter(MatchCandidateExtractor::isValid)
                    .map(OrderMatchFactory::createOrderMatch)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    /**
     * Creates OrderMatch from valid candidate.
     * Factory handles OrderMatch creation, not the strategy.
     */
    public static OrderMatch createOrderMatch(MatchCandidateExtractor candidate) {
        // Price is always set by the seller (business rule)
        Money executionPrice = candidate.getSellOrder().getPrice();
        BigDecimal quantity = candidate.getBuyOrder().getRemainingQuantity()
                .min(candidate.getSellOrder().getRemainingQuantity());

        return new OrderMatch(candidate.getBuyOrder(), candidate.getSellOrder(), quantity, executionPrice);
    }
}