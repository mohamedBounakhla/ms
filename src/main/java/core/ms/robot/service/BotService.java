package core.ms.robot.service;

import core.ms.portfolio.application.services.PortfolioApplicationService;
import core.ms.portfolio.infrastructure.adapters.MarketDataAdapterImpl;
import core.ms.robot.dto.BotStatusDTO;
import core.ms.robot.config.BotConfig;
import core.ms.robot.model.TradingBot;
import core.ms.robot.model.strategies.MomentumStrategy;
import core.ms.robot.model.strategies.RandomStrategy;
import core.ms.robot.model.strategies.TradingStrategy;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BotService {
    private final Map<String, TradingBot> activeBots = new ConcurrentHashMap<>();

    @Autowired
    private PortfolioApplicationService portfolioService;

    @Autowired
    private MarketDataAdapterImpl marketDataAdapter;

    public String createAndStartBot(BotConfig config) {
        // Create strategy based on config
        TradingStrategy strategy = createStrategy(config.getStrategy());

        // Create bot
        TradingBot bot = new TradingBot(config, strategy, portfolioService);
        bot.initialize();

        // Store and return ID
        activeBots.put(bot.getBotId(), bot);
        return bot.getBotId();
    }

    public void stopBot(String botId) {
        TradingBot bot = activeBots.get(botId);
        if (bot != null) {
            bot.stop();
        }
    }

    public void startBot(String botId) {
        TradingBot bot = activeBots.get(botId);
        if (bot != null) {
            bot.start();
        }
    }

    public void removeBot(String botId) {
        TradingBot bot = activeBots.remove(botId);
        if (bot != null) {
            bot.shutdown();
        }
    }

    public Optional<BotStatusDTO> getBotStatus(String botId) {
        TradingBot bot = activeBots.get(botId);
        if (bot == null) {
            return Optional.empty();
        }

        return Optional.of(mapToStatusDTO(bot));
    }

    public List<BotStatusDTO> getAllBotStatuses() {
        return activeBots.values().stream()
                .map(this::mapToStatusDTO)
                .toList();
    }

    @Scheduled(fixedDelay = 5000) // Run every 5 seconds
    public void tickAllBots() {
        // Process each bot in parallel
        activeBots.values().parallelStream().forEach(bot -> {
            if (bot.getStatus() == TradingBot.BotStatus.RUNNING) {
                try {
                    Symbol symbol = Symbol.createFromCode(bot.getConfig().getSymbolCode());
                    Optional<Money> priceOpt = marketDataAdapter.getCurrentPrice(symbol);

                    if (priceOpt.isPresent()) {
                        bot.tick(priceOpt.get());
                    }
                } catch (Exception e) {
                    // Log error but don't crash the scheduler
                    System.err.println("Error ticking bot " + bot.getBotId() + ": " + e.getMessage());
                }
            }
        });
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
        dto.setBotName(bot.getConfig().getBotName());
        dto.setStrategy(bot.getConfig().getStrategy());
        dto.setSymbol(bot.getConfig().getSymbolCode());
        dto.setStatus(bot.getStatus().name());
        dto.setTradesExecuted(bot.getTradesExecuted());
        dto.setLastActionTime(bot.getLastActionTime());
        dto.setLastKnownPrice(bot.getLastKnownPrice());
        dto.setRecentHistory(bot.getTradeHistory().stream()
                .skip(Math.max(0, bot.getTradeHistory().size() - 5))
                .toList());
        return dto;
    }

    // Utility method to stop all bots (useful for shutdown)
    public void stopAllBots() {
        activeBots.values().forEach(TradingBot::stop);
    }
}