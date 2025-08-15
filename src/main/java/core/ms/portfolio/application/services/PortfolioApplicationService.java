package core.ms.portfolio.application.services;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.portfolio.application.dto.command.CreatePortfolioCommand;
import core.ms.portfolio.application.dto.command.DepositCashCommand;
import core.ms.portfolio.application.dto.command.WithdrawCashCommand;
import core.ms.portfolio.application.dto.query.PortfolioDTO;
import core.ms.portfolio.application.dto.query.PortfolioOperationResultDTO;
import core.ms.portfolio.domain.Portfolio;
import core.ms.portfolio.domain.cash.CashManager;
import core.ms.portfolio.domain.cash.CashReservation;
import core.ms.portfolio.domain.events.subscribe.OrderCancelledEvent;
import core.ms.portfolio.domain.events.subscribe.OrderExpiredEvent;
import core.ms.portfolio.domain.events.subscribe.OrderRejectedEvent;
import core.ms.portfolio.domain.events.subscribe.TransactionCreatedEvent;
import core.ms.portfolio.domain.ports.inbound.OrderReservationResult;
import core.ms.portfolio.domain.ports.inbound.PortfolioOperationResult;
import core.ms.portfolio.domain.ports.inbound.PortfolioService;
import core.ms.portfolio.domain.ports.inbound.PortfolioSnapshot;
import core.ms.portfolio.domain.ports.outbound.*;
import core.ms.portfolio.domain.positions.AssetReservation;
import core.ms.portfolio.domain.positions.PositionManager;
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
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class PortfolioApplicationService implements PortfolioService {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PortfolioEventPublisher eventPublisher;

    @Autowired
    private OrderServiceAdapter orderServiceAdapter;

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

    // ===== CASH OPERATIONS =====

    @Override
    public PortfolioOperationResult depositCash(String portfolioId, Money amount) {
        try {
            Portfolio portfolio = getPortfolioOrThrow(portfolioId);
            portfolio.depositCash(amount);
            portfolioRepository.save(portfolio);

            return PortfolioOperationResult.builder()
                    .success(true)
                    .portfolioId(portfolioId)
                    .message("Cash deposited successfully")
                    .build();

        } catch (Exception e) {
            return PortfolioOperationResult.builder()
                    .success(false)
                    .portfolioId(portfolioId)
                    .message("Failed to deposit cash: " + e.getMessage())
                    .errors(List.of(e.getMessage()))
                    .build();
        }
    }

    @Override
    public PortfolioOperationResult withdrawCash(String portfolioId, Money amount) {
        try {
            Portfolio portfolio = getPortfolioOrThrow(portfolioId);
            portfolio.withdrawCash(amount);
            portfolioRepository.save(portfolio);

            return PortfolioOperationResult.builder()
                    .success(true)
                    .portfolioId(portfolioId)
                    .message("Cash withdrawn successfully")
                    .build();

        } catch (Exception e) {
            return PortfolioOperationResult.builder()
                    .success(false)
                    .portfolioId(portfolioId)
                    .message("Failed to withdraw cash: " + e.getMessage())
                    .errors(List.of(e.getMessage()))
                    .build();
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

        // Collect all positions
        Map<Symbol, BigDecimal> positions = new HashMap<>();
        Map<Symbol, BigDecimal> reservedAssets = new HashMap<>();
        // Note: This would need to be enhanced to track all symbols in portfolio
        // For now, simplified implementation

        // Calculate total value (would need market prices)
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

    // ===== ORDER OPERATIONS =====

    @Override
    public OrderReservationResult placeBuyOrder(String portfolioId, String orderId,
                                                Symbol symbol, Money price, BigDecimal quantity) {
        try {
            Portfolio portfolio = getPortfolioOrThrow(portfolioId);

            // Get order from Order BC
            Optional<IBuyOrder> orderOpt = orderServiceAdapter.findBuyOrderById(orderId);
            if (orderOpt.isEmpty()) {
                return OrderReservationResult.failed("Buy order not found: " + orderId);
            }

            IBuyOrder order = orderOpt.get();
            CashReservation reservation = portfolio.placeBuyOrder(order);

            // Save reservation
            reservationRepository.saveCashReservation(reservation);

            // Save portfolio
            portfolioRepository.save(portfolio);

            // Publish events
            List<DomainEvent> events = portfolio.getAndClearEvents();
            eventPublisher.publishEvents(events);

            return OrderReservationResult.successfulCashReservation(
                    reservation.getReservationId(), orderId);

        } catch (Exception e) {
            return OrderReservationResult.failed("Failed to place buy order: " + e.getMessage());
        }
    }

    @Override
    public OrderReservationResult placeSellOrder(String portfolioId, String orderId,
                                                 Symbol symbol, Money price, BigDecimal quantity) {
        try {
            Portfolio portfolio = getPortfolioOrThrow(portfolioId);

            // Get order from Order BC
            Optional<ISellOrder> orderOpt = orderServiceAdapter.findSellOrderById(orderId);
            if (orderOpt.isEmpty()) {
                return OrderReservationResult.failed("Sell order not found: " + orderId);
            }

            ISellOrder order = orderOpt.get();
            AssetReservation reservation = portfolio.placeSellOrder(order);

            // Save reservation
            reservationRepository.saveAssetReservation(reservation);

            // Save portfolio
            portfolioRepository.save(portfolio);

            // Publish events
            List<DomainEvent> events = portfolio.getAndClearEvents();
            eventPublisher.publishEvents(events);

            return OrderReservationResult.successfulAssetReservation(
                    reservation.getReservationId(), orderId);

        } catch (Exception e) {
            return OrderReservationResult.failed("Failed to place sell order: " + e.getMessage());
        }
    }

    // ===== EVENT HANDLERS =====

    @EventListener
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        Optional<Portfolio> portfolioOpt = portfolioRepository.findById(event.getPortfolioId());
        if (portfolioOpt.isPresent()) {
            Portfolio portfolio = portfolioOpt.get();
            portfolio.handleTransactionCreated(event);
            portfolioRepository.save(portfolio);
        }
    }

    @EventListener
    public void handleOrderCancelled(OrderCancelledEvent event) {
        Optional<Portfolio> portfolioOpt = portfolioRepository.findById(event.getPortfolioId());
        if (portfolioOpt.isPresent()) {
            Portfolio portfolio = portfolioOpt.get();
            portfolio.handleOrderCancelled(event);
            portfolioRepository.save(portfolio);
        }
    }

    @EventListener
    public void handleOrderExpired(OrderExpiredEvent event) {
        Optional<Portfolio> portfolioOpt = portfolioRepository.findById(event.getPortfolioId());
        if (portfolioOpt.isPresent()) {
            Portfolio portfolio = portfolioOpt.get();
            portfolio.handleOrderExpired(event);
            portfolioRepository.save(portfolio);
        }
    }

    @EventListener
    public void handleOrderRejected(OrderRejectedEvent event) {
        Optional<Portfolio> portfolioOpt = portfolioRepository.findById(event.getPortfolioId());
        if (portfolioOpt.isPresent()) {
            Portfolio portfolio = portfolioOpt.get();
            portfolio.handleOrderRejected(event);
            portfolioRepository.save(portfolio);
        }
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

        // Also cleanup from repository
        Instant cutoff = Instant.now().minusSeconds(300); // 5 minutes ago
        int deleted = reservationRepository.deleteExpiredReservationsBefore(cutoff);
        if (deleted > 0) {
            System.out.println("Cleaned up " + deleted + " expired reservations");
        }
    }

    @Override
    public int getActiveReservationsCount(String portfolioId) {
        List<CashReservation> cashReservations =
                reservationRepository.findCashReservationsByPortfolioId(portfolioId);
        List<AssetReservation> assetReservations =
                reservationRepository.findAssetReservationsByPortfolioId(portfolioId);
        return cashReservations.size() + assetReservations.size();
    }

    // ===== DTO CONVERSION METHODS =====

    public PortfolioOperationResultDTO createPortfolio(CreatePortfolioCommand command) {
        try {
            Portfolio portfolio = createPortfolio(command.getPortfolioId(), command.getOwnerId());
            return PortfolioOperationResultDTO.success(portfolio.getPortfolioId(),
                    "Portfolio created successfully");
        } catch (Exception e) {
            return PortfolioOperationResultDTO.error(command.getPortfolioId(),
                    "Failed to create portfolio: " + e.getMessage());
        }
    }

    public PortfolioOperationResultDTO depositCash(DepositCashCommand command) {
        Money amount = Money.of(command.getAmount(), command.getCurrency());
        PortfolioOperationResult result = depositCash(command.getPortfolioId(), amount);
        return mapToDTO(result);
    }

    public PortfolioOperationResultDTO withdrawCash(WithdrawCashCommand command) {
        Money amount = Money.of(command.getAmount(), command.getCurrency());
        PortfolioOperationResult result = withdrawCash(command.getPortfolioId(), amount);
        return mapToDTO(result);
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

        // Add cash values (would need exchange rates for non-USD)
        for (Money cash : cashBalances.values()) {
            if (cash.getCurrency() == Currency.USD) {
                totalValue = totalValue.add(cash);
            }
            // For other currencies, would need to convert
        }

        // Add position values (would need market prices)
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

    private PortfolioOperationResultDTO mapToDTO(PortfolioOperationResult result) {
        if (result.isSuccess()) {
            return PortfolioOperationResultDTO.success(result.getPortfolioId(), result.getMessage());
        } else {
            return PortfolioOperationResultDTO.error(result.getPortfolioId(), result.getMessage());
        }
    }

    private PortfolioDTO mapToDTO(Portfolio portfolio) {
        PortfolioSnapshot snapshot = getPortfolioSnapshot(portfolio.getPortfolioId());
        return new PortfolioDTO(
                portfolio.getPortfolioId(),
                portfolio.getOwnerId(),
                snapshot.getCashBalances(),
                snapshot.getPositions(),
                snapshot.getTotalValue(),
                snapshot.getTimestamp()
        );
    }
}