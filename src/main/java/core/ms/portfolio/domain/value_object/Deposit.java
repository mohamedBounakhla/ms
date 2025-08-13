package core.ms.portfolio.domain.value_object;

import core.ms.portfolio.domain.aggregates.Portfolio;
import core.ms.portfolio.domain.entities.WalletOperation;
import core.ms.shared.money.Money;

public class Deposit extends WalletOperation {
    private String source;

    public Deposit(String operationId, Portfolio portfolio, Money amount, String source) {
        super(operationId, portfolio, amount);
        this.source = source;
    }

    @Override
    public void execute() {
        // Portfolio will handle this through BalanceComputer
        portfolio.applyOperation(this);
    }

    @Override
    public boolean validate() {
        return amount != null && amount.isPositive();
    }

    @Override
    public String getType() {
        return "DEPOSIT";
    }

    @Override
    public <T> T accept(WalletOperationVisitor<T> visitor) {
        return visitor.visitDeposit(this);
    }

    public String getSource() { return source; }
}