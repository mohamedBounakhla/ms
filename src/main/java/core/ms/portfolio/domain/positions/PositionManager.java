package core.ms.portfolio.domain.positions;

import core.ms.order.domain.entities.ISellOrder;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

public class PositionManager {
    private final Map<Symbol, AssetWallet> wallets;

    public PositionManager() {
        this.wallets = new TreeMap<>();
    }

    // ===== QUERY METHODS =====
    public BigDecimal getAvailable(Symbol symbol) {
        return getWallet(symbol).getAvailableQuantity();
    }

    public BigDecimal getTotal(Symbol symbol) {
        return getWallet(symbol).getTotalQuantity();
    }

    public BigDecimal getReserved(Symbol symbol) {
        return getWallet(symbol).getReservedQuantity();
    }

    // ===== RESERVATION METHODS =====
    public AssetReservation reserve(ISellOrder order) {
        return getWallet(order.getSymbol()).reserve(order);
    }

    public void executeReservation(String reservationId, Symbol symbol) {
        getWallet(symbol).execute(reservationId);
    }

    public void releaseReservation(String reservationId, Symbol symbol) {
        getWallet(symbol).release(reservationId);
    }

    // ===== ASSET RECEIPT (after buy order execution) =====
    public void addAssets(Symbol symbol, BigDecimal quantity, Money price) {
        getWallet(symbol).addAssets(quantity, price);
    }

    // Note: No sell() method - selling MUST go through reservations

    private AssetWallet getWallet(Symbol symbol) {
        return wallets.computeIfAbsent(symbol, AssetWallet::new);
    }
    public void cleanupExpired() {
        wallets.values().forEach(AssetWallet::cleanupExpired);
    }
}