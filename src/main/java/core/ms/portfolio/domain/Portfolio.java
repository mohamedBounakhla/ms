package core.ms.portfolio.domain;

import core.ms.portfolio.domain.cash.CashManager;
import core.ms.portfolio.domain.events.publish.OrderRequestedEvent;
import core.ms.portfolio.domain.events.subscribe.OrderCreatedEvent;
import core.ms.portfolio.domain.events.subscribe.OrderCreationFailedEvent;
import core.ms.portfolio.domain.events.subscribe.TransactionCreatedEvent;
import core.ms.portfolio.domain.positions.PositionManager;
import core.ms.portfolio.infrastructure.persistence.entities.ReservationEntity;
import core.ms.portfolio.infrastructure.persistence.entities.ReservationEntity.ReservationStatus;
import core.ms.shared.OrderType;
import core.ms.shared.events.DomainEvent;
import core.ms.shared.events.EventContext;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import core.ms.utils.idgenerator.IdGen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class Portfolio {
    private static final Logger logger = LoggerFactory.getLogger(Portfolio.class);
    private static final String SOURCE_BC = "PORTFOLIO_BC";

    private final String portfolioId;
    private final String ownerId;
    private final CashManager cashManager;
    private final PositionManager positionManager;
    private final List<DomainEvent> domainEvents;
    private final Map<String, ReservationEntity> reservations;

    public Portfolio(String portfolioId, String ownerId,
                     CashManager cashManager, PositionManager positionManager) {
        this.portfolioId = portfolioId;
        this.ownerId = ownerId;
        this.cashManager = cashManager;
        this.positionManager = positionManager;
        this.domainEvents = new ArrayList<>();
        this.reservations = new HashMap<>();
    }

    public Portfolio(String portfolioId, String ownerId,
                     CashManager cashManager, PositionManager positionManager,
                     Map<String, ReservationEntity> persistedReservations) {
        this(portfolioId, ownerId, cashManager, positionManager);
        // Load persisted reservations as a map
        this.reservations.putAll(persistedReservations);
    }

    // Order Placement
    public ReservationEntity placeOrder(PlaceOrderCommand command) {
        logger.info("Starting placeOrder for symbol: {}, type: {}, quantity: {}",
                command.getSymbol().getCode(), command.getOrderType(), command.getQuantity());

        EventContext.startNewSaga();
        String correlationId = EventContext.getCurrentCorrelationId();
        String reservationId = IdGen.generate("reservation");

        logger.info("Generated correlation ID: {}, reservation ID: {}", correlationId, reservationId);

        ReservationEntity reservation = null;

        try {
            if (command.getOrderType() == OrderType.BUY) {
                reservation = handleBuyOrderReservation(command, reservationId, correlationId);
            } else {
                reservation = handleSellOrderReservation(command, reservationId, correlationId);
            }

            OrderRequestedEvent orderEvent = new OrderRequestedEvent(
                    correlationId,
                    SOURCE_BC,
                    reservationId,
                    portfolioId,
                    command.getSymbol(),
                    command.getPrice(),
                    command.getQuantity(),
                    command.getOrderType()
            );

            domainEvents.add(orderEvent);
            logger.info("Successfully created OrderRequestedEvent for correlation: {}", correlationId);

            return reservation;

        } catch (Exception e) {
            logger.error("Exception in placeOrder: {}", e.getMessage(), e);
            if (reservation != null) {
                reservation.fail();
                releaseReservation(reservationId);
            }
            throw e;
        }
    }

    private ReservationEntity handleBuyOrderReservation(PlaceOrderCommand command,
                                                        String reservationId,
                                                        String correlationId) {
        Money totalValue = command.getPrice().multiply(command.getQuantity());
        Money availableCash = cashManager.getAvailable(totalValue.getCurrency());

        logger.info("Buy order - Total value: {}, Available cash: {}",
                totalValue.toDisplayString(), availableCash.toDisplayString());

        if (availableCash.isLessThan(totalValue)) {
            throw new InsufficientFundsException(
                    String.format("Insufficient funds. Required: %s, Available: %s",
                            totalValue.toDisplayString(), availableCash.toDisplayString())
            );
        }

        cashManager.createInternalReservation(reservationId, totalValue);

        ReservationEntity reservation = new ReservationEntity(
                reservationId,
                null, // Portfolio will be set by repository
                OrderType.BUY,
                command.getSymbol().getCode(),
                totalValue.getCurrency(),
                totalValue.getAmount(),
                command.getQuantity(),
                correlationId
        );

        reservations.put(reservationId, reservation);
        return reservation;
    }

    private ReservationEntity handleSellOrderReservation(PlaceOrderCommand command,
                                                         String reservationId,
                                                         String correlationId) {
        BigDecimal availableQuantity = positionManager.getAvailable(command.getSymbol());

        logger.info("Sell order - Required: {}, Available: {} {}",
                command.getQuantity(), availableQuantity, command.getSymbol().getCode());

        if (availableQuantity.compareTo(command.getQuantity()) < 0) {
            throw new InsufficientAssetsException(
                    String.format("Insufficient assets. Required: %s, Available: %s",
                            command.getQuantity(), availableQuantity)
            );
        }

        positionManager.createInternalReservation(reservationId, command.getSymbol(), command.getQuantity());

        ReservationEntity reservation = new ReservationEntity(
                reservationId,
                null, // Portfolio will be set by repository
                OrderType.SELL,
                command.getSymbol().getCode(),
                null,
                null,
                command.getQuantity(),
                correlationId
        );

        reservations.put(reservationId, reservation);
        return reservation;
    }

    // Event Handlers
    public void handleOrderCreated(OrderCreatedEvent event) {
        if (!portfolioId.equals(event.getPortfolioId())) {
            return;
        }

        ReservationEntity reservation = reservations.get(event.getReservationId());
        if (reservation != null) {
            reservation.confirm(event.getOrderId());
            logger.info("Order creation confirmed for reservation: {}", event.getReservationId());
        }
    }

    public void handleOrderCreationFailed(OrderCreationFailedEvent event) {
        if (!portfolioId.equals(event.getPortfolioId())) {
            return;
        }

        logger.warn("Order creation failed for reservation: {}, releasing", event.getReservationId());
        ReservationEntity reservation = reservations.get(event.getReservationId());
        if (reservation != null) {
            reservation.fail();
        }
        releaseReservation(event.getReservationId());
    }

    public void handleTransactionCreated(TransactionCreatedEvent event) {
        if (portfolioId.equals(event.getBuyPortfolioId())) {
            logger.info("EXECUTING BUY TRANSACTION for portfolio: {}", portfolioId);
            logger.info("DEBOUG Portfolio.handleTransactionCreated - Buy: {}, Sell: {}",
                    event.getBuyPortfolioId(), event.getSellPortfolioId());
            logger.info("DEBOUG Event details - Symbol: {}, Quantity: {}, Price: {}",
                    event.getSymbol().getCode(),
                    event.getExecutedQuantity(),
                    event.getExecutedPrice().toDisplayString());

            executeBuyTransaction(
                    event.getBuyReservationId(),
                    event.getSymbol(),
                    event.getExecutedQuantity(),
                    event.getExecutedPrice()
            );
        }

        if (portfolioId.equals(event.getSellPortfolioId())) {
            logger.info("EXECUTING SELL TRANSACTION for portfolio: {}", portfolioId);
            logger.info("DEBOUG Calling executeBuyTransaction with quantity: {}",
                    event.getExecutedQuantity());
            logger.info("Sell reservation ID: {}", event.getSellReservationId());
            logger.info("Symbol: {}", event.getSymbol().getCode());
            logger.info("Proceeds should be: {}", event.getTotalValue().toDisplayString());

            executeSellTransaction(
                    event.getSellReservationId(),
                    event.getSymbol(),
                    event.getTotalValue()
            );
        }
    }

    private void executeBuyTransaction(String reservationId, Symbol symbol,
                                       BigDecimal quantity, Money price) {
        logger.info("DEBUG executeBuyTransaction - Symbol: {}, Quantity: {}, Price: {}",
                symbol.getCode(), quantity, price.toDisplayString());

        ReservationEntity reservation = reservations.get(reservationId);

        if (reservation == null) {
            logger.error("Reservation not found for buy execution: {}", reservationId);
            return;
        }

        if (reservation.getStatus() == ReservationStatus.EXECUTED) {
            logger.info("Buy reservation already executed: {}", reservationId);
            return;
        }

        cashManager.executeReservation(reservationId, reservation.getCurrency());
        positionManager.addAssets(symbol, quantity, price);

        reservation.execute();
        logger.info("Buy transaction executed for reservation: {}", reservationId);
    }

    private void executeSellTransaction(String reservationId, Symbol symbol, Money proceeds) {
        logger.info("STEP SELL-1: Starting executeSellTransaction");
        logger.info("  Reservation ID: {}", reservationId);
        logger.info("  Proceeds to add: {}", proceeds.toDisplayString());

        ReservationEntity reservation = reservations.get(reservationId);

        if (reservation == null) {
            logger.error("STEP SELL-2: ERROR - Reservation not found: {}", reservationId);
            return;
        }
        logger.info("STEP SELL-2: Reservation found");

        if (reservation.getStatus() == ReservationStatus.EXECUTED) {
            logger.info("STEP SELL-3: Reservation already executed, skipping");
            return;
        }
        logger.info("STEP SELL-3: Reservation not yet executed, proceeding");

        // Log cash BEFORE deposit
        Money cashBefore = cashManager.getTotal(proceeds.getCurrency());
        logger.info("STEP SELL-4: Cash BEFORE deposit: {}", cashBefore.toDisplayString());

        positionManager.executeReservation(reservationId, symbol);
        logger.info("STEP SELL-5: Position reservation executed");

        cashManager.deposit(proceeds);

        // Log cash AFTER deposit
        Money cashAfter = cashManager.getTotal(proceeds.getCurrency());
        logger.info("STEP SELL-6: Cash AFTER deposit: {}", cashAfter.toDisplayString());

        reservation.execute();
        logger.info("STEP SELL-7: Reservation marked as executed");
    }

    private void releaseReservation(String reservationId) {
        ReservationEntity reservation = reservations.get(reservationId);
        if (reservation == null) {
            return;
        }

        if (reservation.getOrderType() == OrderType.BUY && reservation.getCurrency() != null) {
            cashManager.releaseReservation(reservationId, reservation.getCurrency());
        } else if (reservation.getOrderType() == OrderType.SELL && reservation.getSymbolCode() != null) {
            Symbol symbol = Symbol.createFromCode(reservation.getSymbolCode());
            positionManager.releaseReservation(reservationId, symbol);
        }

        reservation.release();
        logger.info("Reservation released: {}", reservationId);
    }

    public void cleanupExpiredReservations() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        List<String> toRelease = new ArrayList<>();

        for (Map.Entry<String, ReservationEntity> entry : reservations.entrySet()) {
            if (entry.getValue().getCreatedAt().isBefore(cutoff)
                    && entry.getValue().isActive()) {
                toRelease.add(entry.getKey());
            }
        }

        toRelease.forEach(this::releaseReservation);
        cashManager.cleanupExpired();
        positionManager.cleanupExpired();
    }

    // Cash Operations
    public void depositCash(Money amount) {
        cashManager.deposit(amount);
    }

    public void withdrawCash(Money amount) {
        cashManager.withdraw(amount);
    }

    public void depositAsset(Symbol symbol, BigDecimal quantity) {
        positionManager.deposit(symbol, quantity);
    }

    // Query Methods
    public Money getAvailableCash(Currency currency) {
        return cashManager.getAvailable(currency);
    }

    public Money getTotalCash(Currency currency) {
        return cashManager.getTotal(currency);
    }

    public Money getReservedCash(Currency currency) {
        return cashManager.getReserved(currency);
    }

    public BigDecimal getAvailableAssets(Symbol symbol) {
        return positionManager.getAvailable(symbol);
    }

    public BigDecimal getTotalAssets(Symbol symbol) {
        return positionManager.getTotal(symbol);
    }

    public BigDecimal getReservedAssets(Symbol symbol) {
        return positionManager.getReserved(symbol);
    }

    public List<DomainEvent> getAndClearEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    public Collection<ReservationEntity> getReservations() {
        return new ArrayList<>(reservations.values());
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public int getActiveReservationsCount() {
        return (int) reservations.values().stream()
                .filter(ReservationEntity::isActive)
                .count();
    }
    public Set<Symbol> getPositionSymbols() {
        return positionManager.getAllSymbols();
    }
    // Command class
    public static class PlaceOrderCommand {
        private final Symbol symbol;
        private final Money price;
        private final BigDecimal quantity;
        private final OrderType orderType;

        public PlaceOrderCommand(Symbol symbol, Money price, BigDecimal quantity, OrderType orderType) {
            this.symbol = symbol;
            this.price = price;
            this.quantity = quantity;
            this.orderType = orderType;
        }

        public Symbol getSymbol() { return symbol; }
        public Money getPrice() { return price; }
        public BigDecimal getQuantity() { return quantity; }
        public OrderType getOrderType() { return orderType; }
    }

    // Exceptions
    public static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String message) {
            super(message);
        }
    }

    public static class InsufficientAssetsException extends RuntimeException {
        public InsufficientAssetsException(String message) {
            super(message);
        }
    }
}