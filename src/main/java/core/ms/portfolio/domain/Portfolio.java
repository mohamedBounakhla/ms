package core.ms.portfolio.domain;

import core.ms.portfolio.domain.cash.CashManager;
import core.ms.portfolio.domain.events.publish.OrderRequestedEvent;
import core.ms.portfolio.domain.events.subscribe.OrderCreatedEvent;
import core.ms.portfolio.domain.events.subscribe.OrderCreationFailedEvent;
import core.ms.portfolio.domain.events.subscribe.TransactionCreatedEvent;
import core.ms.portfolio.domain.positions.PositionManager;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Portfolio {
    private static final Logger logger = LoggerFactory.getLogger(Portfolio.class);
    private static final String SOURCE_BC = "PORTFOLIO_BC";

    private final String portfolioId;
    private final String ownerId;
    private final CashManager cashManager;
    private final PositionManager positionManager;
    private final List<DomainEvent> domainEvents;
    private final Map<String, ReservationMetadata> activeReservations;

    public Portfolio(String portfolioId, String ownerId,
                     CashManager cashManager, PositionManager positionManager) {
        this.portfolioId = portfolioId;
        this.ownerId = ownerId;
        this.cashManager = cashManager;
        this.positionManager = positionManager;
        this.domainEvents = new ArrayList<>();
        this.activeReservations = new HashMap<>();
    }

    // ===== ORDER PLACEMENT (Saga Initiation) =====

    public void placeOrder(PlaceOrderCommand command) {
        logger.info("Starting placeOrder for symbol: {}, type: {}, quantity: {}",
                command.getSymbol().getCode(), command.getOrderType(), command.getQuantity());

        // Start new saga
        EventContext.startNewSaga();
        String correlationId = EventContext.getCurrentCorrelationId();
        String reservationId = IdGen.generate("reservation");

        logger.info("Generated correlation ID: {}, reservation ID: {}", correlationId, reservationId);

        try {
            if (command.getOrderType() == OrderType.BUY) {
                handleBuyOrderReservation(command, reservationId, correlationId);
            } else {
                handleSellOrderReservation(command, reservationId, correlationId);
            }

            // Create and queue the event
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

        } catch (Exception e) {
            logger.error("Exception in placeOrder: {}", e.getMessage(), e);
            releaseReservation(reservationId);
            throw e;
        }
    }

    private void handleBuyOrderReservation(PlaceOrderCommand command, String reservationId, String correlationId) {
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
        activeReservations.put(reservationId, new ReservationMetadata(
                reservationId, command.getSymbol(), OrderType.BUY,
                totalValue.getCurrency(), null, correlationId
        ));
    }

    private void handleSellOrderReservation(PlaceOrderCommand command, String reservationId, String correlationId) {
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
        activeReservations.put(reservationId, new ReservationMetadata(
                reservationId, command.getSymbol(), OrderType.SELL,
                null, command.getSymbol(), correlationId
        ));
    }

    // ===== EVENT HANDLERS (Saga Participants) =====

    public void handleOrderCreated(OrderCreatedEvent event) {
        if (!portfolioId.equals(event.getPortfolioId())) {
            return;
        }

        ReservationMetadata metadata = activeReservations.get(event.getReservationId());
        if (metadata != null) {
            metadata.setOrderId(event.getOrderId());
            metadata.setStatus(ReservationStatus.CONFIRMED);
            logger.info("Order creation confirmed for reservation: {}", event.getReservationId());
        }
    }

    public void handleOrderCreationFailed(OrderCreationFailedEvent event) {
        if (!portfolioId.equals(event.getPortfolioId())) {
            return;
        }

        logger.warn("Order creation failed for reservation: {}, releasing", event.getReservationId());
        releaseReservation(event.getReservationId());
    }

    public void handleTransactionCreated(TransactionCreatedEvent event) {
        if (portfolioId.equals(event.getBuyPortfolioId())) {
            executeBuyTransaction(
                    event.getBuyReservationId(),
                    event.getSymbol(),
                    event.getExecutedQuantity(),
                    event.getExecutedPrice()
            );
        }

        if (portfolioId.equals(event.getSellPortfolioId())) {
            executeSellTransaction(
                    event.getSellReservationId(),
                    event.getSymbol(),
                    event.getTotalValue()
            );
        }
    }

    // ===== TRANSACTION EXECUTION =====

    private void executeBuyTransaction(String reservationId, Symbol symbol,
                                       BigDecimal quantity, Money price) {
        ReservationMetadata metadata = activeReservations.get(reservationId);
        if (metadata == null || metadata.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Invalid reservation state for execution: " + reservationId);
        }

        cashManager.executeReservation(reservationId, metadata.getCurrency());
        positionManager.addAssets(symbol, quantity, price);

        metadata.setStatus(ReservationStatus.EXECUTED);
        activeReservations.remove(reservationId);

        logger.info("Buy transaction executed for reservation: {}", reservationId);
    }

    private void executeSellTransaction(String reservationId, Symbol symbol, Money proceeds) {
        ReservationMetadata metadata = activeReservations.get(reservationId);
        if (metadata == null || metadata.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Invalid reservation state for execution: " + reservationId);
        }

        positionManager.executeReservation(reservationId, symbol);
        cashManager.deposit(proceeds);

        metadata.setStatus(ReservationStatus.EXECUTED);
        activeReservations.remove(reservationId);

        logger.info("Sell transaction executed for reservation: {}", reservationId);
    }

    // ===== RESERVATION MANAGEMENT =====

    private void releaseReservation(String reservationId) {
        ReservationMetadata metadata = activeReservations.remove(reservationId);
        if (metadata == null) {
            return;
        }

        if (metadata.getOrderType() == OrderType.BUY && metadata.getCurrency() != null) {
            cashManager.releaseReservation(reservationId, metadata.getCurrency());
        } else if (metadata.getOrderType() == OrderType.SELL && metadata.getSymbol() != null) {
            positionManager.releaseReservation(reservationId, metadata.getSymbol());
        }

        metadata.setStatus(ReservationStatus.RELEASED);
        logger.info("Reservation released: {}", reservationId);
    }

    public void cleanupExpiredReservations() {
        Instant cutoff = Instant.now().minusSeconds(300); // 5 minute timeout
        List<String> toRelease = new ArrayList<>();

        for (Map.Entry<String, ReservationMetadata> entry : activeReservations.entrySet()) {
            if (entry.getValue().getCreatedAt().isBefore(cutoff)
                    && entry.getValue().getStatus() == ReservationStatus.PENDING) {
                toRelease.add(entry.getKey());
            }
        }

        toRelease.forEach(this::releaseReservation);
        cashManager.cleanupExpired();
        positionManager.cleanupExpired();
    }

    // ===== CASH OPERATIONS =====

    public void depositCash(Money amount) {
        cashManager.deposit(amount);
    }

    public void withdrawCash(Money amount) {
        cashManager.withdraw(amount);
    }

    // ===== QUERY METHODS =====

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

    // ===== DOMAIN EVENTS =====

    public List<DomainEvent> getAndClearEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public int getActiveReservationsCount() {
        return activeReservations.size();
    }

    // ===== COMMAND =====

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

    // ===== INNER CLASSES =====

    private static class ReservationMetadata {
        private final String reservationId;
        private final Symbol symbol;
        private final OrderType orderType;
        private final Currency currency;
        private final Symbol assetSymbol;
        private final String correlationId;
        private final Instant createdAt;
        private String orderId;
        private ReservationStatus status;

        public ReservationMetadata(String reservationId, Symbol symbol, OrderType orderType,
                                   Currency currency, Symbol assetSymbol, String correlationId) {
            this.reservationId = reservationId;
            this.symbol = symbol;
            this.orderType = orderType;
            this.currency = currency;
            this.assetSymbol = assetSymbol;
            this.correlationId = correlationId;
            this.createdAt = Instant.now();
            this.status = ReservationStatus.PENDING;
        }

        public String getReservationId() { return reservationId; }
        public Symbol getSymbol() { return symbol; }
        public OrderType getOrderType() { return orderType; }
        public Currency getCurrency() { return currency; }
        public Symbol getAssetSymbol() { return assetSymbol; }
        public String getCorrelationId() { return correlationId; }
        public Instant getCreatedAt() { return createdAt; }
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public ReservationStatus getStatus() { return status; }
        public void setStatus(ReservationStatus status) { this.status = status; }
    }

    private enum ReservationStatus {
        PENDING, CONFIRMED, EXECUTED, RELEASED
    }

    // ===== EXCEPTIONS =====

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
    public void depositAsset(Symbol symbol, BigDecimal quantity) {
        positionManager.deposit(symbol, quantity);
    }
}