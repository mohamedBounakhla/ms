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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class PortfolioApplicationService extends CorrelationAwareEventListener {

    private static final Logger logger = LoggerFactory.getLogger(PortfolioApplicationService.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long LOCK_TIMEOUT_SECONDS = 5;

    // Portfolio-level locks for critical operations
    private final Map<String, ReentrantLock> portfolioLocks = new ConcurrentHashMap<>();

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private PortfolioEventPublisher eventPublisher;

    @Autowired
    private MarketDataAdapter marketDataAdapter;

    // ===== PORTFOLIO MANAGEMENT =====

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PortfolioOperationResultDTO createPortfolio(CreatePortfolioCommand command) {
        try {
            // Check existence with shared lock
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

    // ===== ORDER PLACEMENT WITH PESSIMISTIC LOCKING =====

    @Transactional(isolation = Isolation.SERIALIZABLE, timeout = 10)
    @Retryable(
            value = {OptimisticLockingFailureException.class},
            maxAttempts = MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public PortfolioOperationResultDTO placeBuyOrder(PlaceBuyOrderCommand command) {
        String portfolioId = command.getPortfolioId();
        Lock lock = getPortfolioLock(portfolioId);

        try {
            // Try to acquire lock with timeout
            if (!lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                logger.warn("Failed to acquire lock for portfolio: {} within timeout", portfolioId);
                return PortfolioOperationResultDTO.error(
                        portfolioId,
                        "Portfolio is currently busy, please try again"
                );
            }

            try {
                logger.info("Acquired lock for portfolio: {}, processing buy order", portfolioId);

                // Load portfolio with pessimistic write lock
                Portfolio portfolio = portfolioRepository.findByIdWithLock(portfolioId, LockModeType.PESSIMISTIC_WRITE)
                        .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));

                Symbol symbol = Symbol.createFromCode(command.getSymbolCode());
                Money price = Money.of(command.getPrice(), Currency.valueOf(command.getCurrency()));

                Portfolio.PlaceOrderCommand domainCommand = new Portfolio.PlaceOrderCommand(
                        symbol, price, command.getQuantity(), OrderType.BUY
                );

                // Execute command on aggregate
                portfolio.placeOrder(domainCommand);

                // Get events before saving
                List<DomainEvent> events = portfolio.getAndClearEvents();

                // Save with immediate flush to detect conflicts
                portfolioRepository.saveAndFlush(portfolio);

                // Publish events after successful save
                if (!events.isEmpty()) {
                    eventPublisher.publishEvents(events);
                    logger.info("Buy order placed successfully for portfolio: {}", portfolioId);

                    return PortfolioOperationResultDTO.success(
                            portfolioId,
                            "Buy order requested successfully"
                    );
                }

                return PortfolioOperationResultDTO.error(
                        portfolioId,
                        "Failed to generate order events"
                );

            } finally {
                lock.unlock();
                logger.info("Released lock for portfolio: {}", portfolioId);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while waiting for lock on portfolio: {}", portfolioId);
            return PortfolioOperationResultDTO.error(
                    portfolioId,
                    "Operation interrupted"
            );
        } catch (Portfolio.InsufficientFundsException e) {
            logger.warn("Insufficient funds for portfolio: {}", portfolioId);
            return PortfolioOperationResultDTO.error(
                    portfolioId,
                    "Insufficient funds: " + e.getMessage()
            );
        } catch (Exception e) {
            logger.error("Failed to place buy order for portfolio: {}", portfolioId, e);
            return PortfolioOperationResultDTO.error(
                    portfolioId,
                    "Failed to place buy order: " + e.getMessage()
            );
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE, timeout = 10)
    @Retryable(
            value = {OptimisticLockingFailureException.class},
            maxAttempts = MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public PortfolioOperationResultDTO placeSellOrder(PlaceSellOrderCommand command) {
        String portfolioId = command.getPortfolioId();
        Lock lock = getPortfolioLock(portfolioId);

        try {
            if (!lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                logger.warn("Failed to acquire lock for portfolio: {} within timeout", portfolioId);
                return PortfolioOperationResultDTO.error(
                        portfolioId,
                        "Portfolio is currently busy, please try again"
                );
            }

            try {
                logger.info("Acquired lock for portfolio: {}, processing sell order", portfolioId);

                // Load portfolio with pessimistic write lock
                Portfolio portfolio = portfolioRepository.findByIdWithLock(portfolioId, LockModeType.PESSIMISTIC_WRITE)
                        .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));

                Symbol symbol = Symbol.createFromCode(command.getSymbolCode());
                Money price = Money.of(command.getPrice(), Currency.valueOf(command.getCurrency()));

                Portfolio.PlaceOrderCommand domainCommand = new Portfolio.PlaceOrderCommand(
                        symbol, price, command.getQuantity(), OrderType.SELL
                );

                portfolio.placeOrder(domainCommand);

                List<DomainEvent> events = portfolio.getAndClearEvents();
                portfolioRepository.saveAndFlush(portfolio);

                if (!events.isEmpty()) {
                    eventPublisher.publishEvents(events);
                    logger.info("Sell order placed successfully for portfolio: {}", portfolioId);

                    return PortfolioOperationResultDTO.success(
                            portfolioId,
                            "Sell order requested successfully"
                    );
                }

                return PortfolioOperationResultDTO.error(
                        portfolioId,
                        "Failed to generate order events"
                );

            } finally {
                lock.unlock();
                logger.info("Released lock for portfolio: {}", portfolioId);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while waiting for lock on portfolio: {}", portfolioId);
            return PortfolioOperationResultDTO.error(
                    portfolioId,
                    "Operation interrupted"
            );
        } catch (Portfolio.InsufficientAssetsException e) {
            logger.warn("Insufficient assets for portfolio: {}", portfolioId);
            return PortfolioOperationResultDTO.error(
                    portfolioId,
                    "Insufficient assets: " + e.getMessage()
            );
        } catch (Exception e) {
            logger.error("Failed to place sell order for portfolio: {}", portfolioId, e);
            return PortfolioOperationResultDTO.error(
                    portfolioId,
                    "Failed to place sell order: " + e.getMessage()
            );
        }
    }

    // ===== EVENT HANDLERS WITH SEPARATE TRANSACTIONS =====

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200))
    public void handleOrderCreated(OrderCreatedEvent event) {
        handleEvent(event, () -> {
            String portfolioId = event.getPortfolioId();
            logger.info("[SAGA: {}] Processing OrderCreatedEvent for portfolio: {}",
                    event.getCorrelationId(), portfolioId);

            Portfolio portfolio = portfolioRepository.findByIdWithLock(portfolioId, LockModeType.PESSIMISTIC_READ)
                    .orElseThrow(() -> new IllegalStateException("Portfolio not found: " + portfolioId));

            portfolio.handleOrderCreated(event);
            portfolioRepository.saveAndFlush(portfolio);

            logger.info("[SAGA: {}] Order creation confirmed for reservation: {}",
                    event.getCorrelationId(), event.getReservationId());
        });
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200))
    public void handleOrderCreationFailed(OrderCreationFailedEvent event) {
        handleEvent(event, () -> {
            String portfolioId = event.getPortfolioId();
            logger.warn("[SAGA: {}] Processing OrderCreationFailedEvent for portfolio: {}",
                    event.getCorrelationId(), portfolioId);

            Portfolio portfolio = portfolioRepository.findByIdWithLock(portfolioId, LockModeType.PESSIMISTIC_READ)
                    .orElseThrow(() -> new IllegalStateException("Portfolio not found: " + portfolioId));

            portfolio.handleOrderCreationFailed(event);
            portfolioRepository.saveAndFlush(portfolio);

            logger.info("[SAGA: {}] Reservation released: {}",
                    event.getCorrelationId(), event.getReservationId());
        });
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200))
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        handleEvent(event, () -> {
            logger.info("[SAGA: {}] Processing TransactionCreatedEvent - TX: {}",
                    event.getCorrelationId(), event.getTransactionId());

            // Handle buy side with lock
            if (event.getBuyPortfolioId() != null) {
                Portfolio buyPortfolio = portfolioRepository.findByIdWithLock(
                        event.getBuyPortfolioId(),
                        LockModeType.PESSIMISTIC_WRITE
                ).orElseThrow(() -> new IllegalStateException(
                        "Buy portfolio not found: " + event.getBuyPortfolioId()
                ));

                buyPortfolio.handleTransactionCreated(event);
                portfolioRepository.saveAndFlush(buyPortfolio);

                logger.info("[SAGA: {}] Buy side updated for portfolio: {}",
                        event.getCorrelationId(), event.getBuyPortfolioId());
            }

            // Handle sell side with lock
            if (event.getSellPortfolioId() != null) {
                Portfolio sellPortfolio = portfolioRepository.findByIdWithLock(
                        event.getSellPortfolioId(),
                        LockModeType.PESSIMISTIC_WRITE
                ).orElseThrow(() -> new IllegalStateException(
                        "Sell portfolio not found: " + event.getSellPortfolioId()
                ));

                sellPortfolio.handleTransactionCreated(event);
                portfolioRepository.saveAndFlush(sellPortfolio);

                logger.info("[SAGA: {}] Sell side updated for portfolio: {}",
                        event.getCorrelationId(), event.getSellPortfolioId());
            }
        });
    }

    // ===== CASH OPERATIONS WITH OPTIMISTIC LOCKING =====

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(
            value = {OptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50)
    )
    public PortfolioOperationResultDTO depositCash(DepositCashCommand command) {
        try {
            Portfolio portfolio = portfolioRepository.findById(command.getPortfolioId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Portfolio not found: " + command.getPortfolioId()
                    ));

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

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(
            value = {OptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50)
    )
    public PortfolioOperationResultDTO depositAsset(DepositAssetCommand command) {
        try {
            Portfolio portfolio = portfolioRepository.findById(command.getPortfolioId())
                    .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));

            // Use the depositAsset method that exists in Portfolio
            portfolio.depositAsset(command.getSymbol(), command.getQuantity());
            portfolioRepository.save(portfolio);

            return PortfolioOperationResultDTO.success(
                    command.getPortfolioId(),
                    "Asset deposited successfully"
            );
        } catch (Exception e) {
            logger.error("Failed to deposit asset", e);
            return PortfolioOperationResultDTO.error(
                    command.getPortfolioId(),
                    "Failed to deposit asset: " + e.getMessage()
            );
        }
    }
    @Transactional(readOnly = true)
    public Optional<Portfolio> findPortfolioById(String portfolioId) {
        return portfolioRepository.findById(portfolioId);
    }
    // ===== QUERY METHODS (READ-ONLY) =====

    @Transactional(readOnly = true)
    public Optional<PortfolioDTO> findPortfolioByIdAsDTO(String portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Money getAvailableCash(String portfolioId, Currency currency) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));
        return portfolio.getAvailableCash(currency);
    }

    @Transactional(readOnly = true)
    public Money getTotalCash(String portfolioId, Currency currency) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));
        return portfolio.getTotalCash(currency);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAvailableAssets(String portfolioId, Symbol symbol) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));
        return portfolio.getAvailableAssets(symbol);
    }

    @Transactional(readOnly = true)
    public PortfolioSnapshot getPortfolioSnapshot(String portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));

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

    @Scheduled(fixedDelay = 60000)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupExpiredReservations() {
        List<Portfolio> portfolios = portfolioRepository.findAll();
        int cleaned = 0;

        for (Portfolio portfolio : portfolios) {
            try {
                portfolio.cleanupExpiredReservations();
                portfolioRepository.save(portfolio);
                cleaned++;
            } catch (Exception e) {
                logger.error("Failed to cleanup reservations for portfolio: {}",
                        portfolio.getPortfolioId(), e);
            }
        }

        if (cleaned > 0) {
            logger.debug("Cleaned up expired reservations for {} portfolios", cleaned);
        }
    }

    // ===== HELPER METHODS =====

    private Lock getPortfolioLock(String portfolioId) {
        return portfolioLocks.computeIfAbsent(portfolioId, k -> new ReentrantLock(true));
    }

    private Money calculatePortfolioValue(Map<Currency, Money> cashBalances,
                                          Map<Symbol, BigDecimal> positions) {
        Money totalValue = Money.zero(Currency.USD);

        for (Money cash : cashBalances.values()) {
            if (cash.getCurrency() == Currency.USD) {
                totalValue = totalValue.add(cash);
            }
        }

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
    @Transactional
    public void deletePortfolio(String portfolioId) {
        // Check if portfolio exists
        if (!portfolioRepository.existsById(portfolioId)) {
            throw new IllegalArgumentException("Portfolio not found: " + portfolioId);
        }

        // Check for active reservations
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));

        if (portfolio.getActiveReservationsCount() > 0) {  // Use the method that actually exists
            throw new IllegalStateException("Cannot delete portfolio with active reservations");
        }

        // Delete portfolio
        portfolioRepository.deleteById(portfolioId);
        logger.info("Portfolio deleted: {}", portfolioId);
    }
    @Transactional
    public PortfolioOperationResultDTO withdrawCash(WithdrawCashCommand command) {
        try {
            Portfolio portfolio = portfolioRepository.findById(command.getPortfolioId())
                    .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));

            Money amount = Money.of(command.getAmount(), command.getCurrency());

            // Check available balance
            Money available = portfolio.getAvailableCash(command.getCurrency());
            if (available.isLessThan(amount)) {
                return PortfolioOperationResultDTO.error(
                        command.getPortfolioId(),
                        "Insufficient funds. Available: " + available.toDisplayString()
                );
            }

            // Withdraw cash
            portfolio.withdrawCash(amount);
            portfolioRepository.save(portfolio);

            return PortfolioOperationResultDTO.success(
                    command.getPortfolioId(),
                    "Cash withdrawn successfully: " + amount.toDisplayString()
            );

        } catch (Exception e) {
            logger.error("Failed to withdraw cash", e);
            return PortfolioOperationResultDTO.error(
                    command.getPortfolioId(),
                    "Failed to withdraw: " + e.getMessage()
            );
        }
    }
    @Transactional(readOnly = true)
    public int getActiveReservationsCount(String portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));

        return portfolio.getActiveReservationsCount();
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
}