package core.ms.portfolio.domain;

import core.ms.portfolio.domain.value.TransactionType;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import core.ms.utils.IdGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class Portfolio {
    private final String id;
    private final String name;
    private final String userId;
    private Money cashBalance;
    private final Currency baseCurrency;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final Map<Symbol, Position> positions;
    private final List<PortfolioTransaction> transactions;
    private final IdGenerator idGenerator;

    public Portfolio(String id, String name, String userId, Money initialCashBalance) {
        this.id = Objects.requireNonNull(id, "Portfolio ID cannot be null");
        this.name = Objects.requireNonNull(name, "Portfolio name cannot be null");
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.cashBalance = Objects.requireNonNull(initialCashBalance, "Initial cash balance cannot be null");
        this.baseCurrency = initialCashBalance.getCurrency();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.positions = new HashMap<>();
        this.transactions = new ArrayList<>();
        this.idGenerator = new IdGenerator();

        validatePortfolio();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUserId() {
        return userId;
    }

    public Money getCashBalance() {
        return cashBalance;
    }

    public Currency getBaseCurrency() {
        return baseCurrency;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Collection<Position> getPositions() {
        return new ArrayList<>(positions.values());
    }

    public List<PortfolioTransaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    // Business Operations

    /**
     * Deposits cash into the portfolio.
     */
    public PortfolioTransaction depositCash(Money amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        validateCurrency(amount, "Deposit amount");

        if (amount.isNegative() || amount.isZero()) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        updateCashBalance(amount);

        PortfolioTransaction transaction = PortfolioTransaction.createDepositTransaction(
                idGenerator.generateTransactionId(), id, amount);
        addTransaction(transaction);

        return transaction;
    }

    /**
     * Withdraws cash from the portfolio.
     */
    public PortfolioTransaction withdrawCash(Money amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        validateCurrency(amount, "Withdrawal amount");
        validateSufficientCash(amount);

        updateCashBalance(amount.negate());

        PortfolioTransaction transaction = PortfolioTransaction.createWithdrawalTransaction(
                idGenerator.generateTransactionId(), id, amount);
        addTransaction(transaction);

        return transaction;
    }

    /**
     * Buys an asset and updates the portfolio.
     */
    public PortfolioTransaction buyAsset(Symbol symbol, BigDecimal quantity, Money price, String relatedOrderId) {
        Objects.requireNonNull(symbol, "Symbol cannot be null");
        Objects.requireNonNull(quantity, "Quantity cannot be null");
        Objects.requireNonNull(price, "Price cannot be null");
        validateCurrency(price, "Price");

        Money totalCost = price.multiply(quantity);
        validateSufficientCash(totalCost);

        // Update cash balance
        updateCashBalance(totalCost.negate());

        // Update position
        updatePosition(symbol, quantity, price, TransactionType.BUY);

        // Create transaction record
        PortfolioTransaction transaction = PortfolioTransaction.createBuyTransaction(
                idGenerator.generateTransactionId(), id, symbol, quantity, price, relatedOrderId);
        addTransaction(transaction);

        return transaction;
    }

    /**
     * Sells an asset and updates the portfolio.
     */
    public PortfolioTransaction sellAsset(Symbol symbol, BigDecimal quantity, Money price, String relatedOrderId) {
        Objects.requireNonNull(symbol, "Symbol cannot be null");
        Objects.requireNonNull(quantity, "Quantity cannot be null");
        Objects.requireNonNull(price, "Price cannot be null");
        validateCurrency(price, "Price");
        validateSufficientPosition(symbol, quantity);

        Money totalProceeds = price.multiply(quantity);

        // Update position
        updatePosition(symbol, quantity, price, TransactionType.SELL);

        // Update cash balance
        updateCashBalance(totalProceeds);

        // Create transaction record
        PortfolioTransaction transaction = PortfolioTransaction.createSellTransaction(
                idGenerator.generateTransactionId(), id, symbol, quantity, price, relatedOrderId);
        addTransaction(transaction);

        return transaction;
    }

    // Query Operations

    /**
     * Gets a specific position by symbol.
     */
    public Optional<Position> getPosition(Symbol symbol) {
        Objects.requireNonNull(symbol, "Symbol cannot be null");
        return Optional.ofNullable(positions.get(symbol));
    }

    /**
     * Calculates the total portfolio value including cash and positions.
     */
    public Money getTotalValue(Map<Symbol, Money> currentPrices) {
        Objects.requireNonNull(currentPrices, "Current prices cannot be null");

        Money totalPositionValue = positions.values().stream()
                .map(position -> {
                    Money currentPrice = currentPrices.get(position.getSymbol());
                    return currentPrice != null ? position.getCurrentValue(currentPrice) :
                            Money.zero(baseCurrency);
                })
                .reduce(Money.zero(baseCurrency), Money::add);

        return cashBalance.add(totalPositionValue);
    }

    /**
     * Calculates the total profit/loss across all positions.
     */
    public Money getTotalProfitLoss(Map<Symbol, Money> currentPrices) {
        Objects.requireNonNull(currentPrices, "Current prices cannot be null");

        return positions.values().stream()
                .map(position -> {
                    Money currentPrice = currentPrices.get(position.getSymbol());
                    return currentPrice != null ? position.getProfitLoss(currentPrice) :
                            Money.zero(baseCurrency);
                })
                .reduce(Money.zero(baseCurrency), Money::add);
    }

    /**
     * Calculates the total invested amount (cost basis of all positions).
     */
    public Money getTotalInvested() {
        return positions.values().stream()
                .map(Position::getCostBasis)
                .reduce(Money.zero(baseCurrency), Money::add);
    }

    /**
     * Checks if the portfolio has sufficient cash for a transaction.
     */
    public boolean canAfford(Money amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        validateCurrency(amount, "Amount");
        return cashBalance.isGreaterThanOrEqual(amount);
    }

    /**
     * Gets transactions filtered by type.
     */
    public List<PortfolioTransaction> getTransactionsByType(TransactionType type) {
        Objects.requireNonNull(type, "Transaction type cannot be null");
        return transactions.stream()
                .filter(tx -> tx.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * Gets transactions for a specific symbol.
     */
    public List<PortfolioTransaction> getTransactionsForSymbol(Symbol symbol) {
        Objects.requireNonNull(symbol, "Symbol cannot be null");
        return transactions.stream()
                .filter(tx -> symbol.equals(tx.getSymbol()))
                .collect(Collectors.toList());
    }

    // Private helper methods

    private void updateCashBalance(Money amount) {
        this.cashBalance = cashBalance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    private void updatePosition(Symbol symbol, BigDecimal quantity, Money price, TransactionType type) {
        Position existingPosition = positions.get(symbol);

        if (type == TransactionType.BUY) {
            if (existingPosition == null) {
                // Create new position
                String positionId = idGenerator.generateTransactionId(); // Reuse for position IDs
                Position newPosition = new Position(positionId, id, symbol, quantity, price);
                positions.put(symbol, newPosition);
            } else {
                // Add to existing position
                existingPosition.addQuantity(quantity, price);
            }
        } else if (type == TransactionType.SELL) {
            if (existingPosition == null) {
                throw new IllegalStateException("Cannot sell asset with no position");
            }

            existingPosition.removeQuantity(quantity);

            // Remove position if empty
            if (existingPosition.isEmpty()) {
                positions.remove(symbol);
            }
        }
    }

    private void addTransaction(PortfolioTransaction transaction) {
        transactions.add(transaction);
        this.updatedAt = LocalDateTime.now();
    }

    private void validateSufficientCash(Money amount) {
        if (!canAfford(amount)) {
            throw new IllegalArgumentException(
                    String.format("Insufficient cash. Required: %s, Available: %s",
                            amount, cashBalance));
        }
    }

    private void validateSufficientPosition(Symbol symbol, BigDecimal quantity) {
        Position position = positions.get(symbol);
        if (position == null) {
            throw new IllegalArgumentException("No position exists for symbol: " + symbol.getCode());
        }

        if (!position.hasSufficientQuantity(quantity)) {
            throw new IllegalArgumentException(
                    String.format("Insufficient position. Required: %s, Available: %s",
                            quantity, position.getQuantity()));
        }
    }

    private void validateCurrency(Money money, String fieldName) {
        if (!money.getCurrency().equals(baseCurrency)) {
            throw new IllegalArgumentException(
                    String.format("%s currency %s does not match portfolio base currency %s",
                            fieldName, money.getCurrency(), baseCurrency));
        }
    }

    private void validatePortfolio() {
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Portfolio name cannot be empty");
        }

        if (cashBalance.isNegative()) {
            throw new IllegalArgumentException("Initial cash balance cannot be negative");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Portfolio portfolio = (Portfolio) obj;
        return Objects.equals(id, portfolio.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Portfolio[%s: '%s' - %s cash, %d positions, %d transactions]",
                id, name, cashBalance, positions.size(), transactions.size());
    }
}