package core.ms.portfolio.domain.cash;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.utils.idgenerator.IdGen;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class CashWallet {
    private final Currency currency;
    private Money balance;
    private final Map<String, CashReservation> activeReservations;

    public CashWallet(Currency currency) {
        this.currency = currency;
        this.balance = Money.zero(currency);
        this.activeReservations = new HashMap<>();
    }

    public Money getAvailableBalance() {
        Money reserved = activeReservations.values().stream()
                .map(r -> Money.of(r.getReservedAmount(), currency))
                .reduce(Money.zero(currency), Money::add);
        return balance.subtract(reserved);
    }

    public Money getTotalBalance() {
        return balance;
    }

    public Money getReservedBalance() {
        return activeReservations.values().stream()
                .map(r -> Money.of(r.getReservedAmount(), currency))
                .reduce(Money.zero(currency), Money::add);
    }

    public CashReservation reserve(IBuyOrder order) {
        Money needed = order.getTotalValue();
        if (getAvailableBalance().isLessThan(needed)) {
            throw new InsufficientFundsException(
                    String.format("Cannot reserve %s. Available: %s",
                            needed.toDisplayString(),
                            getAvailableBalance().toDisplayString())
            );
        }

        String reservationId = IdGen.generate("cash-reservation");
        CashReservation reservation = new CashReservation(reservationId, order);
        activeReservations.put(reservationId, reservation);
        return reservation;
    }

    public void execute(String reservationId) {
        CashReservation reservation = activeReservations.remove(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found: " + reservationId);
        }

        // Cash is consumed here through reservation
        Money toConsume = Money.of(reservation.getReservedAmount(), currency);
        balance = balance.subtract(toConsume);
    }

    public void release(String reservationId) {
        if (activeReservations.remove(reservationId) == null) {
            throw new IllegalArgumentException("Reservation not found: " + reservationId);
        }
    }

    // Called when cash is received (deposits or sell proceeds)
    public void addCash(Money amount) {
        if (!amount.getCurrency().equals(currency)) {
            throw new IllegalArgumentException(
                    String.format("Currency mismatch. Expected: %s, Got: %s",
                            currency, amount.getCurrency())
            );
        }
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        balance = balance.add(amount);
    }

    // For withdrawals (must check available balance)
    public void removeCash(Money amount) {
        if (!amount.getCurrency().equals(currency)) {
            throw new IllegalArgumentException("Currency mismatch");
        }
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (getAvailableBalance().isLessThan(amount)) {
            throw new InsufficientFundsException(
                    String.format("Cannot withdraw %s. Available: %s",
                            amount.toDisplayString(),
                            getAvailableBalance().toDisplayString())
            );
        }
        balance = balance.subtract(amount);
    }

    public void cleanupExpired() {
        Instant now = Instant.now();
        activeReservations.entrySet().removeIf(entry ->
                entry.getValue().isExpired()
        );
    }

    public static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String message) {
            super(message);
        }
    }
}