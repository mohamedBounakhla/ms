package core.ms.robot.domain.strategies;

import core.ms.order_book.application.dto.query.OrderBookTickerDTO;
import core.ms.order_book.application.services.OrderBookApplicationService;
import core.ms.portfolio.domain.ports.inbound.PortfolioSnapshot;
import core.ms.robot.config.BotConfig;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

/**
 * Strategy that competes by improving on existing order book prices
 * Creates natural price discovery through aggressive competition
 */
public class CompetitiveStrategy implements TradingStrategy {

    private final OrderBookApplicationService orderBookService;
    private final Random random = new Random();
    private final BigDecimal TICK_SIZE = new BigDecimal("50"); // Price increment

    // Track recent activity to become more aggressive
    private int ticksSinceLastTrade = 0;
    private BigDecimal lastTradePrice = null;
    private boolean lastActionWasBuy = false;

    public CompetitiveStrategy(OrderBookApplicationService orderBookService) {
        this.orderBookService = orderBookService;
    }

    @Override
    public TradingDecision decide(Money currentPrice, PortfolioSnapshot snapshot,
                                  Symbol symbol, BotConfig config) {

        ticksSinceLastTrade++;

        // Get the actual order book state
        OrderBookTickerDTO ticker = orderBookService.getOrderBookTicker(symbol);

        BigDecimal holdings = snapshot.getPositions().getOrDefault(symbol, BigDecimal.ZERO);
        Money availableCash = snapshot.getCashBalances().getOrDefault(
                Currency.USD, Money.zero(Currency.USD)
        );

        // Skip if no resources
        if (!availableCash.isPositive() && holdings.compareTo(BigDecimal.ZERO) == 0) {
            return TradingDecision.hold("No resources");
        }

        // Determine aggressiveness based on recent activity
        double aggressiveness = calculateAggressiveness();

        // Decide action based on inventory balance and market state
        boolean shouldBuy = needsToBuy(availableCash, holdings, currentPrice);

        if (shouldBuy && availableCash.isPositive()) {
            return placeAggressiveBuyOrder(ticker, availableCash, config, symbol, aggressiveness);
        } else if (!shouldBuy && holdings.compareTo(BigDecimal.ZERO) > 0) {
            return placeAggressiveSellOrder(ticker, holdings, config, symbol, aggressiveness);
        }

        return TradingDecision.hold("Waiting for opportunity");
    }

    private double calculateAggressiveness() {
        // Become more aggressive if no trades recently (market is stuck)
        if (ticksSinceLastTrade > 5) {
            return 0.8; // Very aggressive
        } else if (ticksSinceLastTrade > 3) {
            return 0.6; // Moderately aggressive
        } else {
            return 0.4; // Normal competition
        }
    }

    private TradingDecision placeAggressiveBuyOrder(OrderBookTickerDTO ticker,
                                                    Money availableCash,
                                                    BotConfig config,
                                                    Symbol symbol,
                                                    double aggressiveness) {
        BigDecimal buyPrice;
        String reason;

        if (ticker.getBidPrice() != null && ticker.getAskPrice() != null) {
            // Market exists with both sides
            BigDecimal spread = ticker.getAskPrice().subtract(ticker.getBidPrice());

            if (spread.compareTo(BigDecimal.ZERO) <= 0) {
                // Spread crossed or touching - be aggressive to push price up
                if (random.nextDouble() < aggressiveness) {
                    // Jump above current ask to reach higher sells
                    buyPrice = ticker.getAskPrice().add(TICK_SIZE.multiply(
                            BigDecimal.valueOf(1 + random.nextInt(3))
                    ));
                    reason = "Aggressive jump buy (pushing price up)";
                } else {
                    // Match the ask
                    buyPrice = ticker.getAskPrice();
                    reason = "Taking liquidity at ask";
                }
            } else if (spread.compareTo(TICK_SIZE.multiply(new BigDecimal("3"))) > 0) {
                // Wide spread - place inside aggressively
                BigDecimal increment = spread.divide(new BigDecimal("3"), 0, RoundingMode.UP);
                buyPrice = ticker.getBidPrice().add(increment);
                reason = "Aggressive bid improvement (wide spread)";
            } else {
                // Tight spread - compete or cross based on aggressiveness
                if (random.nextDouble() < aggressiveness) {
                    // Cross the spread or go higher
                    if (random.nextDouble() < 0.5) {
                        buyPrice = ticker.getAskPrice();
                        reason = "Crossing spread (aggressive)";
                    } else {
                        // Jump above ask to discover new prices
                        buyPrice = ticker.getAskPrice().add(TICK_SIZE);
                        reason = "Price discovery buy (above ask)";
                    }
                } else {
                    // Improve bid
                    buyPrice = ticker.getBidPrice().add(TICK_SIZE);
                    reason = "Improving bid";
                }
            }
        } else if (ticker.getBidPrice() != null) {
            // Only bids exist - be aggressive
            BigDecimal jumpSize = TICK_SIZE.multiply(BigDecimal.valueOf(1 + aggressiveness * 3));
            buyPrice = ticker.getBidPrice().add(jumpSize);
            reason = "Aggressive bid jump (no asks)";
        } else if (ticker.getAskPrice() != null) {
            // Only asks exist - try to reach them
            if (random.nextDouble() < aggressiveness) {
                buyPrice = ticker.getAskPrice();
                reason = "Taking ask (no bids)";
            } else {
                buyPrice = ticker.getAskPrice().multiply(new BigDecimal("0.99"));
                reason = "Starting bid below ask";
            }
        } else {
            // Empty book - set initial price
            buyPrice = lastTradePrice != null ?
                    lastTradePrice.add(TICK_SIZE) : new BigDecimal("45000");
            reason = "Market maker bid (empty book)";
        }

        // Calculate quantity - be more aggressive with size too
        BigDecimal maxAffordable = availableCash.getAmount()
                .divide(buyPrice, 8, RoundingMode.DOWN);

        BigDecimal sizeMultiplier = BigDecimal.valueOf(0.3 + aggressiveness * 0.4);
        BigDecimal quantity = config.getMaxOrderSize().multiply(sizeMultiplier);
        quantity = quantity.min(maxAffordable);

        if (quantity.compareTo(config.getMinOrderSize()) >= 0) {
            lastActionWasBuy = true;
            ticksSinceLastTrade = 0;
            lastTradePrice = buyPrice;

            return new TradingDecision(
                    TradingAction.BUY,
                    quantity,
                    reason + String.format(" @ %s (aggr: %.1f)", buyPrice, aggressiveness),
                    new Money(buyPrice, Currency.USD)
            );
        }

        return TradingDecision.hold("Insufficient funds");
    }

