package core.ms.portfolio.domain.positions;

import core.ms.order.domain.entities.ISellOrder;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class PositionManager {
    private final Map<Symbol, AssetWallet> wallets;
    private final Map<String, InternalReservation> internalReservations;

    public PositionManager() {
        this.wallets = new TreeMap<>();
        this.internalReservations = new HashMap<>();
    }

    // ===== QUERY METHODS =====

    public BigDecimal getAvailable(Symbol symbol) {
        AssetWallet wallet = getWallet(symbol);
        BigDecimal total = wallet.getQuantity();
        BigDecimal reserved = getReserved(symbol);
        return total.subtract(reserved);
    }

    public BigDecimal getTotal(Symbol symbol) {
        return getWallet(symbol).getQuantity();
    }

    public BigDecimal getReserved(Symbol symbol) {
        return internalReservations.values().stream()
                .filter(r -> r.getSymbol().equals(symbol))
                .map(InternalReservation::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ===== INTERNAL RESERVATION METHODS =====

    /**
     * Create an internal reservation for the saga pattern
     */
    public void createInternalReservation(String reservationId, Symbol symbol, BigDecimal quantity) {
        // Check available quantity
        BigDecimal available = getAvailable(symbol);
        if (available.compareTo(quantity) < 0) {
            throw new InsufficientAssetsException(
                    String.format("Cannot reserve %s %s. Available: %s",
                            quantity, symbol.getCode(), available)
            );
        }

        internalReservations.put(reservationId, new InternalReservation(
                reservationId, symbol, quantity
        ));
    }

    /**
     * Execute a reservation (consume the reserved assets)
     */
    public void executeReservation(String reservationId, Symbol symbol) {
        InternalReservation reservation = internalReservations.remove(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found: " + reservationId);
        }

        if (!reservation.getSymbol().equals(symbol)) {
            throw new IllegalArgumentException("Symbol mismatch for reservation: " + reservationId);
        }

        // Deduct from wallet
        AssetWallet wallet = getWallet(symbol);
        wallet.deduct(reservation.getQuantity());
    }

    /**
     * Release a reservation (make assets available again)
     */
    public void releaseReservation(String reservationId, Symbol symbol) {
        InternalReservation reservation = internalReservations.remove(reservationId);
        if (reservation == null) {
            return; // Already released or executed
        }

        if (!reservation.getSymbol().equals(symbol)) {
            throw new IllegalArgumentException("Symbol mismatch for reservation: " + reservationId);
        }

        // Assets become available again (no action needed, just remove from map)
    }

    // ===== ASSET OPERATIONS =====

    /**
     * Add assets after buy order execution
     */
    public void addAssets(Symbol symbol, BigDecimal quantity, Money price) {
        getWallet(symbol).addAssets(quantity, price);
    }

    // ===== MAINTENANCE =====

    public void cleanupExpired() {
        Instant cutoff = Instant.now().minusSeconds(300); // 5 minute timeout
        internalReservations.entrySet().removeIf(entry ->
                entry.getValue().getCreatedAt().isBefore(cutoff)
        );
    }

    // ===== PRIVATE METHODS =====

    private AssetWallet getWallet(Symbol symbol) {
        return wallets.computeIfAbsent(symbol, AssetWallet::new);
    }

    // ===== INNER CLASSES =====

    /**
     * Internal asset wallet for a specific symbol
     */
    private static class AssetWallet {
        private final Symbol symbol;
        private BigDecimal quantity;
        private Money averageCost;

        public AssetWallet(Symbol symbol) {
            this.symbol = symbol;
            this.quantity = BigDecimal.ZERO;
            this.averageCost = Money.zero(symbol.getQuoteCurrency());
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public Money getAverageCost() {
            return averageCost;
        }

        public void addAssets(BigDecimal qty, Money price) {
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }

            // Update average cost
            Money currentTotal = averageCost.multiply(quantity);
            Money additionalCost = price.multiply(qty);
            BigDecimal newQuantity = quantity.add(qty);

            if (newQuantity.compareTo(BigDecimal.ZERO) > 0) {
                averageCost = currentTotal.add(additionalCost).divide(newQuantity);
            }
            quantity = newQuantity;
        }

        public void deduct(BigDecimal qty) {
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            if (quantity.compareTo(qty) < 0) {
                throw new InsufficientAssetsException(
                        String.format("Insufficient quantity. Required: %s, Available: %s",
                                qty, quantity)
                );
            }
            quantity = quantity.subtract(qty);
        }

        // PnL calculations
        public Money getCurrentValue(Money marketPrice) {
            return marketPrice.multiply(quantity);
        }

        public Money getUnrealizedPnL(Money marketPrice) {
            return getCurrentValue(marketPrice).subtract(averageCost.multiply(quantity));
        }
    }

    /**
     * Internal reservation tracking
     */
    private static class InternalReservation {
        private final String reservationId;
        private final Symbol symbol;
        private final BigDecimal quantity;
        private final Instant createdAt;

        public InternalReservation(String reservationId, Symbol symbol, BigDecimal quantity) {
            this.reservationId = reservationId;
            this.symbol = symbol;
            this.quantity = quantity;
            this.createdAt = Instant.now();
        }

        public String getReservationId() { return reservationId; }
        public Symbol getSymbol() { return symbol; }
        public BigDecimal getQuantity() { return quantity; }
        public Instant getCreatedAt() { return createdAt; }
    }

    public static class InsufficientAssetsException extends RuntimeException {
        public InsufficientAssetsException(String message) {
            super(message);
        }
    }
    public void deposit(Symbol symbol, BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit quantity must be positive");
        }

        AssetWallet wallet = getWallet(symbol);

        wallet.addAssets(quantity, Money.zero(symbol.getQuoteCurrency()));
    }
}