package core.ms.portfolio.domain;

import core.ms.shared.money.Money;

public class Withdrawal extends WalletOperation {
    private String destination;

    public Withdrawal(String operationId, Portfolio portfolio, Money amount, String destination) {
        super(operationId, portfolio, amount);
        this.destination = destination;
    }

    @Override
    public void execute() {
        // Portfolio will handle this through BalanceComputer
        portfolio.applyOperation(this);
    }

    @Override
    public boolean validate() {
        return amount != null && amount.isPositive() &&
                portfolio.getAvailableCash(amount.getCurrency()).isGreaterThanOrEqual(amount);
    }

    @Override
    public String getType() {
        return "WITHDRAWAL";
    }

    @Override
    public <T> T accept(WalletOperationVisitor<T> visitor) {
        return visitor.visitWithdrawal(this);
    }

    public String getDestination() { return destination; }
}