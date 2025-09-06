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
 * Market Maker strategy - places both buy and sell orders around market price
 * Creates liquidity by maintaining bid-ask spreads
 */
public class MarketMakerStrategy implements TradingStrategy {
    private final Random random = new Random();
    private boolean lastWasBuy = false;

    @Override
    public TradingDecision decide(Money currentPrice, PortfolioSnapshot snapshot,
                                  Symbol symbol, BotConfig config) {

        BigDecimal holdings = snapshot.getPositions().getOrDefault(symbol, BigDecimal.ZERO);
        Money availableCash = snapshot.getCashBalances().getOrDefault(
                Currency.USD, Money.zero(Currency.USD)
        );

        // Calculate spread based on risk tolerance (0.5% to 3%)
        BigDecimal spreadPercent = new BigDecimal("0.005").add(
                config.getRiskTolerance().multiply(new BigDecimal("0.025"))
        );

        // Market makers alternate between buy and sell to maintain balance
        boolean canBuy = availableCash.getAmount().compareTo(
                config.getMinOrderSize().multiply(currentPrice.getAmount())
        ) > 0;

        boolean canSell = holdings.compareTo(config.getMinOrderSize()) >= 0;

        // Skip if we can't trade
        if (!canBuy && !canSell) {
            return TradingDecision.hold("Insufficient funds/assets");
        }

        // Alternate or choose based on what's possible
        boolean placeBuyOrder = lastWasBuy ? !canSell : canBuy;

        if (placeBuyOrder && canBuy) {
            // Place buy order below market
            BigDecimal bidSpread = spreadPercent.multiply(
                    BigDecimal.ONE.add(BigDecimal.valueOf(random.nextDouble() * 0.5))
            );
            BigDecimal bidPrice = currentPrice.getAmount().multiply(
                    BigDecimal.ONE.subtract(bidSpread)
            );

            // Calculate quantity (smaller orders for market making)
            BigDecimal maxAffordable = availableCash.getAmount()
                    .divide(bidPrice, 8, RoundingMode.DOWN);

            BigDecimal quantity = config.getMinOrderSize().multiply(
                    BigDecimal.ONE.add(BigDecimal.valueOf(random.nextDouble()))
            );

            quantity = quantity.min(maxAffordable)
                    .min(config.getMaxOrderSize().multiply(new BigDecimal("0.5")));

            if (quantity.compareTo(config.getMinOrderSize()) >= 0) {
                lastWasBuy = true;
                return new TradingDecision(
                        TradingAction.BUY,
                        quantity,
                        String.format("Market making bid at -%.2f%%",
                                bidSpread.multiply(new BigDecimal("100"))),
                        new Money(bidPrice, Currency.USD)
                );
            }
        }

        if (!placeBuyOrder && canSell) {
            // Place sell order above market
            BigDecimal askSpread = spreadPercent.multiply(
                    BigDecimal.ONE.add(BigDecimal.valueOf(random.nextDouble() * 0.5))
            );
            BigDecimal askPrice = currentPrice.getAmount().multiply(
                    BigDecimal.ONE.add(askSpread)
            );

            // Calculate quantity
            BigDecimal quantity = config.getMinOrderSize().multiply(
                    BigDecimal.ONE.add(BigDecimal.valueOf(random.nextDouble()))
            );

            quantity = quantity.min(holdings)
                    .min(config.getMaxOrderSize().multiply(new BigDecimal("0.5")));

            if (quantity.compareTo(config.getMinOrderSize()) >= 0) {
                lastWasBuy = false;
                return new TradingDecision(
                        TradingAction.SELL,
                        quantity,
                        String.format("Market making ask at +%.2f%%",
                                askSpread.multiply(new BigDecimal("100"))),
                        new Money(askPrice, Currency.USD)
                );
            }
        }

        return TradingDecision.hold("Waiting for market making opportunity");
    }
}