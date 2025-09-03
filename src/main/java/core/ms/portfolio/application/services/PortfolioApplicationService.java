package core.ms.portfolio.application.services;

import core.ms.portfolio.application.dto.command.*;
import core.ms.portfolio.application.dto.query.PortfolioDTO;
import core.ms.portfolio.application.dto.query.PortfolioOperationResultDTO;
import core.ms.portfolio.domain.Portfolio;
import core.ms.portfolio.domain.cash.CashManager;
import core.ms.portfolio.domain.events.subscribe.OrderCreatedEvent;
import core.ms.portfolio.domain.events.subscribe.OrderCreationFailedEvent;
import core.ms.portfolio.domain.events.subscribe.TransactionCreatedEvent;
import core.ms.portfolio.domain.ports.inbound.PortfolioService;
import core.ms.portfolio.domain.ports.inbound.PortfolioSnapshot;
import core.ms.portfolio.domain.ports.outbound.*;
import core.ms.portfolio.domain.positions.PositionManager;
import core.ms.shared.OrderType;
import core.ms.shared.events.CorrelationAwareEventListener;
import core.ms.shared.events.DomainEvent;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
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
public class PortfolioApplicationService extends CorrelationAwareEventListener implements PortfolioService {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private PortfolioEventPublisher eventPublisher;

    @Autowired
    private MarketDataAdapter marketDataAdapter;

    // ===== PORTFOLIO MANAGEMENT =====

    @Override
    public Portfolio createPortfolio(String portfolioId, String ownerId) {
        // Check if portfolio already exists
        if (portfolioRepository.existsById(portfolioId)) {
            throw new IllegalArgumentException("Portfolio already exists: " + portfolioId);
        }

        // Create new portfolio with managers
        CashManager cashManager = new CashManager();
        PositionManager positionManager = new PositionManager();
        Portfolio portfolio = new Portfolio(portfolioId, ownerId, cashManager, positionManager);

        // Save and return
        return portfolioRepository.save(portfolio);
    }

    @Override
    public Optional<Portfolio> findPortfolioById(String portfolioId) {
        return portfolioRepository.findById(portfolioId);
    }

    @Override
    public Optional<Portfolio> findPortfolioByOwnerId(String ownerId) {
        return portfolioRepository.findByOwnerId(ownerId);
    }

    @Override
    public void deletePortfolio(String portfolioId) {
        portfolioRepository.deleteById(portfolioId);
    }

    // ===== ORDER PLACEMENT (SAGA INITIATION) =====

    /**
     * Places a buy order - initiates saga by creating reservation and emitting OrderRequestedEvent
     */
    public PortfolioOperationResultDTO placeBuyOrder(PlaceBuyOrderCommand command) {
        try {
            Portfolio portfolio = getPortfolioOrThrow(command.getPortfolioId());

            Symbol symbol = Symbol.createFromCode(command.getSymbolCode());
            Money price = Money.of(command.getPrice(), Currency.valueOf(command.getCurrency()));

            // Create domain command
            Portfolio.PlaceOrderCommand domainCommand = new Portfolio.PlaceOrderCommand(
                    symbol, price, command.getQuantity(), OrderType.BUY
            );

            // Start saga
            portfolio.placeOrder(domainCommand);

            // Save portfolio
            portfolioRepository.save(portfolio);

            // Publish events
            List<DomainEvent> events = portfolio.getAndClearEvents();
            eventPublisher.publishEvents(events);

            return PortfolioOperationResultDTO.success(
                    command.getPortfolioId(),
                    "Buy order requested successfully"
            );

        } catch (Exception e) {
            return PortfolioOperationResultDTO.error(
                    command.getPortfolioId(),
                    "Failed to place buy order: " + e.getMessage()
            );
        }
    }

