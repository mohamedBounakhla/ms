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
 * Aggressive trading strategy - places orders very close to market price
 * Aims to capture small price movements with larger volumes
 */
public class AggressiveStrategy implements TradingStrategy {
    private final Random random = new Random();
    private int consecutiveHolds = 0;

    @Override
    public TradingDecision decide(Money currentPrice, PortfolioSnapshot snapshot,
                                  Symbol symbol, BotConfig config) {

        BigDecimal holdings = snapshot.getPositions().getOrDefault(symbol, BigDecimal.ZERO);
        Money availableCash = snapshot.getCashBalances().getOrDefault(
                Currency.USD, Money.zero(Currency.USD)
        );

        // Very tight spread (0.1% to 0.5%)
        BigDecimal spreadPercent = new BigDecimal("0.001").add(
                new BigDecimal("0.004").multiply(BigDecimal.valueOf(random.nextDouble()))
        );

        // Become more aggressive after holding
        if (consecutiveHolds > 2) {
            spreadPercent = spreadPercent.multiply(new BigDecimal("0.5"));
        }

        // 70% chance to act - very active
        if (random.nextDouble() > 0.3 && consecutiveHolds < 3) {
            consecutiveHolds++;
            return TradingDecision.hold("Brief pause");
        }

        consecutiveHolds = 0;

        // Determine direction based on portfolio balance
        BigDecimal totalValue = availableCash.getAmount();
        if (holdings.compareTo(BigDecimal.ZERO) > 0) {
            totalValue = totalValue.add(holdings.multiply(currentPrice.getAmount()));
        }

        BigDecimal cashRatio = totalValue.compareTo(BigDecimal.ZERO) > 0 ?
                availableCash.getAmount().divide(totalValue, 4, RoundingMode.HALF_UP) :
                BigDecimal.ONE;

        // Aggressive rebalancing
        boolean shouldBuy = cashRatio.compareTo(new BigDecimal("0.4")) > 0;

        if (shouldBuy && availableCash.isPositive()) {
            // Aggressive buy - very close to market or even at market
            BigDecimal priceAdjustment = random.nextDouble() < 0.3 ?
                    BigDecimal.ZERO : // 30% chance to buy at market
                    spreadPercent.multiply(BigDecimal.valueOf(random.nextDouble()));

            BigDecimal buyPrice = currentPrice.getAmount().multiply(
                    BigDecimal.ONE.subtract(priceAdjustment)
            );

            // Larger order sizes for aggressive trading
            BigDecimal maxAffordable = availableCash.getAmount()
                    .divide(buyPrice, 8, RoundingMode.DOWN);

            BigDecimal quantity = config.getMaxOrderSize().multiply(
                    new BigDecimal("0.5").add(
                            new BigDecimal("0.5").multiply(BigDecimal.valueOf(random.nextDouble()))
                    )
            );

            quantity = quantity.min(maxAffordable);

            if (quantity.compareTo(config.getMinOrderSize()) >= 0) {
                String reason = priceAdjustment.compareTo(BigDecimal.ZERO) == 0 ?
                        "Aggressive market buy" :
                        String.format("Aggressive buy at -%.3f%%",
                                priceAdjustment.multiply(new BigDecimal("100")));

                return new TradingDecision(
                        TradingAction.BUY,
                        quantity,
                        reason,
                        new Money(buyPrice, Currency.USD)
                );
            }
        } else if (!shouldBuy && holdings.compareTo(BigDecimal.ZERO) > 0) {
            // Aggressive sell
            BigDecimal priceAdjustment = random.nextDouble() < 0.3 ?
                    BigDecimal.ZERO : // 30% chance to sell at market
                    spreadPercent.multiply(BigDecimal.valueOf(random.nextDouble()));

            BigDecimal sellPrice = currentPrice.getAmount().multiply(
                    BigDecimal.ONE.add(priceAdjustment)
            );

            // Larger order sizes
            BigDecimal quantity = holdings.multiply(
                    new BigDecimal("0.3").add(
                            new BigDecimal("0.4").multiply(BigDecimal.valueOf(random.nextDouble()))
                    )
            );

            quantity = quantity.min(config.getMaxOrderSize());

            if (quantity.compareTo(config.getMinOrderSize()) >= 0) {
                String reason = priceAdjustment.compareTo(BigDecimal.ZERO) == 0 ?
                        "Aggressive market sell" :
                        String.format("Aggressive sell at +%.3f%%",
                                priceAdjustment.multiply(new BigDecimal("100")));

                return new TradingDecision(
                        TradingAction.SELL,
                        quantity,
                        reason,
                        new Money(sellPrice, Currency.USD)
                );
            }
        }

        consecutiveHolds = 0;
        return TradingDecision.hold("Regrouping");
    }
}