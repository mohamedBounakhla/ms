package core.ms.portfolio.domain.cash;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class CashManager {
    private final Map<Currency, CashWallet> wallets;
    private final Map<String, InternalReservation> internalReservations;

    public CashManager() {
        this.wallets = new TreeMap<>();
        this.internalReservations = new HashMap<>();
    }

    // ===== QUERY METHODS =====

    public Money getAvailable(Currency currency) {
        CashWallet wallet = getWallet(currency);
        Money total = wallet.getBalance();
        Money reserved = getReserved(currency);
        return total.subtract(reserved);
    }

    public Money getTotal(Currency currency) {
        return getWallet(currency).getBalance();
    }

    public Money getReserved(Currency currency) {
        return internalReservations.values().stream()
                .filter(r -> r.getCurrency().equals(currency))
                .map(InternalReservation::getAmount)
                .reduce(Money.zero(currency), Money::add);
    }

    // ===== INTERNAL RESERVATION METHODS =====

    /**
     * Create an internal reservation for the saga pattern
     */
    public void createInternalReservation(String reservationId, Money amount) {
        Currency currency = amount.getCurrency();

        // Check available balance
        if (getAvailable(currency).isLessThan(amount)) {
            throw new InsufficientFundsException(
                    String.format("Cannot reserve %s. Available: %s",
                            amount.toDisplayString(),
                            getAvailable(currency).toDisplayString())
            );
        }

        internalReservations.put(reservationId, new InternalReservation(
                reservationId, amount, currency
        ));
    }

    /**
     * Execute a reservation (consume the reserved funds)
     */
    public void executeReservation(String reservationId, Currency currency) {
        InternalReservation reservation = internalReservations.remove(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found: " + reservationId);
        }

        if (!reservation.getCurrency().equals(currency)) {
            throw new IllegalArgumentException("Currency mismatch for reservation: " + reservationId);
        }

        // Deduct from wallet
        CashWallet wallet = getWallet(currency);
        wallet.deduct(reservation.getAmount());
    }

    /**
     * Release a reservation (make funds available again)
     */
    public void releaseReservation(String reservationId, Currency currency) {
        InternalReservation reservation = internalReservations.remove(reservationId);
        if (reservation == null) {
            return; // Already released or executed
        }

        if (!reservation.getCurrency().equals(currency)) {
            throw new IllegalArgumentException("Currency mismatch for reservation: " + reservationId);
        }

        // Funds become available again (no action needed, just remove from map)
    }

    // ===== CASH OPERATIONS =====

    public void deposit(Money amount) {
        getWallet(amount.getCurrency()).deposit(amount);
    }

    public void withdraw(Money amount) {
        Currency currency = amount.getCurrency();

        // Check available balance
        if (getAvailable(currency).isLessThan(amount)) {
            throw new InsufficientFundsException(
                    String.format("Cannot withdraw %s. Available: %s",
                            amount.toDisplayString(),
                            getAvailable(currency).toDisplayString())
            );
        }

        getWallet(currency).deduct(amount);
    }

    // ===== MAINTENANCE =====

    public void cleanupExpired() {
        Instant cutoff = Instant.now().minusSeconds(300); // 5 minute timeout
        internalReservations.entrySet().removeIf(entry ->
                entry.getValue().getCreatedAt().isBefore(cutoff)
        );
    }

    // ===== PRIVATE METHODS =====

    private CashWallet getWallet(Currency currency) {
        return wallets.computeIfAbsent(currency, CashWallet::new);
    }

    // ===== INNER CLASSES =====

    /**
     * Internal cash wallet for a specific currency
     */
    private static class CashWallet {
        private final Currency currency;
        private Money balance;

        public CashWallet(Currency currency) {
            this.currency = currency;
            this.balance = Money.zero(currency);
        }

        public Money getBalance() {
            return balance;
        }

        public void deposit(Money amount) {
            validateCurrency(amount);
            if (!amount.isPositive()) {
                throw new IllegalArgumentException("Deposit amount must be positive");
            }
            balance = balance.add(amount);
        }

        public void deduct(Money amount) {
            validateCurrency(amount);
            if (!amount.isPositive()) {
                throw new IllegalArgumentException("Deduction amount must be positive");
            }
            if (balance.isLessThan(amount)) {
                throw new InsufficientFundsException(
                        String.format("Insufficient balance. Required: %s, Available: %s",
                                amount.toDisplayString(), balance.toDisplayString())
                );
            }
            balance = balance.subtract(amount);
        }

        private void validateCurrency(Money amount) {
            if (!amount.getCurrency().equals(currency)) {
                throw new IllegalArgumentException(
                        String.format("Currency mismatch. Expected: %s, Got: %s",
                                currency, amount.getCurrency())
                );
            }
        }
    }

    /**
     * Internal reservation tracking
     */
    private static class InternalReservation {
        private final String reservationId;
        private final Money amount;
        private final Currency currency;
        private final Instant createdAt;

        public InternalReservation(String reservationId, Money amount, Currency currency) {
            this.reservationId = reservationId;
            this.amount = amount;
            this.currency = currency;
            this.createdAt = Instant.now();
        }

        public String getReservationId() { return reservationId; }
        public Money getAmount() { return amount; }
        public Currency getCurrency() { return currency; }
        public Instant getCreatedAt() { return createdAt; }
    }

    public static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String message) {
            super(message);
        }
    }
}