package core.ms.portfolio.domain;


import core.ms.portfolio.domain.cash.CashManager;
import core.ms.portfolio.domain.positions.PositionManager;

import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;


import java.math.BigDecimal;


public class Portfolio {
    private final String portfolioId;
    private final String ownerId;
    private final CashManager cashManager;
    private final PositionManager positionManager;

    public Portfolio(String portfolioId, String ownerId,
                     CashManager cashManager, PositionManager positionManager) {
        this.portfolioId = portfolioId;
        this.ownerId = ownerId;
        this.cashManager = cashManager;
        this.positionManager = positionManager;
    }

    // ===== TRADE ORCHESTRATION =====

    /**
     * Orchestrates buy order execution: consume cash, receive assets
     */
    public void executeBuyOrder(String cashReservationId, Currency currency,
                                Symbol symbol, BigDecimal quantity, Money price) {
        // 1. Consume reserved cash
        cashManager.executeReservation(cashReservationId, currency);

        // 2. Add received assets (no reservation needed for receiving)
        positionManager.addAssets(symbol, quantity, price);
    }

    /**
     * Orchestrates sell order execution: consume assets, receive cash
     */
    public void executeSellOrder(String assetReservationId, Symbol symbol,
                                 Money proceeds) {
        // 1. Consume reserved assets
        positionManager.executeReservation(assetReservationId, symbol);

        // 2. Deposit proceeds
        cashManager.deposit(proceeds);
    }

    /**
     * Handles order cancellation/expiration - releases reservation
     */
    public void releaseReservation(String reservationId, Currency currency) {
        cashManager.releaseReservation(reservationId, currency);
    }

    public void releaseReservation(String reservationId, Symbol symbol) {
        positionManager.releaseReservation(reservationId, symbol);
    }

    // ===== DELEGATION METHODS =====

    // Cash queries
    public Money getAvailableCash(Currency currency) {
        return cashManager.getAvailable(currency);
    }

    public Money getTotalCash(Currency currency) {
        return cashManager.getTotal(currency);
    }

    // Asset queries
    public BigDecimal getAvailableAssets(Symbol symbol) {
        return positionManager.getAvailable(symbol);
    }

    public BigDecimal getTotalAssets(Symbol symbol) {
        return positionManager.getTotal(symbol);
    }

    // Identity
    public String getPortfolioId() { return portfolioId; }
    public String getOwnerId() { return ownerId; }
}