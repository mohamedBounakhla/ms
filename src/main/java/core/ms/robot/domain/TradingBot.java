package core.ms.robot.domain;

import core.ms.portfolio.application.dto.command.CreatePortfolioCommand;
import core.ms.portfolio.application.dto.command.DepositCashCommand;
import core.ms.portfolio.application.dto.command.PlaceBuyOrderCommand;
import core.ms.portfolio.application.dto.command.PlaceSellOrderCommand;
import core.ms.portfolio.application.services.PortfolioApplicationService;
import core.ms.portfolio.domain.ports.inbound.PortfolioSnapshot;
import core.ms.robot.config.BotConfig;
import core.ms.robot.domain.strategies.TradingStrategy;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "trading_bots")
public class TradingBot {
    private static final Logger logger = LoggerFactory.getLogger(TradingBot.class);

    @Id
    @Column(name = "bot_id")
    private String botId;

    @Column(name = "portfolio_id")
    private String portfolioId;

    @Column(name = "bot_name")
    private String botName;

    @Column(name = "strategy_name")
    private String strategyName;

    @Column(name = "symbol_code")
    private String symbolCode;

    @Column(name = "initial_cash")
    private BigDecimal initialCash;

    @Column(name = "initial_assets")
    private BigDecimal initialAssets;

    @Column(name = "max_order_size")
    private BigDecimal maxOrderSize;

    @Column(name = "min_order_size")
    private BigDecimal minOrderSize;

    @Column(name = "tick_interval")
    private Integer tickIntervalSeconds;

    @Column(name = "risk_tolerance")
    private BigDecimal riskTolerance;

    @Column(name = "auto_start")
    private boolean autoStart;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private BotStatus status;

    @Column(name = "last_action_time")
    private LocalDateTime lastActionTime;

    @Column(name = "trades_executed")
    private int tradesExecuted;

    @Column(name = "last_known_price")
    private BigDecimal lastKnownPrice;

    @ElementCollection
    @CollectionTable(name = "bot_trade_history", joinColumns = @JoinColumn(name = "bot_id"))
    @Column(name = "entry")
    private List<String> tradeHistory = new ArrayList<>();

    @Transient
    private TradingStrategy strategy;

    @Transient
    private PortfolioApplicationService portfolioService;

    @Transient
    private TradeEventCallback tradeEventCallback;

    // Callback interface for trade events
    @FunctionalInterface
    public interface TradeEventCallback {
        void onTradeEvent(TradingBot bot, TradingStrategy.TradingDecision decision,
                          Money price, boolean success, String error);
    }

    // JPA constructor
    protected TradingBot() {}

    // Main constructor
    public TradingBot(BotConfig config, TradingStrategy strategy,
                      PortfolioApplicationService portfolioService,
                      TradeEventCallback tradeEventCallback) {
        this.botId = UUID.randomUUID().toString();
        this.portfolioId = "bot-portfolio-" + botId;
        this.botName = config.getBotName();
        this.strategyName = config.getStrategy();
        this.symbolCode = config.getSymbolCode();
        this.initialCash = config.getInitialCash();
        this.initialAssets = config.getInitialAssets() != null ?
                config.getInitialAssets() : BigDecimal.ZERO;
        this.maxOrderSize = config.getMaxOrderSize();
        this.minOrderSize = config.getMinOrderSize();
        this.tickIntervalSeconds = config.getTickIntervalSeconds();
        this.riskTolerance = config.getRiskTolerance();
        this.autoStart = config.isAutoStart();
        this.strategy = strategy;
        this.portfolioService = portfolioService;
        this.tradeEventCallback = tradeEventCallback;
        this.status = BotStatus.CREATED;
        this.tradesExecuted = 0;
    }

    // Set transient dependencies after loading from DB
    public void setDependencies(TradingStrategy strategy, PortfolioApplicationService portfolioService, TradeEventCallback callback) {
        this.strategy = strategy;
        this.portfolioService = portfolioService;
        this.tradeEventCallback = callback;
    }

