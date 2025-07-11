package core.ms.market_engine;

import core.ms.order.domain.ITransaction;
import core.ms.order.domain.Transaction;
import core.ms.order_book.domain.value_object.OrderMatch;
import core.ms.shared.utils.IdGenerator;

import java.math.BigDecimal;
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
     */
    public ITransaction createTransaction(OrderMatch match) {
        Objects.requireNonNull(match, "OrderMatch cannot be null");

        if (!match.isValid()) {
            throw new IllegalArgumentException("Cannot create transaction from invalid match");
        }

        String transactionId = idGenerator.generateTransactionId();

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
     * Updates order statuses after a match is processed.
     */
    public void updateOrderStatuses(OrderMatch match) {
        Objects.requireNonNull(match, "OrderMatch cannot be null");

        BigDecimal executedQuantity = match.getMatchableQuantity();

        // Update buy order
        match.getBuyOrder().fillPartial(executedQuantity);
        if (match.getBuyOrder().getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0) {
            match.getBuyOrder().complete();
        }

        // Update sell order
        match.getSellOrder().fillPartial(executedQuantity);
        if (match.getSellOrder().getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0) {
            match.getSellOrder().complete();
        }
    }

    /**
     * Processes multiple matches into transactions.
     */
    public List<ITransaction> processMatches(List<OrderMatch> matches) {
        return matches.stream()
                .filter(OrderMatch::isValid)
                .map(this::createTransaction)
                .collect(Collectors.toList());
    }
}
