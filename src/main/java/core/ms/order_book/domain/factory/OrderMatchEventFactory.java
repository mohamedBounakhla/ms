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

        System.out.println("DEBUG OrderMatchEventFactory: Starting match event creation");

        // Find matching candidates
        List<MatchCandidateExtractor> candidates = algorithm.findMatchCandidates(bidSide, askSide, strategy);

        System.out.println("DEBUG OrderMatchEventFactory: Found " + candidates.size() + " candidates");

        // Convert valid candidates to domain events with correlation ID
        List<OrderMatchedEvent> events = candidates.stream()
                .filter(MatchCandidateExtractor::isValid)
                .map(OrderMatchEventFactory::createEventFromCandidate)
                .filter(Objects::nonNull)  // Filter out null events
                .collect(Collectors.toList());

        System.out.println("DEBUG OrderMatchEventFactory: Created " + events.size() + " match events");

        return events;
    }

    /**
     * Creates OrderMatchedEvent from valid candidate.
     * Price is always set by the seller (business rule).
     * Uses CONSERVATIVE quantity calculation to prevent overmatching.
     */
    private static OrderMatchedEvent createEventFromCandidate(MatchCandidateExtractor candidate) {
        if (!candidate.isValid()) {
            System.out.println("DEBUG OrderMatchEventFactory: Invalid candidate, skipping");
            return null;
        }

        Money executionPrice = candidate.getSellOrder().getPrice();

        // CONSERVATIVE: Use the minimum of both remaining quantities
        BigDecimal buyRemaining = candidate.getBuyOrder().getRemainingQuantity();
        BigDecimal sellRemaining = candidate.getSellOrder().getRemainingQuantity();
        BigDecimal matchedQuantity = buyRemaining.min(sellRemaining);

        // Log the candidate details
        System.out.println("DEBUG OrderMatchEventFactory: Processing candidate - Buy: " +
                candidate.getBuyOrder().getId() + " (remaining: " + buyRemaining +
                ", status: " + candidate.getBuyOrder().getStatus().getStatus() +
                "), Sell: " + candidate.getSellOrder().getId() +
                " (remaining: " + sellRemaining +
                ", status: " + candidate.getSellOrder().getStatus().getStatus() + ")");

        // Additional safety check - ensure positive quantity
        if (matchedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("WARNING: Skipping match with zero/negative quantity");
            return null;
        }

        // Get current correlation ID from context (will be set by the incoming event handler)
        String correlationId = EventContext.getCurrentCorrelationId();
        if (correlationId == null) {
            correlationId = "ORDERBOOK-" + System.currentTimeMillis();
            System.out.println("WARNING: No correlation ID in context, using: " + correlationId);
        }

        System.out.println("DEBUG: Creating match event with quantity: " + matchedQuantity +
                ", correlation ID: " + correlationId);

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
        if (!candidate.isValid()) {
            return null;
        }

        Money executionPrice = candidate.getSellOrder().getPrice();

        // CONSERVATIVE: Use the minimum of both remaining quantities
        BigDecimal buyRemaining = candidate.getBuyOrder().getRemainingQuantity();
        BigDecimal sellRemaining = candidate.getSellOrder().getRemainingQuantity();
        BigDecimal matchedQuantity = buyRemaining.min(sellRemaining);

        // Safety check
        if (matchedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

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