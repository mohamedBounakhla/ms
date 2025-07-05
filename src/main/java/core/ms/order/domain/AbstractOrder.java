package core.ms.order.domain;

import core.ms.order.domain.value.OrderStatus;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public abstract class AbstractOrder implements IOrder {
    protected final String id;
    protected final Symbol symbol;
    protected Money price;
    protected final BigDecimal quantity;
    protected OrderStatus status; // State Pattern OrderStatus
    protected final LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    protected AbstractOrder(String id, Symbol symbol, Money price, BigDecimal quantity) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");
        this.price = Objects.requireNonNull(price, "Price cannot be null");
        this.quantity = Objects.requireNonNull(quantity, "Quantity cannot be null");
        this.status = new OrderStatus(); // Initialize state pattern
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        validatePriceCurrency(price);
        validateQuantity(quantity);
    }

    // ===== GETTERS =====

    @Override
    public String getId() { return id; }

    @Override
    public Symbol getSymbol() { return symbol; }

    @Override
    public Money getPrice() { return price; }

    @Override
    public BigDecimal getQuantity() { return quantity; }

    @Override
    public OrderStatus getStatus() { return status; }

    @Override
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public String getSymbolCode() {
        return symbol.getCode();
    }

    // ===== BUSINESS METHODS - DELEGATING TO STATE PATTERN =====

    @Override
    public void cancel() {
        status.cancelOrder();
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public void cancelPartial() {
        status.cancelPartialOrder();
        this.updatedAt = LocalDateTime.now();
        // Note: For partial cancellation, you'd also need to update quantity
        // This is simplified for the academic project
    }

    @Override
    public void fillPartial() {
        status.fillPartialOrder();
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public void complete() {
        status.completeOrder();
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public void updatePrice(Money price) {
        Objects.requireNonNull(price, "Price cannot be null");
        validatePriceCurrency(price);

        if (status.isTerminal()) {
            throw new IllegalStateException("Cannot update price of terminal order");
        }

        this.price = price;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public Money getTotalValue() {
        return price.multiply(quantity);
    }

    @Override
    public boolean isActive() {
        return !status.isTerminal();
    }

    // ===== PROTECTED METHODS =====

    protected void validatePriceCurrency(Money price) {
        if (!price.getCurrency().equals(symbol.getQuoteCurrency())) {
            throw new IllegalArgumentException(
                    String.format("Price currency %s does not match symbol quote currency %s",
                            price.getCurrency(), symbol.getQuoteCurrency()));
        }
    }

    private void validateQuantity(BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }

    // ===== OBJECT METHODS =====

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AbstractOrder that = (AbstractOrder) obj;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("%s[%s, %s, %s @ %s, %s]",
                getClass().getSimpleName(), id, symbol.getFullSymbol(),
                quantity, price, status.getStatus());
    }
}