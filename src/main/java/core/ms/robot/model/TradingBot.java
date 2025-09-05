package core.ms.robot.model;

import core.ms.portfolio.application.dto.command.CreatePortfolioCommand;
import core.ms.portfolio.application.dto.command.DepositCashCommand;
import core.ms.portfolio.application.dto.command.PlaceBuyOrderCommand;
import core.ms.portfolio.application.dto.command.PlaceSellOrderCommand;
import core.ms.portfolio.application.services.PortfolioApplicationService;
import core.ms.portfolio.domain.ports.inbound.PortfolioSnapshot;
import core.ms.robot.config.BotConfig;
import core.ms.robot.model.strategies.TradingStrategy;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TradingBot {
    private final String botId;
    private final String portfolioId;
    private final BotConfig config;
    private final TradingStrategy strategy;
    private final PortfolioApplicationService portfolioService;

    private BotStatus status;
    private LocalDateTime lastActionTime;
    private int tradesExecuted;
    private List<String> tradeHistory;
    private BigDecimal lastKnownPrice;

    public TradingBot(BotConfig config, TradingStrategy strategy,
                      PortfolioApplicationService portfolioService) {
        this.botId = UUID.randomUUID().toString();
        this.portfolioId = "bot-portfolio-" + botId;
        this.config = config;
        this.strategy = strategy;
        this.portfolioService = portfolioService;
        this.status = BotStatus.CREATED;
        this.tradesExecuted = 0;
        this.tradeHistory = new ArrayList<>();
    }

    public void initialize() {
        try {
            // Create portfolio for the bot
            CreatePortfolioCommand createCmd = new CreatePortfolioCommand(
                    portfolioId, "bot-" + botId
            );
            portfolioService.createPortfolio(createCmd);

            // Deposit initial cash
            DepositCashCommand depositCmd = new DepositCashCommand(
                    portfolioId, config.getInitialCash(), Currency.USD
            );
            portfolioService.depositCash(depositCmd);

            status = BotStatus.INITIALIZED;

            if (config.isAutoStart()) {
                start();
            }
        } catch (Exception e) {
            status = BotStatus.ERROR;
            throw new RuntimeException("Failed to initialize bot: " + e.getMessage(), e);
        }
    }

    public void start() {
        if (status != BotStatus.INITIALIZED && status != BotStatus.STOPPED) {
            throw new IllegalStateException("Bot must be initialized or stopped to start");
        }
        status = BotStatus.RUNNING;
    }

    public void stop() {
        status = BotStatus.STOPPED;
    }

    public void tick(Money currentPrice) {
        if (status != BotStatus.RUNNING) {
            return;
        }

        try {
            lastKnownPrice = currentPrice.getAmount();
            Symbol symbol = Symbol.createFromCode(config.getSymbolCode());

            // Get current portfolio state
            PortfolioSnapshot snapshot = portfolioService.getPortfolioSnapshot(portfolioId);

            // Let strategy decide
            TradingStrategy.TradingDecision decision = strategy.decide(
                    currentPrice, snapshot, symbol, config
            );

            // Execute decision
            switch (decision.getAction()) {
                case BUY -> executeBuy(decision, currentPrice);
                case SELL -> executeSell(decision, currentPrice);
                case HOLD -> {} // Do nothing
            }

            lastActionTime = LocalDateTime.now();

        } catch (Exception e) {
            status = BotStatus.ERROR;
            addToHistory("ERROR: " + e.getMessage());
        }
    }

    private void executeBuy(TradingStrategy.TradingDecision decision, Money price) {
        PlaceBuyOrderCommand command = new PlaceBuyOrderCommand();
        command.setPortfolioId(portfolioId);
        command.setSymbolCode(config.getSymbolCode());
        command.setPrice(price.getAmount());
        command.setCurrency(price.getCurrency().name());
        command.setQuantity(decision.getQuantity());

        try {
            portfolioService.placeBuyOrder(command);
            tradesExecuted++;
            addToHistory(String.format("BUY %.4f %s @ %s",
                    decision.getQuantity(), config.getSymbolCode(), price.toDisplayString()));
        } catch (Exception e) {
            addToHistory("Failed to execute BUY: " + e.getMessage());
        }
    }

    private void executeSell(TradingStrategy.TradingDecision decision, Money price) {
        PlaceSellOrderCommand command = new PlaceSellOrderCommand();
        command.setPortfolioId(portfolioId);
        command.setSymbolCode(config.getSymbolCode());
        command.setPrice(price.getAmount());
        command.setCurrency(price.getCurrency().name());
        command.setQuantity(decision.getQuantity());

        try {
            portfolioService.placeSellOrder(command);
            tradesExecuted++;
            addToHistory(String.format("SELL %.4f %s @ %s",
                    decision.getQuantity(), config.getSymbolCode(), price.toDisplayString()));
        } catch (Exception e) {
            addToHistory("Failed to execute SELL: " + e.getMessage());
        }
    }

    private void addToHistory(String entry) {
        tradeHistory.add(LocalDateTime.now() + " - " + entry);
        if (tradeHistory.size() > 100) {
            tradeHistory.remove(0);
        }
    }

    public void shutdown() {
        stop();
        status = BotStatus.TERMINATED;
    }

    // Getters
    public String getBotId() { return botId; }
    public String getPortfolioId() { return portfolioId; }
    public BotConfig getConfig() { return config; }
    public BotStatus getStatus() { return status; }
    public LocalDateTime getLastActionTime() { return lastActionTime; }
    public int getTradesExecuted() { return tradesExecuted; }
    public List<String> getTradeHistory() { return new ArrayList<>(tradeHistory); }
    public BigDecimal getLastKnownPrice() { return lastKnownPrice; }

    public enum BotStatus {
        CREATED, INITIALIZED, RUNNING, STOPPED, ERROR, TERMINATED
    }
}