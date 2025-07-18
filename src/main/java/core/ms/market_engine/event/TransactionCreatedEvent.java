package core.ms.market_engine.event;

import core.ms.order.domain.entities.ITransaction;

import java.util.Objects;

public class TransactionCreatedEvent extends DomainEvent {
    private final ITransaction transaction;

    public TransactionCreatedEvent(ITransaction transaction, String engineId) {
        super(engineId);
        this.transaction = Objects.requireNonNull(transaction, "Transaction cannot be null");
    }

    public ITransaction getTransaction() {
        return transaction;
    }

    @Override
    public String toString() {
        return "TransactionCreatedEvent{" +
                "transactionId='" + transaction.getId() + '\'' +
                ", symbol=" + transaction.getSymbol().getCode() +
                ", eventId='" + eventId + '\'' +
                '}';
    }
}

