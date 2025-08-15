package core.ms.portfolio.domain;


import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.portfolio.domain.cash.CashManager;
import core.ms.portfolio.domain.cash.CashReservation;
import core.ms.portfolio.domain.events.publish.BuyOrderRequestedEvent;
import core.ms.portfolio.domain.events.publish.SellOrderRequestedEvent;
import core.ms.portfolio.domain.events.subscribe.OrderCancelledEvent;
import core.ms.portfolio.domain.events.subscribe.OrderExpiredEvent;
import core.ms.portfolio.domain.events.subscribe.OrderRejectedEvent;
import core.ms.portfolio.domain.events.subscribe.TransactionCreatedEvent;
import core.ms.portfolio.domain.positions.AssetReservation;
import core.ms.portfolio.domain.positions.PositionManager;

import core.ms.shared.OrderType;
import core.ms.shared.events.DomainEvent;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


public class Portfolio {
    private final String portfolioId;
    private final String ownerId;
    private final CashManager cashManager;
    private final PositionManager positionManager;
    private final List<DomainEvent> domainEvents = new ArrayList<>();


    public Portfolio(String portfolioId, String ownerId,
                     CashManager cashManager, PositionManager positionManager) {
        this.portfolioId = portfolioId;
        this.ownerId = ownerId;
        this.cashManager = cashManager;
        this.positionManager = positionManager;
    }

    // ===== ORDER PLACEMENT (Creates Reservations) =====

    /**
     * Places a buy order by reserving the required cash
     * @return CashReservation that can be executed or released later
     */
    public CashReservation placeBuyOrder(IBuyOrder order) {
        CashReservation reservation = cashManager.reserve(order);

        // Emit event
        domainEvents.add(new BuyOrderRequestedEvent(
                portfolioId,
                reservation.getReservationId(),
                order.getSymbol(),
                order.getQuantity(),
                order.getPrice(),
                order.getTotalValue()
        ));

        return reservation;    }

    /**
     * Places a sell order by reserving the required assets
     * @return AssetReservation that can be executed or released later
     */
    public AssetReservation placeSellOrder(ISellOrder order) {
        AssetReservation reservation = positionManager.reserve(order);

        // Emit event
        domainEvents.add(new SellOrderRequestedEvent(
                portfolioId,
                reservation.getReservationId(),
                order.getSymbol(),
                order.getQuantity(),
                order.getPrice()
        ));

        return reservation;    }

    public List<DomainEvent> getAndClearEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }



    // ===== EVENT HANDLERS (for consumed events) =====

    /**
     * Handle trade execution from Order/Transaction BC
     */
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        if (!portfolioId.equals(event.getPortfolioId())) {
            throw new IllegalArgumentException("Transaction event not for this portfolio");
        }

        if (event.getType() == OrderType.BUY) {
            executeBuyOrder(
                    event.getReservationId(),
                    event.getSymbol().getQuoteCurrency(),
                    event.getSymbol(),
                    event.getExecutedQuantity(),
                    event.getExecutedPrice()
            );
        } else {
            executeSellOrder(
                    event.getReservationId(),
                    event.getSymbol(),
                    event.getTotalValue()
            );
        }
    }

    /**
     * Handle order cancellation - release reservation
     */
    public void handleOrderCancelled(OrderCancelledEvent event) {
        if (!portfolioId.equals(event.getPortfolioId())) {
            throw new IllegalArgumentException("Cancel event not for this portfolio");
        }

        if (event.getType() == OrderType.BUY) {
            cancelBuyOrder(event.getReservationId(), event.getCurrency());
        } else {
            cancelSellOrder(event.getReservationId(), event.getSymbol());
        }
    }

    /**
     * Handle order expiration - release reservation
     */
    public void handleOrderExpired(OrderExpiredEvent event) {
        if (!portfolioId.equals(event.getPortfolioId())) {
            throw new IllegalArgumentException("Expiration event not for this portfolio");
        }

        if (event.getType() == OrderType.BUY) {
            cancelBuyOrder(event.getReservationId(), event.getCurrency());
        } else {
            cancelSellOrder(event.getReservationId(), event.getSymbol());
        }
    }

    /**
     * Handle order rejection - release reservation
     */
    public void handleOrderRejected(OrderRejectedEvent event) {
        if (!portfolioId.equals(event.getPortfolioId())) {
            throw new IllegalArgumentException("Rejection event not for this portfolio");
        }

        if (event.getType() == OrderType.BUY) {
            cancelBuyOrder(event.getReservationId(), event.getCurrency());
        } else {
            cancelSellOrder(event.getReservationId(), event.getSymbol());
        }
    }
    // ===== ORDER EXECUTION (Consumes Reservations) =====

    /**
     * Executes a buy order: consumes reserved cash, receives assets
     */
    public void executeBuyOrder(String cashReservationId, Currency currency,
                                Symbol symbol, BigDecimal quantity, Money price) {
        // 1. Consume reserved cash
        cashManager.executeReservation(cashReservationId, currency);

        // 2. Add received assets (no reservation needed for receiving)
        positionManager.addAssets(symbol, quantity, price);
    }

    /**
     * Executes a sell order: consumes reserved assets, receives cash
     */
    public void executeSellOrder(String assetReservationId, Symbol symbol,
                                 Money proceeds) {
        // 1. Consume reserved assets
        positionManager.executeReservation(assetReservationId, symbol);

        // 2. Deposit proceeds
        cashManager.deposit(proceeds);
    }

    // ===== ORDER CANCELLATION (Releases Reservations) =====

    /**
     * Cancels a buy order by releasing its cash reservation
     */
    public void cancelBuyOrder(String reservationId, Currency currency) {
        cashManager.releaseReservation(reservationId, currency);
    }

    /**
     * Cancels a sell order by releasing its asset reservation
     */
    public void cancelSellOrder(String reservationId, Symbol symbol) {
        positionManager.releaseReservation(reservationId, symbol);
    }

    // ===== CASH MANAGEMENT =====

    /**
     * Deposits external cash into the portfolio
     */
    public void depositCash(Money amount) {
        cashManager.deposit(amount);
    }

    /**
     * Withdraws cash from the portfolio (checks available balance)
     */
    public void withdrawCash(Money amount) {
        cashManager.withdraw(amount);
    }

    // ===== QUERY METHODS =====

    // Cash queries
    public Money getAvailableCash(Currency currency) {
        return cashManager.getAvailable(currency);
    }

    public Money getTotalCash(Currency currency) {
        return cashManager.getTotal(currency);
    }

    public Money getReservedCash(Currency currency) {
        return cashManager.getReserved(currency);
    }

    // Asset queries
    public BigDecimal getAvailableAssets(Symbol symbol) {
        return positionManager.getAvailable(symbol);
    }

    public BigDecimal getTotalAssets(Symbol symbol) {
        return positionManager.getTotal(symbol);
    }

    public BigDecimal getReservedAssets(Symbol symbol) {
        return positionManager.getReserved(symbol);
    }

    // ===== MAINTENANCE =====

    /**
     * Cleans up expired reservations across all wallets
     */
    public void cleanupExpiredReservations() {
        cashManager.cleanupExpired();
        positionManager.cleanupExpired();
    }

    // ===== IDENTITY =====

    public String getPortfolioId() {
        return portfolioId;
    }

    public String getOwnerId() {
        return ownerId;
    }
}