package core.ms.robot.domain.strategies;

import core.ms.portfolio.domain.ports.inbound.PortfolioSnapshot;
import core.ms.robot.config.BotConfig;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.Queue;

public class MomentumStrategy implements TradingStrategy {
    private final Queue<BigDecimal> priceHistory = new LinkedList<>();
    private final int windowSize = 5;

    @Override
    public TradingDecision decide(Money currentPrice, PortfolioSnapshot snapshot,
                                  Symbol symbol, BotConfig config) {

        // Track price history
        priceHistory.offer(currentPrice.getAmount());
        if (priceHistory.size() > windowSize) {
            priceHistory.poll();
        }

        // Need enough history
        if (priceHistory.size() < windowSize) {
            return TradingDecision.hold("Building price history");
        }

        // Calculate momentum (simple moving average)
        BigDecimal sum = priceHistory.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(windowSize), 8, RoundingMode.HALF_UP);

        BigDecimal momentum = currentPrice.getAmount().subtract(average)
                .divide(average, 4, RoundingMode.HALF_UP);

        // Strong upward momentum - BUY
        if (momentum.compareTo(new BigDecimal("0.02")) > 0) {
            Money availableCash = snapshot.getCashBalances().getOrDefault(
                    Currency.USD, Money.zero(Currency.USD)
            );

            if (availableCash.isPositive()) {
                // Use more aggressive sizing for strong momentum
                BigDecimal quantity = availableCash.divide(currentPrice.getAmount()).getAmount()
                        .multiply(new BigDecimal("0.3")); // Use 30% of available cash

                quantity = quantity.min(config.getMaxOrderSize());

                if (quantity.compareTo(config.getMinOrderSize()) >= 0) {
                    return TradingDecision.buy(quantity,
                            String.format("Momentum BUY signal (%.2f%%)", momentum.multiply(new BigDecimal("100"))));
                }
            }
        }
        // Strong downward momentum - SELL
        else if (momentum.compareTo(new BigDecimal("-0.02")) < 0) {
            BigDecimal holdings = snapshot.getPositions().getOrDefault(symbol, BigDecimal.ZERO);

            if (holdings.compareTo(BigDecimal.ZERO) > 0) {
                // Sell half of holdings on negative momentum
                BigDecimal quantity = holdings.multiply(new BigDecimal("0.5"));

                if (quantity.compareTo(config.getMinOrderSize()) >= 0) {
                    return TradingDecision.sell(quantity,
                            String.format("Momentum SELL signal (%.2f%%)", momentum.multiply(new BigDecimal("100"))));
                }
            }
        }

        return TradingDecision.hold(String.format("No momentum signal (%.2f%%)",
                momentum.multiply(new BigDecimal("100"))));
    }
}