    public void initialize() {
        if (status != BotStatus.CREATED) {
            logger.warn("Bot {} already initialized with status: {}", botId, status);
            return;
        }

        try {
            // Check if portfolio already exists
            if (portfolioService.findPortfolioById(portfolioId).isPresent()) {
                logger.info("Portfolio {} already exists for bot {}", portfolioId, botId);
                status = BotStatus.INITIALIZED;
                if (autoStart) {
                    start();
                }
                return;
            }

            // Create portfolio for the bot
            CreatePortfolioCommand createCmd = new CreatePortfolioCommand(
                    portfolioId, "bot-" + botId
            );
            var result = portfolioService.createPortfolio(createCmd);

            if (!result.isSuccess()) {
                throw new RuntimeException("Failed to create portfolio: " + result.getMessage());
            }

            // Wait a bit for transaction to commit
            Thread.sleep(100);

            // Deposit initial cash
            DepositCashCommand depositCmd = new DepositCashCommand(
                    portfolioId, initialCash, Currency.USD
            );
            var depositResult = portfolioService.depositCash(depositCmd);

            if (!depositResult.isSuccess()) {
                throw new RuntimeException("Failed to deposit cash: " + depositResult.getMessage());
            }

            status = BotStatus.INITIALIZED;

            String initMessage = String.format("Bot initialized with portfolio %s (Cash: $%s",
                    portfolioId, initialCash);
            if (initialAssets != null && initialAssets.compareTo(BigDecimal.ZERO) > 0) {
                initMessage += String.format(", Assets: %s %s", initialAssets, symbolCode);
            }
            initMessage += ")";
            addToHistory(initMessage);

            if (autoStart) {
                start();
            }
        } catch (Exception e) {
            status = BotStatus.ERROR;
            addToHistory("ERROR during initialization: " + e.getMessage());
            throw new RuntimeException("Failed to initialize bot: " + e.getMessage(), e);
        }
    }

    public void start() {
        if (status != BotStatus.INITIALIZED && status != BotStatus.STOPPED) {
            throw new IllegalStateException("Bot must be initialized or stopped to start");
        }
        status = BotStatus.RUNNING;
        addToHistory("Bot started");
    }

    public void stop() {
        status = BotStatus.STOPPED;
        addToHistory("Bot stopped");
    }

    public PortfolioApplicationService getPortfolioService() {
        return portfolioService;
    }

    public void tick(Money currentMarketPrice) {
        if (status != BotStatus.RUNNING) {
            return;
        }

        try {
            lastKnownPrice = currentMarketPrice.getAmount();
            Symbol symbol = Symbol.createFromCode(symbolCode);

            // Get current portfolio state
            PortfolioSnapshot snapshot = portfolioService.getPortfolioSnapshot(portfolioId);

            // Let strategy decide
            TradingStrategy.TradingDecision decision = strategy.decide(
                    currentMarketPrice, snapshot, symbol, getConfig()
            );

            // Use custom price if provided by strategy, otherwise use market price
            Money orderPrice = decision.hasCustomPrice() ?
                    decision.getCustomPrice() : currentMarketPrice;

            // Execute decision and notify via callback
            switch (decision.getAction()) {
                case BUY -> executeBuy(decision, orderPrice);
                case SELL -> executeSell(decision, orderPrice);
                case HOLD -> {
                    if (tradeEventCallback != null) {
                        tradeEventCallback.onTradeEvent(this, decision, currentMarketPrice, true, null);
                    }
                }
            }

            lastActionTime = LocalDateTime.now();

        } catch (Exception e) {
            status = BotStatus.ERROR;
            addToHistory("ERROR: " + e.getMessage());

            if (tradeEventCallback != null) {
                tradeEventCallback.onTradeEvent(this, null, null, false, e.getMessage());
            }
        }
    }

