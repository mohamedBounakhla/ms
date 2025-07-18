package core.ms.order.domain.entities;

import core.ms.order.domain.value_objects.OrderStatus;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractOrder implements IOrder {
    protected final String id;
    protected final Symbol symbol;
    protected Money price;
    protected final BigDecimal quantity;
    protected OrderStatus status;
    protected final LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    // ===== EXECUTION TRACKING (Internal State Only) =====
    protected BigDecimal executedQuantity;

    protected AbstractOrder(String id, Symbol symbol, Money price, BigDecimal quantity) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");
        this.price = Objects.requireNonNull(price, "Price cannot be null");
        this.quantity = Objects.requireNonNull(quantity, "Quantity cannot be null");
        this.status = new OrderStatus();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        this.executedQuantity = BigDecimal.ZERO;

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

    // ===== EXECUTION TRACKING =====
    @Override
    public BigDecimal getExecutedQuantity() {
        return executedQuantity;
    }

    @Override
    public BigDecimal getRemainingQuantity() {
        BigDecimal remaining = quantity.subtract(executedQuantity);
        return remaining.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO : remaining;
    }

    // ===== NEW CLEAN EXECUTION METHODS =====

    @Override
    public void updateExecution(BigDecimal executedAmount) {
        Objects.requireNonNull(executedAmount, "Executed amount cannot be null");

        if (executedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Executed amount must be positive");
        }

        if (executedAmount.compareTo(getRemainingQuantity()) > 0) {
            throw new IllegalArgumentException("Executed amount exceeds remaining quantity");
        }

        this.executedQuantity = this.executedQuantity.add(executedAmount);
        this.updatedAt = LocalDateTime.now();
        updateStatusAfterExecution();
    }

    @Override
    public void setExecutedQuantity(BigDecimal executedQuantity) {
        Objects.requireNonNull(executedQuantity, "Executed quantity cannot be null");

        if (executedQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Executed quantity cannot be negative");
        }

        if (executedQuantity.compareTo(quantity) > 0) {
            throw new IllegalArgumentException("Executed quantity cannot exceed total quantity");
        }

        this.executedQuantity = executedQuantity;
        this.updatedAt = LocalDateTime.now();
        updateStatusAfterExecution();
    }

    private void updateStatusAfterExecution() {
        BigDecimal remaining = getRemainingQuantity();

        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            if (status.getStatus() != OrderStatusEnum.FILLED) {
                status.completeOrder();
            }
        } else if (executedQuantity.compareTo(BigDecimal.ZERO) > 0) {
            if (status.getStatus() == OrderStatusEnum.PENDING) {
                status.fillPartialOrder();
            }
        }
    }

    // ===== STATUS OPERATIONS =====
    @Override
    public void cancel() {
        status.cancelOrder();
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public void cancelPartial() {
        status.cancelPartialOrder();
        this.updatedAt = LocalDateTime.now();
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

    // ===== BUSINESS LOGIC =====
    @Override
    public Money getTotalValue() {
        return price.multiply(quantity);
    }

    @Override
    public boolean isActive() {
        return !status.isTerminal();
    }

    public String getSymbolCode() {
        return symbol.getCode();
    }

    // ===== VALIDATION =====
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
        return String.format("%s[%s, %s, %s @ %s, %s, executed: %s, remaining: %s]",
                getClass().getSimpleName(), id, symbol.getFullSymbol(),
                quantity, price, status.getStatus(), executedQuantity, getRemainingQuantity());
    }
}