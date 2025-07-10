package core.ms.order.domain;

import core.ms.order.domain.value.OrderStatus;
import core.ms.order.domain.value.OrderStatusEnum;
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

    protected BigDecimal executedQuantity;
    protected final List<ITransaction> transactions;

    protected AbstractOrder(String id, Symbol symbol, Money price, BigDecimal quantity) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");
        this.price = Objects.requireNonNull(price, "Price cannot be null");
        this.quantity = Objects.requireNonNull(quantity, "Quantity cannot be null");
        this.status = new OrderStatus();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        this.executedQuantity = BigDecimal.ZERO;
        this.transactions = new ArrayList<>();

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

    // ===== QUANTITY TRACKING GETTERS =====
    @Override
    public BigDecimal getExecutedQuantity() {
        return executedQuantity;
    }


    @Override
    public BigDecimal getRemainingQuantity() {
        BigDecimal remaining = quantity.subtract(executedQuantity);

        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return remaining;
    }

    @Override
    public List<ITransaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    @Override
    public int getTransactionSequence(ITransaction transaction) {
        int index = transactions.indexOf(transaction);
        return index >= 0 ? index + 1 : -1;
    }

    // ===== TRANSACTION MANAGEMENT =====
    @Override
    public void addTransaction(ITransaction transaction, BigDecimal executedAmount) {
        Objects.requireNonNull(transaction, "Transaction cannot be null");
        Objects.requireNonNull(executedAmount, "Executed amount cannot be null");

        if (executedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Executed amount must be positive");
        }

        if (executedAmount.compareTo(getRemainingQuantity()) > 0) {
            throw new IllegalArgumentException("Executed amount exceeds remaining quantity");
        }

        transactions.add(transaction);

        executedQuantity = executedQuantity.add(executedAmount);

        // If the result is zero, use BigDecimal.ZERO
        if (executedQuantity.compareTo(BigDecimal.ZERO) == 0) {
            executedQuantity = BigDecimal.ZERO;
        }

        updatedAt = LocalDateTime.now();
        updateStatusAfterExecution();
    }

    private void updateStatusAfterExecution() {
        BigDecimal remaining = getRemainingQuantity();

        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            if (status.getStatus() != OrderStatusEnum.FILLED) {
                status.completeOrder();
            }
        } else {
            if (status.getStatus() == OrderStatusEnum.PENDING) {
                status.fillPartialOrder();
            }
        }
    }

    // ===== BUSINESS METHODS =====
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
        return String.format("%s[%s, %s, %s @ %s, %s, executed: %s, remaining: %s]",
                getClass().getSimpleName(), id, symbol.getFullSymbol(),
                quantity, price, status.getStatus(), executedQuantity, getRemainingQuantity());
    }
}
