package core.ms.robot.model.strategies;

import core.ms.portfolio.domain.ports.inbound.PortfolioSnapshot;
import core.ms.robot.config.BotConfig;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.util.Random;

public class RandomStrategy implements TradingStrategy {
    private final Random random = new Random();

    @Override
    public TradingDecision decide(Money currentPrice, PortfolioSnapshot snapshot,
                                  Symbol symbol, BotConfig config) {

        // Random decision: 30% buy, 30% sell, 40% hold
        double decision = random.nextDouble();

        if (decision < 0.3) {
            // Try to buy
            Money availableCash = snapshot.getCashBalances().getOrDefault(
                    Currency.USD, Money.zero(Currency.USD)
            );

            if (availableCash.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                // Calculate random quantity within limits
                BigDecimal maxAffordable = availableCash.divide(currentPrice.getAmount()).getAmount();
                BigDecimal quantity = config.getMinOrderSize().add(
                        config.getMaxOrderSize().subtract(config.getMinOrderSize())
                                .multiply(BigDecimal.valueOf(random.nextDouble()))
                );

                quantity = quantity.min(maxAffordable).min(config.getMaxOrderSize());

                if (quantity.compareTo(config.getMinOrderSize()) >= 0) {
                    return TradingDecision.buy(quantity, "Random buy signal");
                }
            }
        } else if (decision < 0.6) {
            // Try to sell
            BigDecimal holdings = snapshot.getPositions().getOrDefault(symbol, BigDecimal.ZERO);

            if (holdings.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal quantity = config.getMinOrderSize().add(
                        holdings.min(config.getMaxOrderSize()).subtract(config.getMinOrderSize())
                                .multiply(BigDecimal.valueOf(random.nextDouble()))
                );

                quantity = quantity.min(holdings);

                if (quantity.compareTo(config.getMinOrderSize()) >= 0) {
                    return TradingDecision.sell(quantity, "Random sell signal");
                }
            }
        }

        return TradingDecision.hold("Random hold signal");
    }
}