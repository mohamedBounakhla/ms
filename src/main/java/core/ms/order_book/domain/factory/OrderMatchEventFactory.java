package core.ms.order_book.domain.factory;

import core.ms.order_book.domain.events.OrderMatchedEvent;
import core.ms.order_book.domain.value_object.*;
import core.ms.shared.money.Money;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OrderMatchEventFactory {

    // Default implementations
    private static final MatchingAlgorithm DEFAULT_ALGORITHM = new TwoPointerMatchingAlgorithm();
    private static final MatchingStrategy DEFAULT_STRATEGY = new PriceTimePriorityMatching();

    /**
     * Creates order matched events using default algorithm and strategy.
     */
    public static List<OrderMatchedEvent> createMatchEvents(BidSideManager bidSide, AskSideManager askSide) {
        return createMatchEvents(bidSide, askSide, DEFAULT_ALGORITHM, DEFAULT_STRATEGY);
    }

    /**
     * Creates order matched events using injected algorithm and strategy.
     * Pure domain logic - no side effects.
     */
    public static List<OrderMatchedEvent> createMatchEvents(
            BidSideManager bidSide,
            AskSideManager askSide,
            MatchingAlgorithm algorithm,
            MatchingStrategy strategy) {

        Objects.requireNonNull(bidSide, "BidSideManager cannot be null");
        Objects.requireNonNull(askSide, "AskSideManager cannot be null");
        Objects.requireNonNull(algorithm, "MatchingAlgorithm cannot be null");
        Objects.requireNonNull(strategy, "MatchingStrategy cannot be null");

        // Find matching candidates
        List<MatchCandidateExtractor> candidates = algorithm.findMatchCandidates(bidSide, askSide, strategy);

        // Convert valid candidates to domain events
        return candidates.stream()
                .filter(MatchCandidateExtractor::isValid)
                .map(OrderMatchEventFactory::createEventFromCandidate)
                .collect(Collectors.toList());
    }

    /**
     * Creates OrderMatchedEvent from valid candidate.
     * Price is always set by the seller (business rule).
     */
    private static OrderMatchedEvent createEventFromCandidate(MatchCandidateExtractor candidate) {
        Money executionPrice = candidate.getSellOrder().getPrice();
        BigDecimal quantity = candidate.getBuyOrder().getRemainingQuantity()
                .min(candidate.getSellOrder().getRemainingQuantity());

        return new OrderMatchedEvent(
                candidate.getBuyOrder().getId(),
                candidate.getSellOrder().getId(),
                candidate.getBuyOrder().getSymbol(),
                quantity,
                executionPrice,
                LocalDateTime.now()
        );
    }

    /**
     * Creates a single OrderMatchedEvent from a pair of orders.
     * Useful for testing or direct event creation.
     */
    public static OrderMatchedEvent createMatchEvent(MatchCandidateExtractor candidate) {
        return createEventFromCandidate(candidate);
    }
}