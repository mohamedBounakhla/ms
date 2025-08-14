package core.ms.portfolio.domain.positions;

import core.ms.order.domain.entities.ISellOrder;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import core.ms.utils.idgenerator.IdGen;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class AssetWallet {
    private final Symbol symbol;
    private BigDecimal quantity;
    private Money averageCost;
    private final Map<String, AssetReservation> activeReservations;

    public AssetWallet(Symbol symbol) {
        this.symbol = symbol;
        this.quantity = BigDecimal.ZERO;
        this.averageCost = Money.zero(symbol.getQuoteCurrency());
        this.activeReservations = new HashMap<>();
    }

    public BigDecimal getAvailableQuantity() {
        BigDecimal reserved = activeReservations.values().stream()
                .map(AssetReservation::getReservedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return quantity.subtract(reserved);
    }

    public BigDecimal getTotalQuantity() {
        return quantity;
    }

    public BigDecimal getReservedQuantity() {
        return activeReservations.values().stream()
                .map(AssetReservation::getReservedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public AssetReservation reserve(ISellOrder order) {
        BigDecimal needed = order.getQuantity();
        BigDecimal available = getAvailableQuantity();

        if (available.compareTo(needed) < 0) {
            throw new InsufficientAssetsException(
                    String.format("Cannot reserve %s %s. Available: %s",
                            needed, symbol.getCode(), available)
            );
        }

        String reservationId = IdGen.generate("reservation");
        AssetReservation reservation = new AssetReservation(reservationId, order);
        activeReservations.put(reservationId, reservation);
        return reservation;
    }

    public void execute(String reservationId) {
        AssetReservation reservation = activeReservations.remove(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found: " + reservationId);
        }

        // Assets are sold here through reservation
        quantity = quantity.subtract(reservation.getReservedAmount());
    }

    public void release(String reservationId) {
        if (activeReservations.remove(reservationId) == null) {
            throw new IllegalArgumentException("Reservation not found: " + reservationId);
        }
    }

    // Called when assets are received (after buy order execution)
    public void addAssets(BigDecimal qty, Money price) {
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        Money currentTotal = averageCost.multiply(quantity);
        Money additionalCost = price.multiply(qty);
        BigDecimal newQuantity = quantity.add(qty);

        if (newQuantity.compareTo(BigDecimal.ZERO) > 0) {
            averageCost = currentTotal.add(additionalCost).divide(newQuantity);
        }
        quantity = newQuantity;
    }

    public void cleanupExpired() {
        Instant now = Instant.now();
        activeReservations.entrySet().removeIf(entry ->
                entry.getValue().isExpired()
        );
    }

    // PnL calculations
    public Money getCurrentValue(Money marketPrice) {
        return marketPrice.multiply(quantity);
    }

    public Money getUnrealizedPnL(Money marketPrice) {
        return getCurrentValue(marketPrice).subtract(averageCost.multiply(quantity));
    }

    // Getters
    public Symbol getSymbol() { return symbol; }
    public BigDecimal getQuantity() { return quantity; }
    public Money getAverageCost() { return averageCost; }

    public static class InsufficientAssetsException extends RuntimeException {
        public InsufficientAssetsException(String message) {
            super(message);
        }
    }
}