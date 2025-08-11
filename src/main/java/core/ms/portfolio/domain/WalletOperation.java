package core.ms.portfolio.domain;

import core.ms.shared.money.Money;

import java.time.Instant;

public abstract class WalletOperation {
    protected String operationId;
    protected Portfolio portfolio;
    protected Money amount;
    protected Instant timestamp;

    public WalletOperation(String operationId, Portfolio portfolio, Money amount) {
        this.operationId = operationId;
        this.portfolio = portfolio;
        this.amount = amount;
        this.timestamp = Instant.now();
    }

    public abstract void execute();
    public abstract boolean validate();
    public abstract String getType();

    // New abstract method for visitor pattern
    public abstract <T> T accept(WalletOperationVisitor<T> visitor);

    // Getters
    public Money getAmount() { return amount; }
    public Instant getTimestamp() { return timestamp; }
    public String getOperationId() { return operationId; }
}