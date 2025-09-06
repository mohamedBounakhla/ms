package core.ms.robot.domain.strategies;

import core.ms.portfolio.domain.ports.inbound.PortfolioSnapshot;
import core.ms.robot.config.BotConfig;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

public class RandomStrategy implements TradingStrategy {
    private final Random random = new Random();

    @Override
    public TradingDecision decide(Money currentPrice, PortfolioSnapshot snapshot,
                                  Symbol symbol, BotConfig config) {

        // Random decision: 30% buy, 30% sell, 40% hold
        double decision = random.nextDouble();

        // Add price variation: -2% to +2% spread around market price
        BigDecimal priceVariation = new BigDecimal("0.02");
        BigDecimal randomSpread = priceVariation.multiply(
                BigDecimal.valueOf(random.nextDouble() * 2 - 1) // -1 to +1
        );

        if (decision < 0.3) {
            // Try to buy
            Money availableCash = snapshot.getCashBalances().getOrDefault(
                    Currency.USD, Money.zero(Currency.USD)
            );

            if (availableCash.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                // Buy BELOW market price (negative spread)
                BigDecimal buySpread = priceVariation.multiply(
                        BigDecimal.valueOf(random.nextDouble() * 0.5) // 0 to 0.5%
                );
                BigDecimal buyPrice = currentPrice.getAmount().multiply(
                        BigDecimal.ONE.subtract(buySpread)
                );

                // Calculate random quantity within limits
                BigDecimal maxAffordable = availableCash.getAmount()
                        .divide(buyPrice, 8, RoundingMode.DOWN);
                BigDecimal quantity = config.getMinOrderSize().add(
                        config.getMaxOrderSize().subtract(config.getMinOrderSize())
                                .multiply(BigDecimal.valueOf(random.nextDouble()))
                );

                quantity = quantity.min(maxAffordable).min(config.getMaxOrderSize());

                if (quantity.compareTo(config.getMinOrderSize()) >= 0) {
                    return new TradingDecision(
                            TradingAction.BUY,
                            quantity,
                            String.format("Random buy at -%.2f%%", buySpread.multiply(new BigDecimal("100"))),
                            new Money(buyPrice, Currency.USD)
                    );
                }
            }
        } else if (decision < 0.6) {
            // Try to sell
            BigDecimal holdings = snapshot.getPositions().getOrDefault(symbol, BigDecimal.ZERO);

            if (holdings.compareTo(BigDecimal.ZERO) > 0) {
                // Sell ABOVE market price (positive spread)
                BigDecimal sellSpread = priceVariation.multiply(
                        BigDecimal.valueOf(random.nextDouble() * 0.5) // 0 to 0.5%
                );
                BigDecimal sellPrice = currentPrice.getAmount().multiply(
                        BigDecimal.ONE.add(sellSpread)
                );

                BigDecimal quantity = config.getMinOrderSize().add(
                        holdings.min(config.getMaxOrderSize()).subtract(config.getMinOrderSize())
                                .multiply(BigDecimal.valueOf(random.nextDouble()))
                );

                quantity = quantity.min(holdings);

                if (quantity.compareTo(config.getMinOrderSize()) >= 0) {
                    return new TradingDecision(
                            TradingAction.SELL,
                            quantity,
                            String.format("Random sell at +%.2f%%", sellSpread.multiply(new BigDecimal("100"))),
                            new Money(sellPrice, Currency.USD)
                    );
                }
            }
        }

        return TradingDecision.hold("Random hold signal");
    }
}