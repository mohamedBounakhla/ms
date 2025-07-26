package core.ms.order_book.domain.value_object;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order_book.domain.entities.OrderBook;
import core.ms.shared.domain.Money;

import java.math.BigDecimal;
import java.util.*;

public class PriceTimePriorityMatching implements MatchingStrategy<PriceTimePriorityMatching.MatchCandidate> {

    @Override
    public List<MatchCandidate> findMatchCandidates(OrderBook orderBook) {
        return new MatchFindingBuilder(orderBook)
                .extractBestOrders()
                .validateSpreadCrossed()
                .validateCanMatch()
                .createCandidates()
                .build();
    }

    @Override
    public boolean canMatch(IBuyOrder buyOrder, ISellOrder sellOrder) {
        return new MatchValidationBuilder(buyOrder, sellOrder)
                .validateSymbolCompatibility()
                .validatePriceCompatibility()
                .validateOrdersActive()
                .validateRemainingQuantity()
                .build();
    }

    // ============ EMBEDDED MATCH CANDIDATE ============

    /**
     * Intermediate storage object specific to PriceTimePriorityMatching.
     * Contains validated matching order pairs and context specific to this strategy.
     */
    public static class MatchCandidate implements MatchCandidateExtractor {
        private final IBuyOrder buyOrder;
        private final ISellOrder sellOrder;
        private final boolean isValid;
        private final String validationContext;

        private MatchCandidate(IBuyOrder buyOrder, ISellOrder sellOrder, boolean isValid, String validationContext) {
            this.buyOrder = buyOrder;
            this.sellOrder = sellOrder;
            this.isValid = isValid;
            this.validationContext = validationContext;
        }

        public static MatchCandidate valid(IBuyOrder buyOrder, ISellOrder sellOrder) {
            return new MatchCandidate(
                    Objects.requireNonNull(buyOrder, "Buy order cannot be null"),
                    Objects.requireNonNull(sellOrder, "Sell order cannot be null"),
                    true,
                    "Price-time priority validation passed"
            );
        }

        public static MatchCandidate validWithContext(IBuyOrder buyOrder, ISellOrder sellOrder, String context) {
            return new MatchCandidate(
                    Objects.requireNonNull(buyOrder, "Buy order cannot be null"),
                    Objects.requireNonNull(sellOrder, "Sell order cannot be null"),
                    true,
                    context
            );
        }

        public IBuyOrder getBuyOrder() {
            return buyOrder;
        }

        public ISellOrder getSellOrder() {
            return sellOrder;
        }

        public boolean isValid() {
            return isValid;
        }

        public String getValidationContext() {
            return validationContext;
        }

        @Override
        public String toString() {
            return String.format("PriceTimePriorityMatchCandidate{valid=%s, buy=%s, sell=%s, context='%s'}",
                    isValid,
                    buyOrder != null ? buyOrder.getId() : "null",
                    sellOrder != null ? sellOrder.getId() : "null",
                    validationContext);
        }
    }

    // ============ MATCH FINDING BUILDER ============

    private static class MatchFindingBuilder {
        private final OrderBook orderBook;
        private Optional<IBuyOrder> bestBuyOrder = Optional.empty();
        private Optional<ISellOrder> bestSellOrder = Optional.empty();
        private boolean spreadCrossed = false;
        private boolean canMatch = false;
        private List<MatchCandidate> candidates = new ArrayList<>();

        public MatchFindingBuilder(OrderBook orderBook) {
            this.orderBook = Objects.requireNonNull(orderBook, "OrderBook cannot be null");
        }

        public MatchFindingBuilder extractBestOrders() {
            bestBuyOrder = orderBook.getBestBuyOrder();
            bestSellOrder = orderBook.getBestSellOrder();
            return this;
        }

        public MatchFindingBuilder validateSpreadCrossed() {
            Optional<Money> bestBid = orderBook.getBestBid();
            Optional<Money> bestAsk = orderBook.getBestAsk();

            if (bestBid.isPresent() && bestAsk.isPresent()) {
                spreadCrossed = bestBid.get().isGreaterThanOrEqual(bestAsk.get());
                // Could add logging here: log.debug("Spread crossed: {} >= {}", bestBid.get(), bestAsk.get());
            }
            return this;
        }

        public MatchFindingBuilder validateCanMatch() {
            if (spreadCrossed && bestBuyOrder.isPresent() && bestSellOrder.isPresent()) {
                // Use the validation builder for consistency
                canMatch = new MatchValidationBuilder(bestBuyOrder.get(), bestSellOrder.get())
                        .validateSymbolCompatibility()
                        .validatePriceCompatibility()
                        .validateOrdersActive()
                        .validateRemainingQuantity()
                        .build();
                // Could add logging here: log.debug("Orders can match: {}", canMatch);
            }
            return this;
        }

        public MatchFindingBuilder createCandidates() {
            if (canMatch) {
                MatchCandidate candidate = MatchCandidate.validWithContext(
                        bestBuyOrder.get(),
                        bestSellOrder.get(),
                        "Price-time priority validation passed"
                );
                candidates.add(candidate);
                // Could add logging here: log.debug("Created candidate: buy={}, sell={}", bestBuyOrder.get().getId(), bestSellOrder.get().getId());
            }
            return this;
        }

        public List<MatchCandidate> build() {
            return candidates.isEmpty() ? Collections.emptyList() : new ArrayList<>(candidates);
        }
    }

    // ============ MATCH VALIDATION BUILDER ============

    private static class MatchValidationBuilder {
        private final IBuyOrder buyOrder;
        private final ISellOrder sellOrder;
        private boolean isValid = true;

        public MatchValidationBuilder(IBuyOrder buyOrder, ISellOrder sellOrder) {
            this.buyOrder = buyOrder;
            this.sellOrder = sellOrder;
        }

        public MatchValidationBuilder validateSymbolCompatibility() {
            if (isValid && !buyOrder.getSymbol().equals(sellOrder.getSymbol())) {
                isValid = false;
                // Could add logging here: log.debug("Symbol mismatch: {} vs {}", buyOrder.getSymbol(), sellOrder.getSymbol());
            }
            return this;
        }

        public MatchValidationBuilder validatePriceCompatibility() {
            if (isValid && !buyOrder.getPrice().isGreaterThanOrEqual(sellOrder.getPrice())) {
                isValid = false;
                // Could add logging here: log.debug("Price incompatible: buy {} < sell {}", buyOrder.getPrice(), sellOrder.getPrice());
            }
            return this;
        }

        public MatchValidationBuilder validateOrdersActive() {
            if (isValid && (!buyOrder.isActive() || !sellOrder.isActive())) {
                isValid = false;
                // Could add logging here: log.debug("Inactive orders: buy active={}, sell active={}", buyOrder.isActive(), sellOrder.isActive());
            }
            return this;
        }

        public MatchValidationBuilder validateRemainingQuantity() {
            if (isValid && (buyOrder.getRemainingQuantity().compareTo(BigDecimal.ZERO) <= 0 ||
                    sellOrder.getRemainingQuantity().compareTo(BigDecimal.ZERO) <= 0)) {
                isValid = false;
                // Could add logging here: log.debug("Insufficient quantity: buy={}, sell={}", buyOrder.getRemainingQuantity(), sellOrder.getRemainingQuantity());
            }
            return this;
        }

        public boolean build() {
            return isValid;
        }
    }
}