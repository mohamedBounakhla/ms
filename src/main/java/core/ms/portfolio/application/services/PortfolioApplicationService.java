package core.ms.portfolio.application.services;

import core.ms.portfolio.application.dto.command.*;
import core.ms.portfolio.application.dto.query.PortfolioDTO;
import core.ms.portfolio.application.dto.query.PortfolioOperationResultDTO;
import core.ms.portfolio.domain.Portfolio;
import core.ms.portfolio.domain.cash.CashManager;
import core.ms.portfolio.domain.events.subscribe.OrderCreatedEvent;
import core.ms.portfolio.domain.events.subscribe.OrderCreationFailedEvent;
import core.ms.portfolio.domain.events.subscribe.TransactionCreatedEvent;
import core.ms.portfolio.domain.ports.inbound.PortfolioSnapshot;
import core.ms.portfolio.domain.ports.outbound.*;
import core.ms.portfolio.domain.positions.PositionManager;
import core.ms.shared.OrderType;
import core.ms.shared.events.CorrelationAwareEventListener;
import core.ms.shared.events.DomainEvent;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class PortfolioApplicationService extends CorrelationAwareEventListener {

    private static final Logger logger = LoggerFactory.getLogger(PortfolioApplicationService.class);

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private PortfolioEventPublisher eventPublisher;

    @Autowired
    private MarketDataAdapter marketDataAdapter;

    // ===== PORTFOLIO MANAGEMENT =====

    public PortfolioOperationResultDTO createPortfolio(CreatePortfolioCommand command) {
        try {
            if (portfolioRepository.existsById(command.getPortfolioId())) {
                return PortfolioOperationResultDTO.error(
                        command.getPortfolioId(),
                        "Portfolio already exists"
                );
            }

            CashManager cashManager = new CashManager();
            PositionManager positionManager = new PositionManager();
            Portfolio portfolio = new Portfolio(
                    command.getPortfolioId(),
                    command.getOwnerId(),
                    cashManager,
                    positionManager
            );

            portfolioRepository.save(portfolio);
            logger.info("Portfolio created: {} for owner: {}",
                    portfolio.getPortfolioId(), portfolio.getOwnerId());

            return PortfolioOperationResultDTO.success(
                    portfolio.getPortfolioId(),
                    "Portfolio created successfully"
            );
        } catch (Exception e) {
            logger.error("Failed to create portfolio", e);
            return PortfolioOperationResultDTO.error(
                    command.getPortfolioId(),
                    "Failed to create portfolio: " + e.getMessage()
            );
        }
    }

    public Optional<PortfolioDTO> findPortfolioByIdAsDTO(String portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .map(this::mapToDTO);
    }

    public Optional<Portfolio> findPortfolioById(String portfolioId) {
        return portfolioRepository.findById(portfolioId);
    }

    public Optional<Portfolio> findPortfolioByOwnerId(String ownerId) {
        return portfolioRepository.findByOwnerId(ownerId);
    }

    public void deletePortfolio(String portfolioId) {
        portfolioRepository.deleteById(portfolioId);
    }

    // ===== ORDER PLACEMENT (SAGA INITIATION) - FIXED =====

    public PortfolioOperationResultDTO placeBuyOrder(PlaceBuyOrderCommand command) {
        logger.info("Received placeBuyOrder request for portfolio: {}, symbol: {}, quantity: {}",
                command.getPortfolioId(), command.getSymbolCode(), command.getQuantity());

        try {
            Portfolio portfolio = getPortfolioOrThrow(command.getPortfolioId());

            Symbol symbol = Symbol.createFromCode(command.getSymbolCode());
            Money price = Money.of(command.getPrice(), Currency.valueOf(command.getCurrency()));

            Portfolio.PlaceOrderCommand domainCommand = new Portfolio.PlaceOrderCommand(
                    symbol, price, command.getQuantity(), OrderType.BUY
            );

            // Execute command on aggregate
            portfolio.placeOrder(domainCommand);

            // Get events BEFORE saving (this is the fix!)
            List<DomainEvent> events = portfolio.getAndClearEvents();
            logger.info("Retrieved {} events from portfolio", events.size());

            // Save the portfolio state
            portfolioRepository.save(portfolio);
            logger.info("Portfolio saved successfully");

            // Publish events
            if (!events.isEmpty()) {
                for (DomainEvent event : events) {
                    logger.info("Publishing event: {} with correlation ID: {}",
                            event.getClass().getSimpleName(), event.getCorrelationId());
                }
                eventPublisher.publishEvents(events);

                return PortfolioOperationResultDTO.success(
                        command.getPortfolioId(),
                        "Buy order requested successfully"
                );
            } else {
                logger.error("No events generated! Saga will not start!");
                return PortfolioOperationResultDTO.error(
                        command.getPortfolioId(),
                        "Internal error: No saga events generated"
                );
            }

        } catch (Portfolio.InsufficientFundsException e) {
            logger.error("Insufficient funds for buy order: {}", e.getMessage());
            return PortfolioOperationResultDTO.error(
                    command.getPortfolioId(),
                    "Insufficient funds: " + e.getMessage()
            );
        } catch (Exception e) {
            logger.error("Failed to place buy order for portfolio: {}", command.getPortfolioId(), e);
            return PortfolioOperationResultDTO.error(
                    command.getPortfolioId(),
                    "Failed to place buy order: " + e.getMessage()
            );
        }
    }

    public PortfolioOperationResultDTO placeSellOrder(PlaceSellOrderCommand command) {
        logger.info("Received placeSellOrder request for portfolio: {}, symbol: {}, quantity: {}",
                command.getPortfolioId(), command.getSymbolCode(), command.getQuantity());

        try {
            Portfolio portfolio = getPortfolioOrThrow(command.getPortfolioId());

            Symbol symbol = Symbol.createFromCode(command.getSymbolCode());
            Money price = Money.of(command.getPrice(), Currency.valueOf(command.getCurrency()));

            Portfolio.PlaceOrderCommand domainCommand = new Portfolio.PlaceOrderCommand(
                    symbol, price, command.getQuantity(), OrderType.SELL
            );

            // Execute command on aggregate
            portfolio.placeOrder(domainCommand);

            // Get events BEFORE saving (this is the fix!)
            List<DomainEvent> events = portfolio.getAndClearEvents();
            logger.info("Retrieved {} events from portfolio", events.size());

            // Save the portfolio state
            portfolioRepository.save(portfolio);
            logger.info("Portfolio saved successfully");

            // Publish events
            if (!events.isEmpty()) {
                for (DomainEvent event : events) {
                    logger.info("Publishing event: {} with correlation ID: {}",
                            event.getClass().getSimpleName(), event.getCorrelationId());
                }
                eventPublisher.publishEvents(events);

                return PortfolioOperationResultDTO.success(
                        command.getPortfolioId(),
                        "Sell order requested successfully"
                );
            } else {
                logger.error("No events generated! Saga will not start!");
                return PortfolioOperationResultDTO.error(
                        command.getPortfolioId(),
                        "Internal error: No saga events generated"
                );
            }

        } catch (Portfolio.InsufficientAssetsException e) {
            logger.warn("Insufficient assets for sell order: {}", e.getMessage());
            return PortfolioOperationResultDTO.error(
                    command.getPortfolioId(),
                    "Insufficient assets: " + e.getMessage()
            );
        } catch (Exception e) {
            logger.error("Failed to place sell order", e);
            return PortfolioOperationResultDTO.error(
                    command.getPortfolioId(),
                    "Failed to place sell order: " + e.getMessage()
            );
        }
    }

    // ===== EVENT HANDLERS (SAGA PARTICIPANTS) =====

    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        handleEvent(event, () -> {
            logger.info("[SAGA: {}] OrderCreatedEvent received for portfolio: {}, order: {}",
                    event.getCorrelationId(), event.getPortfolioId(), event.getOrderId());

            Optional<Portfolio> portfolioOpt = portfolioRepository.findById(event.getPortfolioId());
            if (portfolioOpt.isPresent()) {
                Portfolio portfolio = portfolioOpt.get();
                portfolio.handleOrderCreated(event);
                portfolioRepository.save(portfolio);

                logger.info("[SAGA: {}] Order creation confirmed for reservation: {}",
                        event.getCorrelationId(), event.getReservationId());
            }
        });
    }

    @EventListener
    public void handleOrderCreationFailed(OrderCreationFailedEvent event) {
        handleEvent(event, () -> {
            logger.warn("[SAGA: {}] OrderCreationFailedEvent received for portfolio: {}, reason: {}",
                    event.getCorrelationId(), event.getPortfolioId(), event.getReason());

            Optional<Portfolio> portfolioOpt = portfolioRepository.findById(event.getPortfolioId());
            if (portfolioOpt.isPresent()) {
                Portfolio portfolio = portfolioOpt.get();
                portfolio.handleOrderCreationFailed(event);
                portfolioRepository.save(portfolio);

                logger.info("[SAGA: {}] Reservation released: {}",
                        event.getCorrelationId(), event.getReservationId());
            }
        });
    }

    @EventListener
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        handleEvent(event, () -> {
            logger.info("[SAGA: {}] TransactionCreatedEvent received - TX: {}, Buy: {}, Sell: {}",
                    event.getCorrelationId(), event.getTransactionId(),
                    event.getBuyOrderId(), event.getSellOrderId());

            // Handle buy side portfolio
            if (event.getBuyPortfolioId() != null) {
                Optional<Portfolio> buyPortfolio = portfolioRepository.findById(event.getBuyPortfolioId());
                if (buyPortfolio.isPresent()) {
                    Portfolio portfolio = buyPortfolio.get();
                    portfolio.handleTransactionCreated(event);
                    portfolioRepository.save(portfolio);

                    logger.info("[SAGA: {}] Buy side updated for portfolio: {}",
                            event.getCorrelationId(), event.getBuyPortfolioId());
                }
            }

            // Handle sell side portfolio
            if (event.getSellPortfolioId() != null) {
                Optional<Portfolio> sellPortfolio = portfolioRepository.findById(event.getSellPortfolioId());
                if (sellPortfolio.isPresent()) {
                    Portfolio portfolio = sellPortfolio.get();
                    portfolio.handleTransactionCreated(event);
                    portfolioRepository.save(portfolio);

                    logger.info("[SAGA: {}] Sell side updated for portfolio: {}",
                            event.getCorrelationId(), event.getSellPortfolioId());
                }
            }
        });
    }

    // ===== CASH OPERATIONS =====

    public PortfolioOperationResultDTO depositCash(DepositCashCommand command) {
        try {
            Portfolio portfolio = getPortfolioOrThrow(command.getPortfolioId());
            Money amount = Money.of(command.getAmount(), command.getCurrency());

            portfolio.depositCash(amount);
            portfolioRepository.save(portfolio);

            logger.info("Cash deposited: {} to portfolio: {}",
                    amount.toDisplayString(), command.getPortfolioId());

            return PortfolioOperationResultDTO.success(
                    command.getPortfolioId(),
                    "Cash deposited successfully"
            );

        } catch (Exception e) {
            logger.error("Failed to deposit cash", e);
            return PortfolioOperationResultDTO.error(
                    command.getPortfolioId(),
                    "Failed to deposit cash: " + e.getMessage()
            );
        }
    }

    public PortfolioOperationResultDTO withdrawCash(WithdrawCashCommand command) {
        try {
            Portfolio portfolio = getPortfolioOrThrow(command.getPortfolioId());
            Money amount = Money.of(command.getAmount(), command.getCurrency());

            portfolio.withdrawCash(amount);
            portfolioRepository.save(portfolio);

            logger.info("Cash withdrawn: {} from portfolio: {}",
                    amount.toDisplayString(), command.getPortfolioId());

            return PortfolioOperationResultDTO.success(
                    command.getPortfolioId(),
                    "Cash withdrawn successfully"
            );

        } catch (Exception e) {
            logger.error("Failed to withdraw cash", e);
            return PortfolioOperationResultDTO.error(
                    command.getPortfolioId(),
                    "Failed to withdraw cash: " + e.getMessage()
            );
        }
    }

    public Money getAvailableCash(String portfolioId, Currency currency) {
        Portfolio portfolio = getPortfolioOrThrow(portfolioId);
        return portfolio.getAvailableCash(currency);
    }

    public Money getTotalCash(String portfolioId, Currency currency) {
        Portfolio portfolio = getPortfolioOrThrow(portfolioId);
        return portfolio.getTotalCash(currency);
    }

    // ===== POSITION OPERATIONS =====

    public BigDecimal getAvailableAssets(String portfolioId, Symbol symbol) {
        Portfolio portfolio = getPortfolioOrThrow(portfolioId);
        return portfolio.getAvailableAssets(symbol);
    }

    public BigDecimal getTotalAssets(String portfolioId, Symbol symbol) {
        Portfolio portfolio = getPortfolioOrThrow(portfolioId);
        return portfolio.getTotalAssets(symbol);
    }

    public PortfolioSnapshot getPortfolioSnapshot(String portfolioId) {
        Portfolio portfolio = getPortfolioOrThrow(portfolioId);

        Map<Currency, Money> cashBalances = new HashMap<>();
        Map<Currency, Money> reservedCash = new HashMap<>();
        for (Currency currency : Currency.values()) {
            Money total = portfolio.getTotalCash(currency);
            if (total.isPositive()) {
                cashBalances.put(currency, total);
                Money reserved = portfolio.getReservedCash(currency);
                if (reserved.isPositive()) {
                    reservedCash.put(currency, reserved);
                }
            }
        }

        // For a complete implementation, you'd need to track all symbols
        Map<Symbol, BigDecimal> positions = new HashMap<>();
        Map<Symbol, BigDecimal> reservedAssets = new HashMap<>();

        Money totalValue = calculatePortfolioValue(cashBalances, positions);

        return new PortfolioSnapshot(
                portfolioId,
                portfolio.getOwnerId(),
                cashBalances,
                positions,
                reservedCash,
                reservedAssets,
                totalValue
        );
    }

    // ===== MAINTENANCE =====

    @Scheduled(fixedDelay = 60000) // Every minute
    public void cleanupExpiredReservations() {
        List<Portfolio> portfolios = portfolioRepository.findAll();
        int cleaned = 0;

        for (Portfolio portfolio : portfolios) {
            portfolio.cleanupExpiredReservations();
            portfolioRepository.save(portfolio);
            cleaned++;
        }

        if (cleaned > 0) {
            logger.debug("Cleaned up expired reservations for {} portfolios", cleaned);
        }
    }

    public int getActiveReservationsCount(String portfolioId) {
        Portfolio portfolio = getPortfolioOrThrow(portfolioId);
        return portfolio.getActiveReservationsCount();
    }

    // ===== PRIVATE HELPER METHODS =====

    private Portfolio getPortfolioOrThrow(String portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));
    }

    private Money calculatePortfolioValue(Map<Currency, Money> cashBalances,
                                          Map<Symbol, BigDecimal> positions) {
        Money totalValue = Money.zero(Currency.USD);

        // Add cash values
        for (Money cash : cashBalances.values()) {
            if (cash.getCurrency() == Currency.USD) {
                totalValue = totalValue.add(cash);
            }
            // For other currencies, would need exchange rates
        }

        // Add position values
        for (Map.Entry<Symbol, BigDecimal> entry : positions.entrySet()) {
            Optional<Money> priceOpt = marketDataAdapter.getCurrentPrice(entry.getKey());
            if (priceOpt.isPresent()) {
                Money positionValue = priceOpt.get().multiply(entry.getValue());
                if (positionValue.getCurrency() == Currency.USD) {
                    totalValue = totalValue.add(positionValue);
                }
            }
        }

        return totalValue;
    }

    private PortfolioDTO mapToDTO(Portfolio portfolio) {
        PortfolioSnapshot snapshot = getPortfolioSnapshot(portfolio.getPortfolioId());
        return new PortfolioDTO(
                portfolio.getPortfolioId(),
                portfolio.getOwnerId(),
                snapshot.getCashBalances(),
                snapshot.getPositions(),
                snapshot.getTotalValue(),
                LocalDateTime.now()
        );
    }
    public PortfolioOperationResultDTO depositAsset(DepositAssetCommand command) {
        try {
            Portfolio portfolio = getPortfolioOrThrow(command.getPortfolioId());

            portfolio.depositAsset(command.getSymbol(), command.getQuantity());
            portfolioRepository.save(portfolio);

            logger.info("📦 Assets deposited: {} {} to portfolio: {}",
                    command.getQuantity(), command.getSymbol().getCode(), command.getPortfolioId());

            return PortfolioOperationResultDTO.success(
                    command.getPortfolioId(),
                    String.format("Successfully deposited %s %s",
                            command.getQuantity(), command.getSymbol().getCode())
            );

        } catch (Exception e) {
            logger.error("Failed to deposit assets", e);
            return PortfolioOperationResultDTO.error(
                    command.getPortfolioId(),
                    "Failed to deposit assets: " + e.getMessage()
            );
        }
    }
}