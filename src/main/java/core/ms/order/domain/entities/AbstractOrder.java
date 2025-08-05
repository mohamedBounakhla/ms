package core.ms.order.domain.entities;

import core.ms.order.domain.value_objects.OrderStatus;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Pure order entity - NO business rules, NO validation.
 * All validation is handled by the factory/builder pattern.
 */
public abstract class AbstractOrder implements IOrder {
    protected final String id;
    protected final Symbol symbol;
    protected Money price;
    protected final BigDecimal quantity;
    protected OrderStatus status;
    protected final LocalDateTime createdAt;
    protected LocalDateTime updatedAt;
    protected BigDecimal executedQuantity;

    protected AbstractOrder(String id, Symbol symbol, Money price, BigDecimal quantity) {
        this.id = id;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.status = new OrderStatus();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.executedQuantity = BigDecimal.ZERO;
    }

    // Alternative constructor for builder pattern
    protected AbstractOrder(String id, Symbol symbol, Money price, BigDecimal quantity,
                            OrderStatus status, LocalDateTime createdAt, LocalDateTime updatedAt,
                            BigDecimal executedQuantity) {
        this.id = id;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.executedQuantity = executedQuantity;
    }

    // ===== PURE GETTERS =====
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

    @Override
    public BigDecimal getExecutedQuantity() { return executedQuantity; }

    @Override
    public BigDecimal getRemainingQuantity() {
        BigDecimal remaining = quantity.subtract(executedQuantity);
        return remaining.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO : remaining;
    }

    // ===== STATE CHANGE METHODS (Domain Behavior) =====

    @Override
    public void updateExecution(BigDecimal executedAmount) {
        // Domain behavior - update execution quantity
        this.executedQuantity = this.executedQuantity.add(executedAmount);
        this.updatedAt = LocalDateTime.now();
        updateStatusAfterExecution();
    }

    @Override
    public void setExecutedQuantity(BigDecimal executedQuantity) {
        // Domain behavior - set execution quantity directly
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
        // Domain behavior - update price
        this.price = price;
        this.updatedAt = LocalDateTime.now();
    }

    // ===== BUSINESS LOGIC (Pure Calculations) =====
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