package core.ms.robot.service;

import core.ms.portfolio.application.services.PortfolioApplicationService;
import core.ms.portfolio.domain.ports.inbound.PortfolioSnapshot;
import core.ms.portfolio.infrastructure.adapters.MarketDataAdapterImpl;
import core.ms.robot.dto.BotListUpdateDTO;
import core.ms.robot.dto.BotStatusDTO;
import core.ms.robot.dto.BotTradeEventDTO;
import core.ms.robot.config.BotConfig;
import core.ms.robot.domain.TradingBot;
import core.ms.robot.domain.strategies.MomentumStrategy;
import core.ms.robot.domain.strategies.RandomStrategy;
import core.ms.robot.domain.strategies.TradingStrategy;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import core.ms.robot.dao.TradingBotDAO;
import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class BotService {

    private static final Logger logger = LoggerFactory.getLogger(BotService.class);

    private final Map<String, TradingBot> activeBots = new ConcurrentHashMap<>();

    @Autowired
    private PortfolioApplicationService portfolioService;

    @Autowired
    private MarketDataAdapterImpl marketDataAdapter;

    @Autowired
    private BotWebSocketService webSocketService;

    @Autowired
    private TradingBotDAO botDAO;

    @PostConstruct
    public void loadBots() {
        // Load all non-terminated bots on startup
        logger.info("Loading persisted bots...");
        List<TradingBot> savedBots = botDAO.findAll();

        for (TradingBot bot : savedBots) {
            if (bot.getStatus() != TradingBot.BotStatus.TERMINATED) {
                // Recreate strategy and set dependencies
                TradingStrategy strategy = createStrategy(bot.getStrategyName());
                bot.setDependencies(strategy, portfolioService, this::handleBotTradeEvent);
                activeBots.put(bot.getBotId(), bot);

                logger.info("Loaded bot: {} ({}) - Status: {}",
                        bot.getBotId(), bot.getBotName(), bot.getStatus());

                // Auto-start if it was running
                if (bot.getStatus() == TradingBot.BotStatus.RUNNING) {
                    bot.start();
                }
            }
        }
        logger.info("Loaded {} bots from database", activeBots.size());
    }

    public String createAndStartBot(BotConfig config) {
        try {
            // Create strategy based on config
            TradingStrategy strategy = createStrategy(config.getStrategy());

            // Create bot with WebSocket callback
            TradingBot bot = new TradingBot(config, strategy, portfolioService,
                    this::handleBotTradeEvent);
            bot.initialize();

            // Store bot in memory
            activeBots.put(bot.getBotId(), bot);

            // Save to database
            botDAO.save(bot);

            // Broadcast bot creation
            BotStatusDTO status = mapToStatusDTO(bot);
            webSocketService.broadcastBotListUpdate(
                    BotListUpdateDTO.botCreated(bot.getBotId(), config.getBotName(), status)
            );

            logger.info("Bot created and started: {} ({})", bot.getBotId(), config.getBotName());
            return bot.getBotId();

        } catch (Exception e) {
            logger.error("Failed to create bot", e);
            throw new RuntimeException("Failed to create bot: " + e.getMessage(), e);
        }
    }

    public void stopBot(String botId) {
        TradingBot bot = activeBots.get(botId);
        if (bot != null) {
            bot.stop();

            // Save status change to database
            botDAO.save(bot);

            // Broadcast status change
            webSocketService.broadcastBotListUpdate(
                    BotListUpdateDTO.botStatusChanged(botId, bot.getBotName(), "STOPPED")
            );
            webSocketService.broadcastBotStatus(mapToStatusDTO(bot));

            logger.info("Bot stopped: {}", botId);
        }
    }

    public void startBot(String botId) {
        TradingBot bot = activeBots.get(botId);
        if (bot != null) {
            bot.start();

            // Save status change to database
            botDAO.save(bot);

            // Broadcast status change
            webSocketService.broadcastBotListUpdate(
                    BotListUpdateDTO.botStatusChanged(botId, bot.getBotName(), "STARTED")
            );
            webSocketService.broadcastBotStatus(mapToStatusDTO(bot));

            logger.info("Bot started: {}", botId);
        }
    }

    public void removeBot(String botId) {
        TradingBot bot = activeBots.remove(botId);
        if (bot != null) {
            String botName = bot.getBotName();
            bot.shutdown();

            // Save final status to database
            botDAO.save(bot);

            // Broadcast bot removal
            webSocketService.broadcastBotListUpdate(
                    BotListUpdateDTO.botRemoved(botId, botName)
            );

            logger.info("Bot removed: {} ({})", botId, botName);
        }
    }

    public Optional<BotStatusDTO> getBotStatus(String botId) {
        TradingBot bot = activeBots.get(botId);
        if (bot == null) {
            return Optional.empty();
        }

        return Optional.of(mapToStatusDTOWithPnL(bot));
    }

    public List<BotStatusDTO> getAllBotStatuses() {
        return activeBots.values().stream()
                .map(this::mapToStatusDTOWithPnL)
                .collect(Collectors.toList());
    }

    @Scheduled(fixedDelay = 2000) // Run every 2 seconds for more activity
    public void tickAllBots() {
        List<TradingBot> botsToSave = new ArrayList<>();

        List<BotStatusDTO> allStatuses = activeBots.values().parallelStream()
                .filter(bot -> bot.getStatus() == TradingBot.BotStatus.RUNNING)
                .map(bot -> {
                    try {
                        Symbol symbol = Symbol.createFromCode(bot.getSymbolCode());
                        Optional<Money> priceOpt = marketDataAdapter.getCurrentPrice(symbol);

                        if (priceOpt.isPresent()) {
                            int previousTrades = bot.getTradesExecuted();
                            bot.tick(priceOpt.get());

                            // Mark for save if trades were executed
                            if (bot.getTradesExecuted() > previousTrades) {
                                botsToSave.add(bot);
                            }

                            return mapToStatusDTOWithPnL(bot);
                        }
                    } catch (Exception e) {
                        logger.error("Error ticking bot " + bot.getBotId() + ": " + e.getMessage());
                        webSocketService.broadcastBotError(bot.getBotId(), e.getMessage());
                    }
                    return null;
                })
                .filter(status -> status != null)
                .collect(Collectors.toList());

        // Save bots that executed trades
        for (TradingBot bot : botsToSave) {
            try {
                botDAO.save(bot);
            } catch (Exception e) {
                logger.error("Failed to save bot state: {}", bot.getBotId(), e);
            }
        }

        // Broadcast bulk update if we have updates
        if (!allStatuses.isEmpty()) {
            webSocketService.broadcastBulkStatusUpdate(allStatuses);
        }
    }

    /**
     * Callback for bot trade events
     */
    private void handleBotTradeEvent(TradingBot bot, TradingStrategy.TradingDecision decision,
                                     Money price, boolean success, String error) {
        BotTradeEventDTO tradeEvent;

        if (decision != null) {
            if (success) {
                tradeEvent = new BotTradeEventDTO(
                        bot.getBotId(),
                        bot.getBotName(),
                        bot.getPortfolioId(),
                        decision.getAction().name(),
                        bot.getSymbolCode(),
                        decision.getQuantity(),
                        price != null ? price.getAmount() : null,
                        decision.getReason()
                );
            } else {
                tradeEvent = new BotTradeEventDTO(
                        bot.getBotId(),
                        bot.getBotName(),
                        bot.getPortfolioId(),
                        decision.getAction().name(),
                        bot.getSymbolCode(),
                        error
                );
            }

            // Broadcast trade event
            webSocketService.broadcastTradeEvent(tradeEvent);
        }

        // Also broadcast updated bot status
        webSocketService.broadcastBotStatus(mapToStatusDTOWithPnL(bot));
    }

    private TradingStrategy createStrategy(String strategyName) {
        return switch (strategyName.toLowerCase()) {
            case "random" -> new RandomStrategy();
            case "momentum" -> new MomentumStrategy();
            default -> new RandomStrategy();
        };
    }

    private BotStatusDTO mapToStatusDTO(TradingBot bot) {
        BotStatusDTO dto = new BotStatusDTO();
        dto.setBotId(bot.getBotId());
        dto.setPortfolioId(bot.getPortfolioId());
        dto.setBotName(bot.getBotName());
        dto.setStrategy(bot.getStrategyName());
        dto.setSymbol(bot.getSymbolCode());
        dto.setStatus(bot.getStatus().name());
        dto.setTradesExecuted(bot.getTradesExecuted());
        dto.setLastActionTime(bot.getLastActionTime());
        dto.setLastKnownPrice(bot.getLastKnownPrice());
        dto.setRecentHistory(bot.getTradeHistory().stream()
                .skip(Math.max(0, bot.getTradeHistory().size() - 5))
                .toList());
        return dto;
    }

    private BotStatusDTO mapToStatusDTOWithPnL(TradingBot bot) {
        BotStatusDTO dto = mapToStatusDTO(bot);

        // Calculate P&L from portfolio
        try {
            PortfolioSnapshot snapshot = portfolioService.getPortfolioSnapshot(bot.getPortfolioId());

            // Calculate total portfolio value
            BigDecimal totalValue = BigDecimal.ZERO;

            // Add cash values
            Money usdCash = snapshot.getCashBalances().get(Currency.USD);
            if (usdCash != null) {
                totalValue = totalValue.add(usdCash.getAmount());
            }

            // Add position values (simplified - assumes USD pricing)
            Symbol btcSymbol = Symbol.createFromCode(bot.getSymbolCode());
            BigDecimal holdings = snapshot.getPositions().get(btcSymbol);
            if (holdings != null && holdings.compareTo(BigDecimal.ZERO) > 0) {
                Optional<Money> currentPrice = marketDataAdapter.getCurrentPrice(btcSymbol);
                if (currentPrice.isPresent()) {
                    BigDecimal positionValue = holdings.multiply(currentPrice.get().getAmount());
                    totalValue = totalValue.add(positionValue);
                }
            }

            // Calculate P&L
            BigDecimal initialCash = bot.getConfig().getInitialCash();
            BigDecimal pnl = totalValue.subtract(initialCash);
            BigDecimal pnlPercent = BigDecimal.ZERO;
            if (initialCash.compareTo(BigDecimal.ZERO) > 0) {
                pnlPercent = pnl.divide(initialCash, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }

            dto.setTotalValue(totalValue);
            dto.setPnl(pnl);
            dto.setPnlPercent(pnlPercent);
            dto.setCashBalance(usdCash != null ? usdCash.getAmount() : BigDecimal.ZERO);
            dto.setPositionSize(holdings != null ? holdings : BigDecimal.ZERO);

        } catch (Exception e) {
            logger.error("Failed to calculate P&L for bot {}", bot.getBotId(), e);
        }

        return dto;
    }

    // Utility method to stop all bots (useful for shutdown)
    public void stopAllBots() {
        activeBots.values().forEach(bot -> {
            bot.stop();
            try {
                botDAO.save(bot);
            } catch (Exception e) {
                logger.error("Failed to save bot on shutdown: {}", bot.getBotId(), e);
            }
        });
        logger.info("All bots stopped");
    }

    // Additional utility method for cleanup
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void cleanupTerminatedBots() {
        List<TradingBot> allBots = botDAO.findAll();
        int cleaned = 0;

        for (TradingBot bot : allBots) {
            if (bot.getStatus() == TradingBot.BotStatus.TERMINATED &&
                    !activeBots.containsKey(bot.getBotId())) {
                // Bot is terminated and not in active memory, safe to clean history
                if (bot.getTradeHistory().size() > 20) {
                    // Keep only last 20 entries for terminated bots
                    List<String> history = bot.getTradeHistory();
                    history.subList(0, history.size() - 20).clear();
                    botDAO.save(bot);
                    cleaned++;
                }
            }
        }

        if (cleaned > 0) {
            logger.info("Cleaned up history for {} terminated bots", cleaned);
        }
    }
}