    /**
     * Places a sell order - initiates saga by creating reservation and emitting OrderRequestedEvent
     */
    public PortfolioOperationResultDTO placeSellOrder(PlaceSellOrderCommand command) {
        try {
            Portfolio portfolio = getPortfolioOrThrow(command.getPortfolioId());

            Symbol symbol = Symbol.createFromCode(command.getSymbolCode());
            Money price = Money.of(command.getPrice(), Currency.valueOf(command.getCurrency()));

            // Create domain command
            Portfolio.PlaceOrderCommand domainCommand = new Portfolio.PlaceOrderCommand(
                    symbol, price, command.getQuantity(), OrderType.SELL
            );

            // Start saga
            portfolio.placeOrder(domainCommand);

            // Save portfolio
            portfolioRepository.save(portfolio);

            // Publish events
            List<DomainEvent> events = portfolio.getAndClearEvents();
            eventPublisher.publishEvents(events);

            return PortfolioOperationResultDTO.success(
                    command.getPortfolioId(),
                    "Sell order requested successfully"
            );

        } catch (Exception e) {
            return PortfolioOperationResultDTO.error(
                    command.getPortfolioId(),
                    "Failed to place sell order: " + e.getMessage()
            );
        }
    }

    // ===== EVENT HANDLERS (SAGA PARTICIPANTS) =====

