package core.ms.order_book.domain.value_object;
import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;

import java.util.*;

public class TwoPointerMatchingAlgorithm implements MatchingAlgorithm {

    @Override
    public List<MatchCandidateExtractor> findMatchCandidates(
            BidSideManager bidSide,
            AskSideManager askSide,
            MatchingStrategy strategy) {

        Objects.requireNonNull(bidSide, "BidSideManager cannot be null");
        Objects.requireNonNull(askSide, "AskSideManager cannot be null");
        Objects.requireNonNull(strategy, "MatchingStrategy cannot be null");

        // Get sorted levels from managers
        List<BidPriceLevel> bidLevels = new ArrayList<>(bidSide.getLevels());
        List<AskPriceLevel> askLevels = new ArrayList<>(askSide.getLevels());

        if (bidLevels.isEmpty() || askLevels.isEmpty()) {
            return Collections.emptyList();
        }

        return executeTraversal(bidLevels, askLevels, strategy);
    }

    /**
     * Core two-pointer traversal algorithm.
     */
    private List<MatchCandidateExtractor> executeTraversal(
            List<BidPriceLevel> bidLevels,
            List<AskPriceLevel> askLevels,
            MatchingStrategy strategy) {

        List<MatchCandidateExtractor> allCandidates = new ArrayList<>();

        int bidIndex = 0;  // Start with highest bid (descending order)
        int askIndex = 0;  // Start with lowest ask (ascending order)

        // Two-pointer traversal: O(n + m) complexity
        while (bidIndex < bidLevels.size() && askIndex < askLevels.size()) {
            BidPriceLevel bidLevel = bidLevels.get(bidIndex);
            AskPriceLevel askLevel = askLevels.get(askIndex);

            // Check if price levels can potentially match
            if (canPriceLevelsCross(bidLevel, askLevel)) {
                // Delegate to strategy for actual validation
                List<MatchCandidateExtractor> levelCandidates =
                        findCandidatesAtPriceLevels(bidLevel, askLevel, strategy);
                allCandidates.addAll(levelCandidates);

                // Move to next price levels
                bidIndex++;
                askIndex++;
            } else {
                // No more matches possible - elegant termination
                // bid < ask: all remaining bids will be < current ask (descending)
                // all remaining asks will be > current bid (ascending)
                break;
            }
        }

        return allCandidates;
    }

    /**
     * Quick price level compatibility check before delegating to strategy.
     */
    private boolean canPriceLevelsCross(BidPriceLevel bidLevel, AskPriceLevel askLevel) {
        return bidLevel.getPrice().isGreaterThanOrEqual(askLevel.getPrice());
    }

    /**
     * Finds match candidates between orders at two specific price levels.
     * Delegates validation to strategy.
     */
    private List<MatchCandidateExtractor> findCandidatesAtPriceLevels(
            BidPriceLevel bidLevel,
            AskPriceLevel askLevel,
            MatchingStrategy strategy) {

        // Get best orders from each level (FIFO within price level)
        Optional<IBuyOrder> bestBuyOrder = bidLevel.getFirstActiveOrder();
        Optional<ISellOrder> bestSellOrder = askLevel.getFirstActiveOrder();

        if (bestBuyOrder.isPresent() && bestSellOrder.isPresent()) {
            // Delegate to strategy for validation and candidate creation
            return new ArrayList<>(strategy.findMatchCandidates(bestBuyOrder.get(), bestSellOrder.get()));
        }

        return Collections.emptyList();
    }
}

