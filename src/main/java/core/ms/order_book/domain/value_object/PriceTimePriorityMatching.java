package core.ms.order_book.domain.value_object;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;

import java.math.BigDecimal;
import java.util.*;

public class PriceTimePriorityMatching implements MatchingStrategy {

    @Override
    public List<MatchCandidate> findMatchCandidates(IBuyOrder buyOrder, ISellOrder sellOrder) {
        MatchCandidate candidate = new MatchValidationBuilder(buyOrder, sellOrder)
                .validateSymbolCompatibility()
                .validatePriceCompatibility()
                .validateOrdersActive()
                .validateRemainingQuantity()
                .build();

        return candidate.isValid() ? List.of(candidate) : Collections.emptyList();
    }

    @Override
    public boolean canMatch(IBuyOrder buyOrder, ISellOrder sellOrder) {
        return new MatchValidationBuilder(buyOrder, sellOrder)
                .validateSymbolCompatibility()
                .validatePriceCompatibility()
                .validateOrdersActive()
                .validateRemainingQuantity()
                .build()
                .isValid();
    }

    // ============ EMBEDDED MATCH CANDIDATE ============

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

        public static MatchCandidate invalid(IBuyOrder buyOrder, ISellOrder sellOrder, String reason) {
            return new MatchCandidate(buyOrder, sellOrder, false, reason);
        }

        @Override
        public IBuyOrder getBuyOrder() {
            return buyOrder;
        }

        @Override
        public ISellOrder getSellOrder() {
            return sellOrder;
        }

        @Override
        public boolean isValid() {
            return isValid;
        }

        public String getValidationContext() {
            return validationContext;
        }
    }

    // ============ MATCH VALIDATION BUILDER (DSL) ============

    private static class MatchValidationBuilder {
        private final IBuyOrder buyOrder;
        private final ISellOrder sellOrder;
        private boolean isValid = true;
        private String failureReason = "";

        public MatchValidationBuilder(IBuyOrder buyOrder, ISellOrder sellOrder) {
            this.buyOrder = buyOrder;
            this.sellOrder = sellOrder;
        }

        public MatchValidationBuilder validateSymbolCompatibility() {
            if (isValid && !buyOrder.getSymbol().equals(sellOrder.getSymbol())) {
                isValid = false;
                failureReason = "Symbol mismatch: " + buyOrder.getSymbol() + " vs " + sellOrder.getSymbol();
            }
            return this;
        }

        public MatchValidationBuilder validatePriceCompatibility() {
            if (isValid && buyOrder.getPrice().isLessThan(sellOrder.getPrice())) {
                isValid = false;
                failureReason = "Price incompatible: buy " + buyOrder.getPrice() + " < sell " + sellOrder.getPrice();
            }
            return this;
        }

        public MatchValidationBuilder validateOrdersActive() {
            if (isValid && (!buyOrder.isActive() || !sellOrder.isActive())) {
                isValid = false;
                failureReason = "Inactive orders: buy active=" + buyOrder.isActive() + ", sell active=" + sellOrder.isActive();
            }
            return this;
        }

        public MatchValidationBuilder validateRemainingQuantity() {
            if (isValid && (buyOrder.getRemainingQuantity().compareTo(BigDecimal.ZERO) <= 0 ||
                    sellOrder.getRemainingQuantity().compareTo(BigDecimal.ZERO) <= 0)) {
                isValid = false;
                failureReason = "Insufficient quantity: buy=" + buyOrder.getRemainingQuantity() + ", sell=" + sellOrder.getRemainingQuantity();
            }
            return this;
        }

        public MatchCandidate build() {
            if (isValid) {
                return MatchCandidate.valid(buyOrder, sellOrder);
            } else {
                return MatchCandidate.invalid(buyOrder, sellOrder, failureReason);
            }
        }
    }
}