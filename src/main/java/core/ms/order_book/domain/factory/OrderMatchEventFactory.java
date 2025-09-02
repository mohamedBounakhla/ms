package core.ms.order_book.domain.factory;

import core.ms.order_book.domain.events.publish.OrderMatchedEvent;
import core.ms.order_book.domain.value_object.*;
import core.ms.shared.events.EventContext;
import core.ms.shared.money.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OrderMatchEventFactory {

    // Default implementations
    private static final MatchingAlgorithm DEFAULT_ALGORITHM = new TwoPointerMatchingAlgorithm();
    private static final MatchingStrategy DEFAULT_STRATEGY = new PriceTimePriorityMatching();

    /**
     * Creates order matched events using default algorithm and strategy.
     * Uses current correlation ID from EventContext.
     */
    public static List<OrderMatchedEvent> createMatchEvents(BidSideManager bidSide, AskSideManager askSide) {
        return createMatchEvents(bidSide, askSide, DEFAULT_ALGORITHM, DEFAULT_STRATEGY);
    }

    /**
     * Creates order matched events using injected algorithm and strategy.
     * Maintains correlation ID for saga pattern.
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

        // Convert valid candidates to domain events with correlation ID
        return candidates.stream()
                .filter(MatchCandidateExtractor::isValid)
                .map(OrderMatchEventFactory::createEventFromCandidate)
                .collect(Collectors.toList());
    }

    /**
     * Creates OrderMatchedEvent from valid candidate.
     * Price is always set by the seller (business rule).
     * Includes correlation ID for saga tracking.
     */
    private static OrderMatchedEvent createEventFromCandidate(MatchCandidateExtractor candidate) {
        Money executionPrice = candidate.getSellOrder().getPrice();
        BigDecimal matchedQuantity = candidate.getBuyOrder().getRemainingQuantity()
                .min(candidate.getSellOrder().getRemainingQuantity());

        // Get current correlation ID from context (will be set by the incoming event handler)
        String correlationId = EventContext.getCurrentCorrelationId();

        return new OrderMatchedEvent(
                correlationId,
                candidate.getBuyOrder().getId(),
                candidate.getSellOrder().getId(),
                candidate.getBuyOrder().getSymbol(),
                matchedQuantity,
                executionPrice
        );
    }

    /**
     * Creates a single OrderMatchedEvent from a pair of orders with explicit correlation ID.
     * Useful for testing or when correlation ID needs to be explicitly set.
     */
    public static OrderMatchedEvent createMatchEvent(String correlationId, MatchCandidateExtractor candidate) {
        Money executionPrice = candidate.getSellOrder().getPrice();
        BigDecimal matchedQuantity = candidate.getBuyOrder().getRemainingQuantity()
                .min(candidate.getSellOrder().getRemainingQuantity());

        return new OrderMatchedEvent(
                correlationId,
                candidate.getBuyOrder().getId(),
                candidate.getSellOrder().getId(),
                candidate.getBuyOrder().getSymbol(),
                matchedQuantity,
                executionPrice
        );
    }
}