package core.ms.order_book.domain.value_object;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;

import java.math.BigDecimal;
import java.util.*;

public class PriceTimePriorityMatching implements MatchingStrategy {

    @Override
    public List<MatchCandidate> findMatchCandidates(IBuyOrder buyOrder, ISellOrder sellOrder) {
        System.out.println("DEBUG PriceTimePriorityMatching: Evaluating match between Buy: " +
                buyOrder.getId() + " (price: " + buyOrder.getPrice() +
                ", remaining: " + buyOrder.getRemainingQuantity() +
                ") and Sell: " + sellOrder.getId() +
                " (price: " + sellOrder.getPrice() +
                ", remaining: " + sellOrder.getRemainingQuantity() + ")");

        MatchCandidate candidate = new MatchValidationBuilder(buyOrder, sellOrder)
                .validateSymbolCompatibility()
                .validatePriceCompatibility()
                .validateOrdersActive()
                .validateRemainingQuantity()
                .build();

        System.out.println("DEBUG: Match candidate valid: " + candidate.isValid() +
                (candidate.isValid() ? "" : " - Reason: " + candidate.getValidationContext()));

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
                System.out.println("DEBUG Validation: " + failureReason);
            }
            return this;
        }

        public MatchValidationBuilder validatePriceCompatibility() {
            if (isValid && buyOrder.getPrice().isLessThan(sellOrder.getPrice())) {
                isValid = false;
                failureReason = "Price incompatible: buy " + buyOrder.getPrice() + " < sell " + sellOrder.getPrice();
                System.out.println("DEBUG Validation: " + failureReason);
            }
            return this;
        }

        public MatchValidationBuilder validateOrdersActive() {
            if (isValid) {
                if (!buyOrder.isActive()) {
                    isValid = false;
                    failureReason = "Buy order not active: " + buyOrder.getStatus().getStatus();
                    System.out.println("DEBUG Validation: " + failureReason);
                } else if (!sellOrder.isActive()) {
                    isValid = false;
                    failureReason = "Sell order not active: " + sellOrder.getStatus().getStatus();
                    System.out.println("DEBUG Validation: " + failureReason);
                }
            }
            return this;
        }

        public MatchValidationBuilder validateRemainingQuantity() {
            if (isValid) {
                BigDecimal buyRemaining = buyOrder.getRemainingQuantity();
                BigDecimal sellRemaining = sellOrder.getRemainingQuantity();

                if (buyRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                    isValid = false;
                    failureReason = "Buy order has no remaining quantity: " + buyRemaining;
                    System.out.println("DEBUG Validation: " + failureReason);
                } else if (sellRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                    isValid = false;
                    failureReason = "Sell order has no remaining quantity: " + sellRemaining;
                    System.out.println("DEBUG Validation: " + failureReason);
                }
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