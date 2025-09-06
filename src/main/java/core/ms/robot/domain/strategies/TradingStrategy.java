package core.ms.robot.domain.strategies;

import core.ms.portfolio.domain.ports.inbound.PortfolioSnapshot;
import core.ms.robot.config.BotConfig;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;

public interface TradingStrategy {
    TradingDecision decide(Money currentPrice, PortfolioSnapshot snapshot,
                           Symbol symbol, BotConfig config);

    class TradingDecision {
        private final TradingAction action;
        private final BigDecimal quantity;
        private final String reason;
        private final Money customPrice; // Optional custom price for the order

        public TradingDecision(TradingAction action, BigDecimal quantity, String reason) {
            this.action = action;
            this.quantity = quantity;
            this.reason = reason;
            this.customPrice = null; // Use market price
        }

        public TradingDecision(TradingAction action, BigDecimal quantity, String reason, Money customPrice) {
            this.action = action;
            this.quantity = quantity;
            this.reason = reason;
            this.customPrice = customPrice;
        }

        public static TradingDecision buy(BigDecimal quantity, String reason) {
            return new TradingDecision(TradingAction.BUY, quantity, reason);
        }

        public static TradingDecision buy(BigDecimal quantity, String reason, Money price) {
            return new TradingDecision(TradingAction.BUY, quantity, reason, price);
        }

        public static TradingDecision sell(BigDecimal quantity, String reason) {
            return new TradingDecision(TradingAction.SELL, quantity, reason);
        }

        public static TradingDecision sell(BigDecimal quantity, String reason, Money price) {
            return new TradingDecision(TradingAction.SELL, quantity, reason, price);
        }

        public static TradingDecision hold(String reason) {
            return new TradingDecision(TradingAction.HOLD, BigDecimal.ZERO, reason);
        }

        public TradingAction getAction() { return action; }
        public BigDecimal getQuantity() { return quantity; }
        public String getReason() { return reason; }
        public Money getCustomPrice() { return customPrice; }
        public boolean hasCustomPrice() { return customPrice != null; }
    }

    enum TradingAction {
        BUY, SELL, HOLD
    }
}