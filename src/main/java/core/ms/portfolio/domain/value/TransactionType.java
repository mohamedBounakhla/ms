package core.ms.portfolio.domain.value;

public enum TransactionType {
    BUY("Asset Purchase"),
    SELL("Asset Sale"),
    DEPOSIT("Cash Deposit"),
    WITHDRAWAL("Cash Withdrawal");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns true if this transaction type affects the cash balance.
     */
    public boolean affectsCash() {
        return switch (this) {
            case BUY, WITHDRAWAL -> true;  // Decreases cash
            case SELL, DEPOSIT -> true;    // Increases cash
        };
    }

    /**
     * Returns true if this transaction type affects asset positions.
     */
    public boolean affectsPosition() {
        return switch (this) {
            case BUY -> true;     // Increases position
            case SELL -> true;    // Decreases position
            case DEPOSIT, WITHDRAWAL -> false; // Cash only
        };
    }

    /**
     * Returns true if this is an asset-related transaction (buy/sell).
     */
    public boolean isAssetTransaction() {
        return this == BUY || this == SELL;
    }

    /**
     * Returns true if this is a cash-only transaction (deposit/withdrawal).
     */
    public boolean isCashTransaction() {
        return this == DEPOSIT || this == WITHDRAWAL;
    }

    /**
     * Returns true if this transaction increases cash balance.
     */
    public boolean increasesCash() {
        return this == SELL || this == DEPOSIT;
    }

    /**
     * Returns true if this transaction decreases cash balance.
     */
    public boolean decreasesCash() {
        return this == BUY || this == WITHDRAWAL;
    }

    @Override
    public String toString() {
        return description;
    }
}