    /**
     * Handle OrderCreatedEvent from Order BC
     */
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        handleEvent(event, () -> {
            Optional<Portfolio> portfolioOpt = portfolioRepository.findById(event.getPortfolioId());
            if (portfolioOpt.isPresent()) {
                Portfolio portfolio = portfolioOpt.get();
                portfolio.handleOrderCreated(event);
                portfolioRepository.save(portfolio);
            }
        });
    }

    /**
     * Handle OrderCreationFailedEvent from Order BC
     */
    @EventListener
    public void handleOrderCreationFailed(OrderCreationFailedEvent event) {
        handleEvent(event, () -> {
            Optional<Portfolio> portfolioOpt = portfolioRepository.findById(event.getPortfolioId());
            if (portfolioOpt.isPresent()) {
                Portfolio portfolio = portfolioOpt.get();
                portfolio.handleOrderCreationFailed(event);
                portfolioRepository.save(portfolio);
            }
        });
    }

    /**
     * Handle TransactionCreatedEvent from Order BC
     */
    @EventListener
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        handleEvent(event, () -> {
            // Handle buy side portfolio
            if (event.getBuyPortfolioId() != null) {
                Optional<Portfolio> buyPortfolio = portfolioRepository.findById(event.getBuyPortfolioId());
                if (buyPortfolio.isPresent()) {
                    Portfolio portfolio = buyPortfolio.get();
                    portfolio.handleTransactionCreated(event);
                    portfolioRepository.save(portfolio);
                }
            }

            // Handle sell side portfolio
            if (event.getSellPortfolioId() != null) {
                Optional<Portfolio> sellPortfolio = portfolioRepository.findById(event.getSellPortfolioId());
                if (sellPortfolio.isPresent()) {
                    Portfolio portfolio = sellPortfolio.get();
                    portfolio.handleTransactionCreated(event);
                    portfolioRepository.save(portfolio);
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

            return PortfolioOperationResultDTO.success(
                    command.getPortfolioId(),
                    "Cash deposited successfully"
            );

        } catch (Exception e) {
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

            return PortfolioOperationResultDTO.success(
                    command.getPortfolioId(),
                    "Cash withdrawn successfully"
            );

        } catch (Exception e) {
            return PortfolioOperationResultDTO.error(
                    command.getPortfolioId(),
                    "Failed to withdraw cash: " + e.getMessage()
            );
        }
    }

    @Override
    public Money getAvailableCash(String portfolioId, Currency currency) {
        Portfolio portfolio = getPortfolioOrThrow(portfolioId);
        return portfolio.getAvailableCash(currency);
    }

    @Override
    public Money getTotalCash(String portfolioId, Currency currency) {
        Portfolio portfolio = getPortfolioOrThrow(portfolioId);
        return portfolio.getTotalCash(currency);
    }

    // ===== POSITION OPERATIONS =====

    @Override
    public BigDecimal getAvailableAssets(String portfolioId, Symbol symbol) {
        Portfolio portfolio = getPortfolioOrThrow(portfolioId);
        return portfolio.getAvailableAssets(symbol);
    }

    @Override
    public BigDecimal getTotalAssets(String portfolioId, Symbol symbol) {
        Portfolio portfolio = getPortfolioOrThrow(portfolioId);
        return portfolio.getTotalAssets(symbol);
    }

    @Override
    public PortfolioSnapshot getPortfolioSnapshot(String portfolioId) {
        Portfolio portfolio = getPortfolioOrThrow(portfolioId);

        // Collect all cash balances
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

        // Collect all positions (simplified - would need to track all symbols)
        Map<Symbol, BigDecimal> positions = new HashMap<>();
        Map<Symbol, BigDecimal> reservedAssets = new HashMap<>();

        // Calculate total value
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

    @Override
    @Scheduled(fixedDelay = 60000) // Every minute
    public void cleanupExpiredReservations() {
        List<Portfolio> portfolios = portfolioRepository.findAll();
        for (Portfolio portfolio : portfolios) {
            portfolio.cleanupExpiredReservations();
            portfolioRepository.save(portfolio);
        }
    }

    @Override
    public int getActiveReservationsCount(String portfolioId) {
        // This would need to be enhanced to track reservation counts
        return 0;
    }

    // ===== DTO CONVERSION METHODS =====

    public PortfolioOperationResultDTO createPortfolio(CreatePortfolioCommand command) {
        try {
            Portfolio portfolio = createPortfolio(command.getPortfolioId(), command.getOwnerId());
            return PortfolioOperationResultDTO.success(
                    portfolio.getPortfolioId(),
                    "Portfolio created successfully"
            );
        } catch (Exception e) {
            return PortfolioOperationResultDTO.error(
                    command.getPortfolioId(),
                    "Failed to create portfolio: " + e.getMessage()
            );
        }
    }

    public Optional<PortfolioDTO> findPortfolioByIdAsDTO(String portfolioId) {
        Optional<Portfolio> portfolio = findPortfolioById(portfolioId);
        return portfolio.map(this::mapToDTO);
    }

    // ===== PRIVATE HELPER METHODS =====

    private Portfolio getPortfolioOrThrow(String portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));
    }

    private Money calculatePortfolioValue(Map<Currency, Money> cashBalances,
                                          Map<Symbol, BigDecimal> positions) {
        // Calculate total value in USD (simplified)
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

    // ===== LEGACY INTERFACE IMPLEMENTATIONS =====

    @Override
    @Deprecated
    public core.ms.portfolio.domain.ports.inbound.PortfolioOperationResult depositCash(String portfolioId, Money amount) {
        DepositCashCommand command = new DepositCashCommand(portfolioId, amount.getAmount(), amount.getCurrency());
        PortfolioOperationResultDTO result = depositCash(command);
        return mapToLegacyResult(result);
    }

    @Override
    @Deprecated
    public core.ms.portfolio.domain.ports.inbound.PortfolioOperationResult withdrawCash(String portfolioId, Money amount) {
        WithdrawCashCommand command = new WithdrawCashCommand(portfolioId, amount.getAmount(), amount.getCurrency());
        PortfolioOperationResultDTO result = withdrawCash(command);
        return mapToLegacyResult(result);
    }

    @Override
    @Deprecated
    public core.ms.portfolio.domain.ports.inbound.OrderReservationResult placeBuyOrder(
            String portfolioId, String orderId, Symbol symbol, Money price, BigDecimal quantity) {
        // This legacy method is no longer used with the saga pattern
        throw new UnsupportedOperationException("Use placeBuyOrder(PlaceBuyOrderCommand) instead");
    }

    @Override
    @Deprecated
    public core.ms.portfolio.domain.ports.inbound.OrderReservationResult placeSellOrder(
            String portfolioId, String orderId, Symbol symbol, Money price, BigDecimal quantity) {
        // This legacy method is no longer used with the saga pattern
        throw new UnsupportedOperationException("Use placeSellOrder(PlaceSellOrderCommand) instead");
    }

    private core.ms.portfolio.domain.ports.inbound.PortfolioOperationResult mapToLegacyResult(
            PortfolioOperationResultDTO dto) {
        return core.ms.portfolio.domain.ports.inbound.PortfolioOperationResult.builder()
                .success(dto.isSuccess())
                .portfolioId(dto.getPortfolioId())
                .message(dto.getMessage())
                .errors(dto.getErrors())
                .build();
    }
}