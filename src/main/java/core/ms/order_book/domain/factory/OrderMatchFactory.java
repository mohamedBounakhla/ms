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

    // Default implementations
    private static final MatchingAlgorithm DEFAULT_ALGORITHM = new TwoPointerMatchingAlgorithm();
    private static final MatchingStrategy DEFAULT_STRATEGY = new PriceTimePriorityMatching();

    /**
     * Finds matches using default algorithm and strategy.
     */
    public static List<OrderMatch> findMatches(BidSideManager bidSide, AskSideManager askSide) {
        return findMatches(bidSide, askSide, DEFAULT_ALGORITHM, DEFAULT_STRATEGY);
    }

    /**
     * Finds matches using injected algorithm and strategy.
     * Pure composition - both concerns are injected abstractions.
     */
    public static List<OrderMatch> findMatches(
            BidSideManager bidSide,
            AskSideManager askSide,
            MatchingAlgorithm algorithm,
            MatchingStrategy strategy) {

        Objects.requireNonNull(bidSide, "BidSideManager cannot be null");
        Objects.requireNonNull(askSide, "AskSideManager cannot be null");
        Objects.requireNonNull(algorithm, "MatchingAlgorithm cannot be null");
        Objects.requireNonNull(strategy, "MatchingStrategy cannot be null");

        // Pure orchestration - delegate to injected components
        List<MatchCandidateExtractor> candidates = algorithm.findMatchCandidates(bidSide, askSide, strategy);

        // Convert valid candidates to OrderMatch instances
        return candidates.stream()
                .filter(MatchCandidateExtractor::isValid)
                .map(OrderMatchFactory::createOrderMatch)
                .collect(Collectors.toList());
    }

    /**
     * Creates OrderMatch from valid candidate.
     * Price is always set by the seller (business rule).
     */
    public static OrderMatch createOrderMatch(MatchCandidateExtractor candidate) {
        // Price is always set by the seller (business rule)
        Money executionPrice = candidate.getSellOrder().getPrice();
        BigDecimal quantity = candidate.getBuyOrder().getRemainingQuantity()
                .min(candidate.getSellOrder().getRemainingQuantity());

        return new OrderMatch(candidate.getBuyOrder(), candidate.getSellOrder(), quantity, executionPrice);
    }
}