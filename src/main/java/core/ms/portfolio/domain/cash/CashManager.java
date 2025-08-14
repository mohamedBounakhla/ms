package core.ms.portfolio.domain.cash;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;

import java.util.Map;
import java.util.TreeMap;

public class CashManager {
    private final Map<Currency, CashWallet> wallets;

    public CashManager() {
        this.wallets = new TreeMap<>();
    }

    // ===== QUERY METHODS =====
    public Money getAvailable(Currency currency) {
        return getWallet(currency).getAvailableBalance();
    }

    public Money getTotal(Currency currency) {
        return getWallet(currency).getTotalBalance();
    }

    public Money getReserved(Currency currency) {
        return getWallet(currency).getReservedBalance();
    }

    // ===== RESERVATION METHODS =====
    public CashReservation reserve(IBuyOrder order) {
        Money amount = order.getTotalValue();
        return getWallet(amount.getCurrency()).reserve(order);
    }

    public void executeReservation(String reservationId, Currency currency) {
        getWallet(currency).execute(reservationId);
    }

    public void releaseReservation(String reservationId, Currency currency) {
        getWallet(currency).release(reservationId);
    }

    // ===== CASH OPERATIONS =====
    public void deposit(Money amount) {
        getWallet(amount.getCurrency()).addCash(amount);
    }

    public void withdraw(Money amount) {
        getWallet(amount.getCurrency()).removeCash(amount);
    }

    private CashWallet getWallet(Currency currency) {
        return wallets.computeIfAbsent(currency, CashWallet::new);
    }
}