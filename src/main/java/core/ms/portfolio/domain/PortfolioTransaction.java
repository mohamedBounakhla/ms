package core.ms.portfolio.domain;

import core.ms.portfolio.domain.value.TransactionType;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class PortfolioTransaction {
    private final String id;
    private final String portfolioId;
    private final TransactionType type;
    private final Symbol symbol; // Null for cash-only transactions
    private final BigDecimal quantity; // Zero for cash-only transactions
    private final Money price; // Price per unit for asset transactions
    private final Money totalAmount; // Total transaction amount
    private final LocalDateTime timestamp;
    private final String relatedOrderId; // Null if not related to an order

    public PortfolioTransaction(String id, String portfolioId, TransactionType type,
                                Symbol symbol, BigDecimal quantity, Money price,
                                Money totalAmount, String relatedOrderId) {
        this.id = Objects.requireNonNull(id, "Transaction ID cannot be null");
        this.portfolioId = Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null");
        this.type = Objects.requireNonNull(type, "Transaction type cannot be null");
        this.symbol = symbol; // Can be null for cash transactions
        this.quantity = quantity != null ? quantity : BigDecimal.ZERO;
        this.price = price; // Can be null for cash transactions
        this.totalAmount = Objects.requireNonNull(totalAmount, "Total amount cannot be null");
        this.timestamp = LocalDateTime.now();
        this.relatedOrderId = relatedOrderId; // Can be null

        validateTransaction();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public TransactionType getType() {
        return type;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public Money getPrice() {
        return price;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getRelatedOrderId() {
        return relatedOrderId;
    }

    // Business methods
    public boolean isCashTransaction() {
        return type.isCashTransaction();
    }

    public boolean isAssetTransaction() {
        return type.isAssetTransaction();
    }

    public boolean affectsCashBalance() {
        return type.affectsCash();
    }

    public boolean affectsPosition() {
        return type.affectsPosition();
    }

    public String getSymbolCode() {
        return symbol != null ? symbol.getCode() : null;
    }

    // Static factory methods for creating specific transaction types
    public static PortfolioTransaction createBuyTransaction(String id, String portfolioId,
                                                            Symbol symbol, BigDecimal quantity,
                                                            Money price, String relatedOrderId) {
        Money totalAmount = price.multiply(quantity);
        return new PortfolioTransaction(id, portfolioId, TransactionType.BUY,
                symbol, quantity, price, totalAmount, relatedOrderId);
    }

    public static PortfolioTransaction createSellTransaction(String id, String portfolioId,
                                                             Symbol symbol, BigDecimal quantity,
                                                             Money price, String relatedOrderId) {
        Money totalAmount = price.multiply(quantity);
        return new PortfolioTransaction(id, portfolioId, TransactionType.SELL,
                symbol, quantity, price, totalAmount, relatedOrderId);
    }

    public static PortfolioTransaction createDepositTransaction(String id, String portfolioId,
                                                                Money amount) {
        return new PortfolioTransaction(id, portfolioId, TransactionType.DEPOSIT,
                null, BigDecimal.ZERO, null, amount, null);
    }

    public static PortfolioTransaction createWithdrawalTransaction(String id, String portfolioId,
                                                                   Money amount) {
        return new PortfolioTransaction(id, portfolioId, TransactionType.WITHDRAWAL,
                null, BigDecimal.ZERO, null, amount, null);
    }

    private void validateTransaction() {
        if (type.isAssetTransaction()) {
            if (symbol == null) {
                throw new IllegalArgumentException("Symbol is required for asset transactions");
            }
            if (price == null) {
                throw new IllegalArgumentException("Price is required for asset transactions");
            }
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity must be positive for asset transactions");
            }
        }

        if (type.isCashTransaction()) {
            if (symbol != null) {
                throw new IllegalArgumentException("Symbol should be null for cash transactions");
            }
            if (price != null) {
                throw new IllegalArgumentException("Price should be null for cash transactions");
            }
        }

        if (totalAmount.isNegative()) {
            throw new IllegalArgumentException("Total amount cannot be negative");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PortfolioTransaction that = (PortfolioTransaction) obj;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        if (type.isAssetTransaction()) {
            return String.format("PortfolioTransaction[%s: %s %s %s @ %s = %s]",
                    id, type, quantity, symbol.getCode(), price, totalAmount);
        } else {
            return String.format("PortfolioTransaction[%s: %s %s]",
                    id, type, totalAmount);
        }
    }
}