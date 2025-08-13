package core.ms.portfolio.domain.aggregates;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.portfolio.domain.entities.*;
import core.ms.portfolio.domain.value_object.Deposit;
import core.ms.portfolio.domain.value_object.ResourceConsumption;
import core.ms.portfolio.domain.value_object.WalletOperationVisitor;
import core.ms.portfolio.domain.value_object.Withdrawal;
import core.ms.portfolio.domain.value_object.state.ReservationState;
import core.ms.portfolio.domain.value_object.state.ReservationStates;
import core.ms.portfolio.domain.value_object.state.ReservationTransitionContext;
import core.ms.portfolio.domain.value_object.validation.ValidationResult;
import core.ms.security.domain.MSUser;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import core.ms.utils.IdGenerator;

import java.util.*;
import java.math.BigDecimal;
import java.util.stream.Collectors;

public class Portfolio {
    private final String portfolioId;
    private final String ownerId;
    private final Map<Currency, Money> cashBalances;
    private final Map<Symbol, Position> positions;
    private final List<CashReservation> cashReservations;
    private final List<AssetReservation> assetReservations;
    private final List<WalletOperation> operations;
    private final BalanceComputer balanceComputer;

    public Portfolio(String portfolioId, String ownerId) {
        this.portfolioId = portfolioId;
        this.ownerId = ownerId;
        this.cashBalances = new HashMap<>();
        this.positions = new HashMap<>();
        this.cashReservations = new ArrayList<>();
        this.assetReservations = new ArrayList<>();
        this.operations = new ArrayList<>();
        this.balanceComputer = new BalanceComputer();
    }

    // ===== RESERVATION CREATION =====

    public CashReservation createCashReservation(IBuyOrder buyOrder, Money amount) {
        // Validate available funds
        Money available = getAvailableCash(amount.getCurrency());
        if (available.isLessThan(amount)) {
            throw new InsufficientFundsException(
                    String.format("Cannot reserve %s. Available: %s",
                            amount.toDisplayString(), available.toDisplayString())
            );
        }

        String reservationId = IdGenerator.generateReservationId();
        CashReservation reservation = new CashReservation(reservationId, buyOrder, amount);
        cashReservations.add(reservation);

        // Initial state entry
        reservation.getState().onEntry(reservation);

        return reservation;
    }

    public AssetReservation createAssetReservation(ISellOrder sellOrder, Symbol symbol, BigDecimal quantity) {
        // Validate available assets
        BigDecimal available = getAvailableAssets(symbol);
        if (available.compareTo(quantity) < 0) {
            throw new InsufficientAssetsException(
                    String.format("Cannot reserve %s %s. Available: %s",
                            quantity, symbol.getCode(), available)
            );
        }

        String reservationId = IdGenerator.generateReservationId();
        AssetReservation reservation = new AssetReservation(reservationId, sellOrder, symbol, quantity);
        assetReservations.add(reservation);

        // Initial state entry
        reservation.getState().onEntry(reservation);

        return reservation;
    }

    // ===== RESERVATION STATE TRANSITIONS =====

