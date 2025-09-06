package core.ms.robot.domain.strategies;

import core.ms.portfolio.domain.ports.inbound.PortfolioSnapshot;
import core.ms.robot.config.BotConfig;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

/**
 * Strategy that places orders with a spread around the current market price
 * Helps create more realistic market depth
 */
public class SpreadStrategy implements TradingStrategy {
    private final Random random = new Random();
    private int tickCount = 0;

    @Override
    public TradingDecision decide(Money currentPrice, PortfolioSnapshot snapshot,
                                  Symbol symbol, BotConfig config) {
        tickCount++;

        // Determine spread percentage based on risk tolerance (0.1% to 2%)
        BigDecimal spreadPercent = config.getRiskTolerance()
                .multiply(new BigDecimal("0.02"))
                .max(new BigDecimal("0.001"));

        // Calculate price with spread
        BigDecimal priceVariation = currentPrice.getAmount()
                .multiply(spreadPercent)
                .multiply(BigDecimal.valueOf(random.nextGaussian())); // Normal distribution

        BigDecimal adjustedPrice = currentPrice.getAmount().add(priceVariation);

        // Decide action based on position
        BigDecimal holdings = snapshot.getPositions().getOrDefault(symbol, BigDecimal.ZERO);
        Money availableCash = snapshot.getCashBalances().getOrDefault(
                Currency.USD, Money.zero(Currency.USD)
        );

        // 40% chance to act on each tick
        if (random.nextDouble() > 0.4) {
            return TradingDecision.hold("Waiting for opportunity");
        }

        // If we have more assets, bias toward selling
        // If we have more cash, bias toward buying
        BigDecimal totalValue = availableCash.getAmount();
        if (holdings.compareTo(BigDecimal.ZERO) > 0) {
            totalValue = totalValue.add(holdings.multiply(currentPrice.getAmount()));
        }

        BigDecimal cashRatio = availableCash.getAmount().divide(totalValue, 4, RoundingMode.HALF_UP);

        boolean shouldBuy = cashRatio.compareTo(new BigDecimal("0.5")) > 0 ?
                random.nextDouble() < 0.7 : random.nextDouble() < 0.3;

        if (shouldBuy && availableCash.isPositive()) {
            // Place buy order below market price
            BigDecimal buyPrice = adjustedPrice.subtract(
                    adjustedPrice.multiply(spreadPercent.multiply(new BigDecimal("0.5")))
            );

            BigDecimal maxAffordable = availableCash.getAmount()
                    .divide(buyPrice, 8, RoundingMode.DOWN);

            BigDecimal quantity = config.getMinOrderSize().add(
                    config.getMaxOrderSize().subtract(config.getMinOrderSize())
                            .multiply(BigDecimal.valueOf(random.nextDouble() * 0.3))
            );

            quantity = quantity.min(maxAffordable).min(config.getMaxOrderSize());

            if (quantity.compareTo(config.getMinOrderSize()) >= 0) {
                // Return decision with the ADJUSTED price for variety
                return new TradingDecision(
                        TradingAction.BUY,
                        quantity,
                        String.format("Spread buy at %.2f%% below market",
                                spreadPercent.multiply(new BigDecimal("50"))),
                        new Money(buyPrice, Currency.USD)
                );
            }
        } else if (!shouldBuy && holdings.compareTo(BigDecimal.ZERO) > 0) {
            // Place sell order above market price
            BigDecimal sellPrice = adjustedPrice.add(
                    adjustedPrice.multiply(spreadPercent.multiply(new BigDecimal("0.5")))
            );

            BigDecimal quantity = config.getMinOrderSize().add(
                    holdings.min(config.getMaxOrderSize()).subtract(config.getMinOrderSize())
                            .multiply(BigDecimal.valueOf(random.nextDouble() * 0.3))
            );

            quantity = quantity.min(holdings);

            if (quantity.compareTo(config.getMinOrderSize()) >= 0) {
                return new TradingDecision(
                        TradingAction.SELL,
                        quantity,
                        String.format("Spread sell at %.2f%% above market",
                                spreadPercent.multiply(new BigDecimal("50"))),
                        new Money(sellPrice, Currency.USD)
                );
            }
        }

        return TradingDecision.hold("No spread opportunity");
    }
}