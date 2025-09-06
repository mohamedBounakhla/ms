package core.ms.order_book.domain.value_object;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;

import java.math.BigDecimal;
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

        System.out.println("DEBUG TwoPointer: Bid levels: " + bidLevels.size() +
                ", Ask levels: " + askLevels.size());

        if (bidLevels.isEmpty() || askLevels.isEmpty()) {
            return Collections.emptyList();
        }

        return executeTraversal(bidLevels, askLevels, strategy);
    }

    /**
     * Core two-pointer traversal algorithm.
     * Modified to handle multiple orders at each price level properly.
     */
    private List<MatchCandidateExtractor> executeTraversal(
            List<BidPriceLevel> bidLevels,
            List<AskPriceLevel> askLevels,
            MatchingStrategy strategy) {

        List<MatchCandidateExtractor> allCandidates = new ArrayList<>();

        // Process all crossing price levels
        for (BidPriceLevel bidLevel : bidLevels) {
            for (AskPriceLevel askLevel : askLevels) {

                // Check if price levels can potentially match
                if (!canPriceLevelsCross(bidLevel, askLevel)) {
                    // Since ask levels are sorted ascending, if this doesn't cross,
                    // no further ask levels will cross with this bid level
                    break;
                }

                System.out.println("DEBUG TwoPointer: Checking bid level " + bidLevel.getPrice() +
                        " vs ask level " + askLevel.getPrice());

                // Find all matches at these price levels
                List<MatchCandidateExtractor> levelCandidates =
                        findCandidatesAtPriceLevels(bidLevel, askLevel, strategy);

                allCandidates.addAll(levelCandidates);
            }
        }

        System.out.println("DEBUG TwoPointer: Total candidates found: " + allCandidates.size());
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
     * Processes ALL orders at these levels, not just the first.
     */
    private List<MatchCandidateExtractor> findCandidatesAtPriceLevels(
            BidPriceLevel bidLevel,
            AskPriceLevel askLevel,
            MatchingStrategy strategy) {

        // Get ALL active orders from each level
        List<IBuyOrder> buyOrders = bidLevel.getActiveOrders();
        List<ISellOrder> sellOrders = askLevel.getActiveOrders();

        System.out.println("DEBUG TwoPointer: Found " + buyOrders.size() +
                " buy orders and " + sellOrders.size() +
                " sell orders at crossing levels");

        List<MatchCandidateExtractor> candidates = new ArrayList<>();

        // Track which orders have been matched to avoid double-matching
        Set<String> matchedBuyOrders = new HashSet<>();
        Set<String> matchedSellOrders = new HashSet<>();

        // Process orders in FIFO order within the price level
        for (IBuyOrder buyOrder : buyOrders) {
            if (matchedBuyOrders.contains(buyOrder.getId())) {
                continue;
            }

            for (ISellOrder sellOrder : sellOrders) {
                if (matchedSellOrders.contains(sellOrder.getId())) {
                    continue;
                }

                // Check if orders have remaining quantity
                if (buyOrder.getRemainingQuantity().compareTo(BigDecimal.ZERO) <= 0 ||
                        sellOrder.getRemainingQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    System.out.println("DEBUG TwoPointer: Skipping order with no remaining quantity");
                    continue;
                }

                System.out.println("DEBUG TwoPointer: Testing match between Buy: " +
                        buyOrder.getId() + " and Sell: " + sellOrder.getId());

                // Delegate to strategy for validation and candidate creation
                List<? extends MatchCandidateExtractor> matches =
                        strategy.findMatchCandidates(buyOrder, sellOrder);

                if (!matches.isEmpty()) {
                    candidates.addAll(matches);

                    // Mark orders as matched for this round
                    matchedBuyOrders.add(buyOrder.getId());
                    matchedSellOrders.add(sellOrder.getId());

                    // Move to next buy order (FIFO)
                    break;
                }
            }
        }

        return candidates;
    }
}