    /**
     * Execute a reservation when the order is filled
     */
    public void executeReservation(String reservationId,
                                   ReservationTransitionContext.ExecutionDetails executionDetails) {
        Reservation<?> reservation = findReservation(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        // Create validation context
        ReservationTransitionContext context = new ReservationTransitionContext(reservation, this)
                .withExecutionDetails(executionDetails);

        // Validate transition
        ValidationResult validation = reservation.getState().validateExecute(context);
        if (!validation.isValid()) {
            throw new InvalidStateTransitionException(
                    "Cannot execute reservation: " + validation.getErrorMessage()
            );
        }

        // Perform state transition
        performStateTransition(reservation, ReservationStates.EXECUTED);

        // Consume resources based on reservation type
        consumeReservationResources(reservation);

        // Mark reservation as consumed
        reservation.markAsConsumed();
    }

    /**
     * Cancel a reservation
     */
    public void cancelReservation(String reservationId,
                                  ReservationTransitionContext.CancellationContext cancellationContext) {
        Reservation<?> reservation = findReservation(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        // Create validation context
        ReservationTransitionContext context = new ReservationTransitionContext(reservation, this)
                .withCancellationContext(cancellationContext);

        // Validate transition
        ValidationResult validation = reservation.getState().validateCancel(context);
        if (!validation.isValid()) {
            throw new InvalidStateTransitionException(
                    "Cannot cancel reservation: " + validation.getErrorMessage()
            );
        }

        // Perform state transition
        performStateTransition(reservation, ReservationStates.CANCELLED);

        // Mark as released (no resources consumed)
        reservation.markAsReleased();
    }

    /**
     * Expire a reservation
     */
    public void expireReservation(String reservationId) {
        Reservation<?> reservation = findReservation(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        // Create validation context
        ReservationTransitionContext context = new ReservationTransitionContext(reservation, this);

        // Validate transition
        ValidationResult validation = reservation.getState().validateExpire(context);
        if (!validation.isValid()) {
            throw new InvalidStateTransitionException(
                    "Cannot expire reservation: " + validation.getErrorMessage()
            );
        }

        // Perform state transition
        performStateTransition(reservation, ReservationStates.EXPIRED);

        // Mark as released (no resources consumed)
        reservation.markAsReleased();
    }

    /**
     * Process all expired reservations
     */
    public void processExpiredReservations() {
        List<Reservation<?>> expiredReservations = getExpiredReservations();
        for (Reservation<?> reservation : expiredReservations) {
            try {
                expireReservation(reservation.getReservationId());
            } catch (Exception e) {
                // Log error but continue processing
                System.err.println("Failed to expire reservation " +
                        reservation.getReservationId() + ": " + e.getMessage());
            }
        }
    }

    // ===== INTERNAL RESOURCE MANAGEMENT =====

    private void performStateTransition(Reservation<?> reservation, ReservationState newState) {
        ReservationState oldState = reservation.getState();
        oldState.onExit(reservation);
        reservation.transitionTo(newState);
        newState.onEntry(reservation);
    }

    private void consumeReservationResources(Reservation<?> reservation) {
        ResourceConsumption consumption = reservation.getResourceConsumption();

        if (consumption.isCashConsumption()) {
            // Consume cash for executed buy orders
            Money toConsume = consumption.getCashToConsume();
            Currency currency = toConsume.getCurrency();
            Money currentBalance = getTotalCash(currency);
            Money newBalance = currentBalance.subtract(toConsume);
            setCashBalance(currency, newBalance);

        } else if (consumption.isAssetConsumption()) {
            // Consume assets for executed sell orders
            Symbol symbol = consumption.getAssetSymbol();
            BigDecimal quantity = consumption.getAssetQuantity();
            reducePosition(symbol, quantity);
        }
    }

    // ===== RESERVATION QUERIES =====

    /**
     * Get total reserved cash (only PENDING reservations count)
     */
    public Money getReservedCash(Currency currency) {
        return cashReservations.stream()
                .filter(Reservation::isPending)
                .map(CashReservation::getReservedAmount)
                .filter(m -> m.getCurrency().equals(currency))
                .reduce(Money.zero(currency), Money::add);
    }

    /**
     * Get total reserved assets (only PENDING reservations count)
     */
    public BigDecimal getReservedAssets(Symbol symbol) {
        return assetReservations.stream()
                .filter(Reservation::isPending)
                .filter(r -> r.getSymbol().equals(symbol))
                .map(AssetReservation::getReservedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean hasExecutedReservation(String reservationId) {
        return findReservation(reservationId)
                .map(Reservation::isExecuted)
                .orElse(false);
    }

    public boolean hasConsumedReservation(String reservationId) {
        return hasExecutedReservation(reservationId);
    }

    public List<CashReservation> getPendingCashReservations() {
        return cashReservations.stream()
                .filter(Reservation::isPending)
                .collect(Collectors.toList());
    }

    public List<AssetReservation> getPendingAssetReservations() {
        return assetReservations.stream()
                .filter(Reservation::isPending)
                .collect(Collectors.toList());
    }

    public Optional<Reservation<?>> findReservation(String reservationId) {
        Optional<CashReservation> cashRes = cashReservations.stream()
                .filter(r -> r.getReservationId().equals(reservationId))
                .findFirst();

        if (cashRes.isPresent()) {
            return Optional.of(cashRes.get());
        }

        return assetReservations.stream()
                .filter(r -> r.getReservationId().equals(reservationId))
                .findFirst()
                .map(r -> (Reservation<?>) r);
    }

    public Optional<CashReservation> findCashReservationByOrderId(String orderId) {
        return cashReservations.stream()
                .filter(r -> r.getOrder().getId().equals(orderId))
                .findFirst();
    }

    public Optional<AssetReservation> findAssetReservationByOrderId(String orderId) {
        return assetReservations.stream()
                .filter(r -> r.getOrder().getId().equals(orderId))
                .findFirst();
    }

    public List<Reservation<?>> getExpiredReservations() {
        List<Reservation<?>> expired = new ArrayList<>();

        expired.addAll(cashReservations.stream()
                .filter(r -> r.isExpired() && r.canExpire())
                .collect(Collectors.toList()));

        expired.addAll(assetReservations.stream()
                .filter(r -> r.isExpired() && r.canExpire())
                .collect(Collectors.toList()));

        return expired;
    }

    // ===== BALANCE CALCULATIONS =====

    public Money getAvailableCash(Currency currency) {
        Money total = getTotalCash(currency);
        Money reserved = getReservedCash(currency);
        return total.subtract(reserved);
    }

    public BigDecimal getAvailableAssets(Symbol symbol) {
        Position position = positions.get(symbol);
        if (position == null) return BigDecimal.ZERO;

        BigDecimal total = position.getQuantity();
        BigDecimal reserved = getReservedAssets(symbol);
        return total.subtract(reserved);
    }

    public Money getTotalCash(Currency currency) {
        return cashBalances.getOrDefault(currency, Money.zero(currency));
    }

    private void setCashBalance(Currency currency, Money newBalance) {
        if (!newBalance.getCurrency().equals(currency)) {
            throw new IllegalArgumentException("Currency mismatch");
        }
        cashBalances.put(currency, newBalance);
    }

    public Position getPosition(Symbol symbol) {
        return positions.get(symbol);
    }

    // ===== POSITION MANAGEMENT =====

    public void addPosition(Symbol symbol, BigDecimal quantity, Money price) {
        Position position = positions.get(symbol);
        if (position == null) {
            String positionId = IdGenerator.generateReservationId();
            position = new Position(positionId, symbol, quantity, price);
            positions.put(symbol, position);
        } else {
            position.increase(quantity, price);
        }
    }

    public void reducePosition(Symbol symbol, BigDecimal quantity) {
        Position position = positions.get(symbol);
        if (position != null) {
            position.decrease(quantity);
            if (position.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                positions.remove(symbol);
            }
        }
    }

    // ===== WALLET OPERATIONS =====

    public void applyOperation(WalletOperation operation) {
        if (!operation.validate()) {
            throw new InvalidOperationException("Operation validation failed: " + operation.getOperationId());
        }
        operation.accept(balanceComputer);
        operations.add(operation);
    }

    public Money computeTotalOperationEffect(Currency currency) {
        return operations.stream()
                .filter(op -> op.getAmount().getCurrency().equals(currency))
                .map(op -> balanceComputer.computeEffect(op))
                .reduce(Money.zero(currency), Money::add);
    }

    public List<WalletOperation> getOperationHistory(Currency currency) {
        return operations.stream()
                .filter(op -> op.getAmount().getCurrency().equals(currency))
                .collect(Collectors.toList());
    }

    // ===== BALANCE COMPUTER (VISITOR) =====

    private class BalanceComputer implements WalletOperationVisitor<Money> {

        @Override
        public Money visitDeposit(Deposit deposit) {
            Money amount = deposit.getAmount();
            Currency currency = amount.getCurrency();
            Money currentBalance = getTotalCash(currency);
            Money newBalance = currentBalance.add(amount);
            cashBalances.put(currency, newBalance);
            return newBalance;
        }

        @Override
        public Money visitWithdrawal(Withdrawal withdrawal) {
            Money amount = withdrawal.getAmount();
            Currency currency = amount.getCurrency();
            Money currentBalance = getTotalCash(currency);
            Money availableBalance = getAvailableCash(currency);

            if (availableBalance.isLessThan(amount)) {
                throw new InsufficientFundsException(
                        String.format("Insufficient funds. Available: %s, Required: %s",
                                availableBalance.toDisplayString(),
                                amount.toDisplayString())
                );
            }

            Money newBalance = currentBalance.subtract(amount);
            cashBalances.put(currency, newBalance);
            return newBalance;
        }

        public Money computeEffect(WalletOperation operation) {
            if (operation instanceof Deposit) {
                return operation.getAmount();
            } else if (operation instanceof Withdrawal) {
                return operation.getAmount().negate();
            }
            return Money.zero(operation.getAmount().getCurrency());
        }
    }

    // ===== GETTERS =====

    public String getPortfolioId() { return portfolioId; }
    public String getOwner() { return ownerId; }
    public List<WalletOperation> getOperations() { return new ArrayList<>(operations); }
    public List<CashReservation> getCashReservations() { return new ArrayList<>(cashReservations); }
    public List<AssetReservation> getAssetReservations() { return new ArrayList<>(assetReservations); }

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

    public static class InvalidOperationException extends RuntimeException {
        public InvalidOperationException(String message) {
            super(message);
        }
    }

    public static class InvalidStateTransitionException extends RuntimeException {
        public InvalidStateTransitionException(String message) {
            super(message);
        }
    }
}