package core.ms.portfolio.domain;

public interface WalletOperationVisitor <T>{
    T visitDeposit(Deposit deposit);
    T visitWithdrawal(Withdrawal withdrawal);
}
