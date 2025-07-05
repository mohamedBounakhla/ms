package core.ms.order.domain;

import core.ms.order.domain.validator.ValidateOrderState;
import core.ms.order.domain.validator.annotation.OrderNotFinal;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public abstract class AbstractTransaction implements ITransaction {
    protected final String id;
    protected final Symbol symbol;

    @OrderNotFinal(message = "Buy order must be active (not FILLED or CANCELLED)")
    protected final IBuyOrder buyOrder;

    @OrderNotFinal(message = "Sell order must be active (not FILLED or CANCELLED)")
    protected final ISellOrder sellOrder;

    protected final Money price;
    protected final BigDecimal quantity;
    protected final LocalDateTime createdAt;

    private static final ValidateOrderState validator = new ValidateOrderState();

    protected AbstractTransaction(String id, Symbol symbol, IBuyOrder buyOrder,
                                  ISellOrder sellOrder, Money price, BigDecimal quantity) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");
        this.buyOrder = Objects.requireNonNull(buyOrder, "Buy order cannot be null");
        this.sellOrder = Objects.requireNonNull(sellOrder, "Sell order cannot be null");
        this.price = Objects.requireNonNull(price, "Price cannot be null");
        this.quantity = Objects.requireNonNull(quantity, "Quantity cannot be null");
        this.createdAt = LocalDateTime.now();

        // Validate using annotations
        validator.validateAndThrow(this);

        // Additional validation
        validateTransaction();

        // ===== NEW: UPDATE ORDER STATES AFTER TRANSACTION =====
        updateOrderStates();
    }

    private void validateTransaction() {
        // Validate symbols match
        if (!buyOrder.getSymbol().equals(symbol) || !sellOrder.getSymbol().equals(symbol)) {
            throw new IllegalArgumentException("All orders must have the same symbol");
        }

        // Validate price consistency
        validatePriceConsistency();

        // Validate quantity constraints
        validateQuantityConstraints();

        // Validate price currency
        if (!price.getCurrency().equals(symbol.getQuoteCurrency())) {
            throw new IllegalArgumentException("Price currency must match symbol quote currency");
        }

        // Validate quantity is positive
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }

    private void validatePriceConsistency() {
        if (!buyOrder.getPrice().equals(price)) {
            throw new IllegalArgumentException("Transaction price must match buy order price");
        }
        if (!sellOrder.getPrice().equals(price)) {
            throw new IllegalArgumentException("Transaction price must match sell order price");
        }
    }

    private void validateQuantityConstraints() {
        if (quantity.compareTo(buyOrder.getQuantity()) > 0) {
            throw new IllegalArgumentException("Transaction quantity cannot exceed buy order quantity");
        }
        if (quantity.compareTo(sellOrder.getQuantity()) > 0) {
            throw new IllegalArgumentException("Transaction quantity cannot exceed sell order quantity");
        }
    }

    /**
     * Updates order states based on execution quantity
     * Business Rule: Orders should transition states when executed
     */
    private void updateOrderStates() {
        updateBuyOrderState();
        updateSellOrderState();
    }

    private void updateBuyOrderState() {
        if (quantity.compareTo(buyOrder.getQuantity()) == 0) {
            // Full execution: PENDING/PARTIAL → FILLED
            buyOrder.complete();
        } else if (quantity.compareTo(buyOrder.getQuantity()) < 0) {
            // Partial execution: PENDING → PARTIAL, PARTIAL → PARTIAL
            buyOrder.fillPartial();
        }
    }

    private void updateSellOrderState() {
        if (quantity.compareTo(sellOrder.getQuantity()) == 0) {
            // Full execution: PENDING/PARTIAL → FILLED
            sellOrder.complete();
        } else if (quantity.compareTo(sellOrder.getQuantity()) < 0) {
            // Partial execution: PENDING → PARTIAL, PARTIAL → PARTIAL
            sellOrder.fillPartial();
        }
    }

    // ... rest of implementation remains same
    @Override
    public String getId() { return id; }

    @Override
    public Symbol getSymbol() { return symbol; }

    @Override
    public IBuyOrder getBuyOrder() { return buyOrder; }

    @Override
    public ISellOrder getSellOrder() { return sellOrder; }

    @Override
    public Money getPrice() { return price; }

    @Override
    public BigDecimal getQuantity() { return quantity; }

    @Override
    public LocalDateTime getCreatedAt() { return createdAt; }

    public Money getTotalValue() {
        return price.multiply(quantity);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AbstractTransaction that = (AbstractTransaction) obj;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("%s[%s, %s, %s @ %s]",
                getClass().getSimpleName(), id, symbol.getFullSymbol(),
                quantity, price);
    }
}
