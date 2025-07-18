package core.ms.order.domain.entities;

import core.ms.order.domain.validators.ValidateOrderState;
import core.ms.order.domain.validators.annotation.OrderNotFinal;
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


    }

    private void validateTransaction() {
        // Validate symbols match
        if (!buyOrder.getSymbol().equals(symbol) || !sellOrder.getSymbol().equals(symbol)) {
            throw new IllegalArgumentException("All orders must have the same symbol");
        }

        // ===== NEW: ORDER MATCHING VALIDATION (REPLACES STRICT PRICE MATCHING) =====
        validateOrderMatching();

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

    // ===== NEW: ORDER MATCHING LOGIC =====
    private void validateOrderMatching() {
        // Check if buy price >= sell price (can match)
        if (buyOrder.getPrice().isLessThan(sellOrder.getPrice())) {
            throw new IllegalArgumentException("Orders cannot match: buy price is less than sell price");
        }

        // Check if execution price is within valid range
        if (price.isLessThan(sellOrder.getPrice()) || price.isGreaterThan(buyOrder.getPrice())) {
            throw new IllegalArgumentException("Execution price must be between sell price and buy price");
        }
    }

    private void validateQuantityConstraints() {
        if (quantity.compareTo(buyOrder.getRemainingQuantity()) > 0) {
            throw new IllegalArgumentException("Transaction quantity cannot exceed buy order remaining quantity");
        }
        if (quantity.compareTo(sellOrder.getRemainingQuantity()) > 0) {
            throw new IllegalArgumentException("Transaction quantity cannot exceed sell order remaining quantity");
        }
    }



    // ===== GETTERS =====
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

    @Override
    public Money getTotalValue() {
        return price.multiply(quantity);
    }

    // ===== STATIC HELPER METHOD FOR OPTIMAL PRICING =====
    public static Money determineExecutionPrice(IBuyOrder buyOrder, ISellOrder sellOrder) {
        if (buyOrder.getPrice().isLessThan(sellOrder.getPrice())) {
            throw new IllegalArgumentException("Orders cannot match: buy price is less than sell price");
        }

        // Use mid-point pricing for fair execution
        Money buyPrice = buyOrder.getPrice();
        Money sellPrice = sellOrder.getPrice();

        // Calculate mid-point: (buy + sell) / 2
        Money sum = buyPrice.add(sellPrice);
        Money midPoint = sum.divide(new BigDecimal("2"));

        return midPoint;
    }

    // ===== OBJECT METHODS =====
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