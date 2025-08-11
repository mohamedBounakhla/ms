package core.ms.portfolio.domain;

import core.ms.security.domain.MSUser;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.util.stream.Collectors;

public class Portfolio {
    private final String portfolioId;
    private final MSUser owner;
    private final Map<Currency, Money> cashBalances;
    private final Map<Symbol, Position> positions;
    private final List<Reservation> reservations;
    private final List<WalletOperation> operations;
    private final BalanceComputer balanceComputer;

    public Portfolio(String portfolioId, MSUser owner) {
        this.portfolioId = portfolioId;
        this.owner = owner;
        this.cashBalances = new HashMap<>();
        this.positions = new HashMap<>();
        this.reservations = new ArrayList<>();
        this.operations = new ArrayList<>();
        this.balanceComputer = new BalanceComputer();
    }

    /**
     * Embedded class for computing balance changes using Visitor pattern
     */
    private class BalanceComputer implements WalletOperationVisitor<Money> {

        @Override
        public Money visitDeposit(Deposit deposit) {
            Money amount = deposit.getAmount();
            Currency currency = amount.getCurrency();
            Money currentBalance = getTotalCash(currency);
            Money newBalance = currentBalance.add(amount);

            // Update the balance
            cashBalances.put(currency, newBalance);

            return newBalance;
        }

        @Override
        public Money visitWithdrawal(Withdrawal withdrawal) {
            Money amount = withdrawal.getAmount();
            Currency currency = amount.getCurrency();
            Money currentBalance = getTotalCash(currency);

            // Check if sufficient funds (considering reservations)
            Money availableBalance = getAvailableCash(currency);
            if (availableBalance.isLessThan(amount)) {
                throw new InsufficientFundsException(
                        String.format("Insufficient funds. Available: %s, Required: %s",
                                availableBalance.toDisplayString(),
                                amount.toDisplayString())
                );
            }

            Money newBalance = currentBalance.subtract(amount);

            // Update the balance
            cashBalances.put(currency, newBalance);

            return newBalance;
        }

        /**
         * Computes the net effect of an operation without applying it
         */
        public Money computeEffect(WalletOperation operation) {
            if (operation instanceof Deposit) {
                return operation.getAmount();
            } else if (operation instanceof Withdrawal) {
                return operation.getAmount().negate();
            }
            return Money.zero(operation.getAmount().getCurrency());
        }
    }

    public Money getAvailableCash(Currency currency) {
        Money total = getTotalCash(currency);
        Money reserved = calculateReservedCash(currency);
        return total.subtract(reserved);
    }

    private Money calculateReservedCash(Currency currency) {
        return reservations.stream()
                .filter(r -> r.status == ReservationStatus.PENDING)
                .filter(r -> r instanceof CashReservation)
                .map(r -> ((CashReservation) r).getReservedAmount())
                .filter(m -> m.getCurrency() == currency)
                .reduce(Money.zero(currency), Money::add);
    }

    public Money getTotalCash(Currency currency) {
        return cashBalances.getOrDefault(currency, Money.zero(currency));
    }

    public Position getPosition(Symbol symbol) {
        return positions.get(symbol);
    }

    public BigDecimal getAvailablePosition(Symbol symbol) {
        Position position = positions.get(symbol);
        if (position == null) return BigDecimal.ZERO;

        BigDecimal reserved = reservations.stream()
                .filter(r -> r.status == ReservationStatus.PENDING)
                .filter(r -> r instanceof AssetReservation)
                .map(r -> (AssetReservation) r)
                .filter(r -> r.getSymbol().equals(symbol))
                .map(AssetReservation::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return position.getQuantity().subtract(reserved);
    }

    public void applyOperation(WalletOperation operation) {
        if (!operation.validate()) {
            throw new InvalidOperationException("Operation validation failed: " + operation.getOperationId());
        }

        // Use the visitor pattern to apply the operation
        Money newBalance = operation.accept(balanceComputer);

        // Record the operation
        operations.add(operation);
    }

    /**
     * Computes the total balance change from all operations
     */
    public Money computeTotalOperationEffect(Currency currency) {
        return operations.stream()
                .filter(op -> op.getAmount().getCurrency() == currency)
                .map(op -> balanceComputer.computeEffect(op))
                .reduce(Money.zero(currency), Money::add);
    }

    /**
     * Gets the operation history for a specific currency
     */
    public List<WalletOperation> getOperationHistory(Currency currency) {
        return operations.stream()
                .filter(op -> op.getAmount().getCurrency() == currency)
                .collect(Collectors.toList());
    }

    // Custom exceptions for better error handling
    public static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String message) {
            super(message);
        }
    }

    public static class InvalidOperationException extends RuntimeException {
        public InvalidOperationException(String message) {
            super(message);
        }
    }

    // Getters
    public String getPortfolioId() { return portfolioId; }
    public MSUser getOwner() { return owner; }
    public List<WalletOperation> getOperations() { return new ArrayList<>(operations); }
    public List<Reservation> getReservations() { return new ArrayList<>(reservations); }
}