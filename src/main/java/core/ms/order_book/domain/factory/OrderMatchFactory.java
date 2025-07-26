package core.ms.order_book.domain.factory;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order_book.domain.entities.OrderBook;
import core.ms.order_book.domain.value_object.MatchingStrategy;
import core.ms.order_book.domain.value_object.OrderMatch;
import core.ms.order_book.domain.value_object.PriceTimePriorityMatching;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class OrderMatchFactory {

    // Default strategy - can be injected later if needed
    private static final MatchingStrategy DEFAULT_STRATEGY = new PriceTimePriorityMatching();

    /**
     * Creates matches from an OrderBook using the default strategy.
     * The OrderBook just passes itself and doesn't need to care about the details.
     */
    public static List<OrderMatch> findMatches(OrderBook orderBook) {
        return findMatches(orderBook, DEFAULT_STRATEGY);
    }

    /**
     * Creates matches from an OrderBook using a specific strategy.
     * Pure delegation - the strategy handles ALL matching logic.
     */
    public static List<OrderMatch> findMatches(OrderBook orderBook, MatchingStrategy strategy) {
        Objects.requireNonNull(orderBook, "OrderBook cannot be null");
        Objects.requireNonNull(strategy, "Strategy cannot be null");

        return strategy.findMatches(orderBook);
    }

    /**
     * Attempts to create a single OrderMatch between two orders.
     * Uses the strategy's validation rules to determine if a match should be created.
     */
    public static Optional<OrderMatch> tryCreateMatch(IBuyOrder buyOrder, ISellOrder sellOrder) {
        return tryCreateMatch(buyOrder, sellOrder, DEFAULT_STRATEGY);
    }

    /**
     * Attempts to create a single OrderMatch using a specific strategy.
     * Pure delegation - the strategy validates, factory just creates if valid.
     */
    public static Optional<OrderMatch> tryCreateMatch(IBuyOrder buyOrder, ISellOrder sellOrder, MatchingStrategy strategy) {
        Objects.requireNonNull(buyOrder, "Buy order cannot be null");
        Objects.requireNonNull(sellOrder, "Sell order cannot be null");
        Objects.requireNonNull(strategy, "Strategy cannot be null");

        // Pure delegation - strategy does the validation
        if (strategy.canMatch(buyOrder, sellOrder)) {
            try {
                return Optional.of(new OrderMatch(buyOrder, sellOrder));
            } catch (IllegalArgumentException e) {
                // OrderMatch constructor validation failed
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    /**
     * Builder-style creation with validation feedback.
     * Returns a result that can indicate why a match failed.
     */
    public static MatchCreationResult tryCreateMatchWithResult(IBuyOrder buyOrder, ISellOrder sellOrder) {
        return tryCreateMatchWithResult(buyOrder, sellOrder, DEFAULT_STRATEGY);
    }

    /**
     * Advanced creation method that provides detailed feedback about why matching failed.
     * Useful for debugging and logging.
     */
    public static MatchCreationResult tryCreateMatchWithResult(IBuyOrder buyOrder, ISellOrder sellOrder, MatchingStrategy strategy) {
        Objects.requireNonNull(buyOrder, "Buy order cannot be null");
        Objects.requireNonNull(sellOrder, "Sell order cannot be null");
        Objects.requireNonNull(strategy, "Strategy cannot be null");

        // Use enhanced validation builder that can provide failure reasons
        return new MatchCreationBuilder(buyOrder, sellOrder, strategy)
                .validateAndBuild();
    }

    /**
     * Result wrapper that can contain either a successful match or failure information.
     */
    public static class MatchCreationResult {
        private final OrderMatch match;
        private final String failureReason;
        private final boolean success;

        private MatchCreationResult(OrderMatch match) {
            this.match = match;
            this.failureReason = null;
            this.success = true;
        }

        private MatchCreationResult(String failureReason) {
            this.match = null;
            this.failureReason = failureReason;
            this.success = false;
        }

        public static MatchCreationResult success(OrderMatch match) {
            return new MatchCreationResult(match);
        }

        public static MatchCreationResult failure(String reason) {
            return new MatchCreationResult(reason);
        }

        public boolean isSuccess() {
            return success;
        }

        public Optional<OrderMatch> getMatch() {
            return Optional.ofNullable(match);
        }

        public Optional<String> getFailureReason() {
            return Optional.ofNullable(failureReason);
        }
    }

    /**
     * Internal builder for advanced match creation with detailed feedback.
     */
    private static class MatchCreationBuilder {
        private final IBuyOrder buyOrder;
        private final ISellOrder sellOrder;
        private final MatchingStrategy strategy;

        public MatchCreationBuilder(IBuyOrder buyOrder, ISellOrder sellOrder, MatchingStrategy strategy) {
            this.buyOrder = buyOrder;
            this.sellOrder = sellOrder;
            this.strategy = strategy;
        }

        public MatchCreationResult validateAndBuild() {
            // Use strategy validation first
            if (!strategy.canMatch(buyOrder, sellOrder)) {
                return MatchCreationResult.failure("Strategy validation failed");
            }

            try {
                OrderMatch match = new OrderMatch(buyOrder, sellOrder);
                return MatchCreationResult.success(match);
            } catch (IllegalArgumentException e) {
                return MatchCreationResult.failure("OrderMatch creation failed: " + e.getMessage());
            }
        }
    }
}}