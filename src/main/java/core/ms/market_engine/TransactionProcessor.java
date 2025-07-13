package core.ms.market_engine;

import core.ms.order.domain.ITransaction;
import core.ms.order.domain.Transaction;
import core.ms.order_book.domain.value_object.OrderMatch;
import core.ms.shared.utils.IdGenerator;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TransactionProcessor {
    private final IdGenerator idGenerator;

    public TransactionProcessor() {
        this.idGenerator = new IdGenerator();
    }

    /**
     * Creates a transaction from an order match.
     * Note: Order status updates and quantity management are automatically
     * handled by the Order domain when the Transaction is created.
     */
    public ITransaction createTransaction(OrderMatch match) {
        Objects.requireNonNull(match, "OrderMatch cannot be null");

        if (!match.isValid()) {
            throw new IllegalArgumentException("Cannot create transaction from invalid match");
        }

        String transactionId = idGenerator.generateTransactionId();

        // Creating the transaction automatically updates both orders
        // via AbstractTransaction.updateOrdersAfterTransaction()
        return new Transaction(
                transactionId,
                match.getBuyOrder().getSymbol(),
                match.getBuyOrder(),
                match.getSellOrder(),
                match.getSuggestedPrice(),
                match.getMatchableQuantity()
        );
    }

    /**
     * Processes multiple matches into transactions.
     * Each transaction creation automatically updates the involved orders.
     */
    public List<ITransaction> processMatches(List<OrderMatch> matches) {
        return matches.stream()
                .filter(OrderMatch::isValid)
                .map(this::createTransaction)
                .collect(Collectors.toList());
    }

}