    private TradingDecision placeAggressiveSellOrder(OrderBookTickerDTO ticker,
                                                     BigDecimal holdings,
                                                     BotConfig config,
                                                     Symbol symbol,
                                                     double aggressiveness) {
        BigDecimal sellPrice;
        String reason;

        if (ticker.getBidPrice() != null && ticker.getAskPrice() != null) {
            // Market exists with both sides
            BigDecimal spread = ticker.getAskPrice().subtract(ticker.getBidPrice());

            if (spread.compareTo(BigDecimal.ZERO) <= 0) {
                // Spread crossed or touching - be less aggressive on sells
                if (random.nextDouble() < (aggressiveness * 0.5)) {
                    // Drop below bid to take liquidity
                    sellPrice = ticker.getBidPrice().subtract(TICK_SIZE);
                    reason = "Aggressive sell below bid";
                } else {
                    // Match the bid
                    sellPrice = ticker.getBidPrice();
                    reason = "Taking liquidity at bid";
                }
            } else if (spread.compareTo(TICK_SIZE.multiply(new BigDecimal("3"))) > 0) {
                // Wide spread - place inside
                BigDecimal increment = spread.divide(new BigDecimal("3"), 0, RoundingMode.DOWN);
                sellPrice = ticker.getAskPrice().subtract(increment);
                reason = "Aggressive ask improvement (wide spread)";
            } else {
                // Tight spread
                if (random.nextDouble() < (aggressiveness * 0.7)) {
                    // Cross the spread
                    sellPrice = ticker.getBidPrice();
                    reason = "Crossing spread to sell";
                } else {
                    // Improve ask
                    sellPrice = ticker.getAskPrice().subtract(TICK_SIZE);
                    reason = "Improving ask";
                }
            }
        } else if (ticker.getAskPrice() != null) {
            // Only asks exist - compete aggressively
            sellPrice = ticker.getAskPrice().subtract(TICK_SIZE);
            reason = "Undercutting ask";
        } else if (ticker.getBidPrice() != null) {
            // Only bids exist - place ask
            if (random.nextDouble() < aggressiveness) {
                sellPrice = ticker.getBidPrice();
                reason = "Taking bid (no asks)";
            } else {
                sellPrice = ticker.getBidPrice().multiply(new BigDecimal("1.01"));
                reason = "Starting ask above bid";
            }
        } else {
            // Empty book
            sellPrice = lastTradePrice != null ?
                    lastTradePrice : new BigDecimal("45100");
            reason = "Market maker ask (empty book)";
        }

        // Calculate quantity
        BigDecimal sizeMultiplier = BigDecimal.valueOf(0.3 + aggressiveness * 0.3);
        BigDecimal quantity = holdings.multiply(sizeMultiplier);
        quantity = quantity.min(config.getMaxOrderSize());

        if (quantity.compareTo(config.getMinOrderSize()) >= 0) {
            lastActionWasBuy = false;
            ticksSinceLastTrade = 0;
            lastTradePrice = sellPrice;

            return new TradingDecision(
                    TradingAction.SELL,
                    quantity,
                    reason + String.format(" @ %s (aggr: %.1f)", sellPrice, aggressiveness),
                    new Money(sellPrice, Currency.USD)
            );
        }

        return TradingDecision.hold("Insufficient assets");
    }

    private boolean needsToBuy(Money cash, BigDecimal holdings, Money currentPrice) {
        if (holdings.compareTo(BigDecimal.ZERO) == 0) {
            return true; // No assets, should buy
        }

        if (!cash.isPositive()) {
            return false; // No cash, can't buy
        }

        // Calculate portfolio balance
        BigDecimal holdingsValue = holdings.multiply(currentPrice.getAmount());
        BigDecimal totalValue = cash.getAmount().add(holdingsValue);

        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }

        BigDecimal cashRatio = cash.getAmount().divide(totalValue, 4, RoundingMode.HALF_UP);

        // Alternate more frequently to create market movement
        if (ticksSinceLastTrade > 3) {
            // Been too long, switch action
            return !lastActionWasBuy;
        }

        // Try to maintain 50/50 balance but with some randomness
        return cashRatio.compareTo(new BigDecimal("0.5")) > 0 ||
                (cashRatio.compareTo(new BigDecimal("0.3")) > 0 && random.nextDouble() < 0.4);
    }
}