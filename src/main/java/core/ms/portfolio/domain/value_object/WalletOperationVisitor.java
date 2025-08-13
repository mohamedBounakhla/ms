package core.ms.portfolio.domain.value_object;

public interface WalletOperationVisitor <T>{
    T visitDeposit(Deposit deposit);
    T visitWithdrawal(Withdrawal withdrawal);
}