    private void executeBuy(TradingStrategy.TradingDecision decision, Money price) {
        PlaceBuyOrderCommand command = new PlaceBuyOrderCommand();
        command.setPortfolioId(portfolioId);
        command.setSymbolCode(symbolCode);
        command.setPrice(price.getAmount());
        command.setCurrency(price.getCurrency().name());
        command.setQuantity(decision.getQuantity());

        try {
            var result = portfolioService.placeBuyOrder(command);

            // Only increment if successful
            if (result.isSuccess()) {
                tradesExecuted++;
                String historyEntry = String.format("BUY %.4f %s @ %s (%s)",
                        decision.getQuantity(), symbolCode, price.toDisplayString(), decision.getReason());
                addToHistory(historyEntry);

                if (tradeEventCallback != null) {
                    tradeEventCallback.onTradeEvent(this, decision, price, true, null);
                }
            } else {
                // Order failed but don't increment trades
                addToHistory("Failed to execute BUY: " + result.getMessage());
                if (tradeEventCallback != null) {
                    tradeEventCallback.onTradeEvent(this, decision, price, false, result.getMessage());
                }
            }
        } catch (Exception e) {
            // Don't increment trades on exception
            addToHistory("Failed to execute BUY: " + e.getMessage());
            if (tradeEventCallback != null) {
                tradeEventCallback.onTradeEvent(this, decision, price, false, e.getMessage());
            }
        }
    }

    private void executeSell(TradingStrategy.TradingDecision decision, Money price) {
        PlaceSellOrderCommand command = new PlaceSellOrderCommand();
        command.setPortfolioId(portfolioId);
        command.setSymbolCode(symbolCode);
        command.setPrice(price.getAmount());
        command.setCurrency(price.getCurrency().name());
        command.setQuantity(decision.getQuantity());

        try {
            var result = portfolioService.placeSellOrder(command);

            // Only increment if successful
            if (result.isSuccess()) {
                tradesExecuted++;
                String historyEntry = String.format("SELL %.4f %s @ %s (%s)",
                        decision.getQuantity(), symbolCode, price.toDisplayString(), decision.getReason());
                addToHistory(historyEntry);

                if (tradeEventCallback != null) {
                    tradeEventCallback.onTradeEvent(this, decision, price, true, null);
                }
            } else {
                // Order failed but don't increment trades
                addToHistory("Failed to execute SELL: " + result.getMessage());
                if (tradeEventCallback != null) {
                    tradeEventCallback.onTradeEvent(this, decision, price, false, result.getMessage());
                }
            }
        } catch (Exception e) {
            // Don't increment trades on exception
            addToHistory("Failed to execute SELL: " + e.getMessage());
            if (tradeEventCallback != null) {
                tradeEventCallback.onTradeEvent(this, decision, price, false, e.getMessage());
            }
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
        addToHistory("Bot terminated");
    }

    public BotConfig getConfig() {
        BotConfig config = new BotConfig();
        config.setBotName(botName);
        config.setInitialCash(initialCash);
        config.setInitialAssets(initialAssets);
        config.setStrategy(strategyName);
        config.setSymbolCode(symbolCode);
        config.setMaxOrderSize(maxOrderSize);
        config.setMinOrderSize(minOrderSize);
        config.setTickIntervalSeconds(tickIntervalSeconds);
        config.setRiskTolerance(riskTolerance);
        config.setAutoStart(autoStart);
        return config;
    }

    // Getters
    public String getBotId() { return botId; }
    public String getPortfolioId() { return portfolioId; }
    public String getBotName() { return botName; }
    public String getStrategyName() { return strategyName; }
    public String getSymbolCode() { return symbolCode; }
    public BotStatus getStatus() { return status; }
    public LocalDateTime getLastActionTime() { return lastActionTime; }
    public int getTradesExecuted() { return tradesExecuted; }
    public List<String> getTradeHistory() { return new ArrayList<>(tradeHistory); }
    public BigDecimal getLastKnownPrice() { return lastKnownPrice; }
    public BigDecimal getInitialAssets() { return initialAssets; }

    public enum BotStatus {
        CREATED, INITIALIZED, RUNNING, STOPPED, ERROR, TERMINATED
    }
}