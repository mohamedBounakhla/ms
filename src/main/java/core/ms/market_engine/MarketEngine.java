package core.ms.market_engine;

import core.ms.market_engine.event.OrderAcceptedEvent;
import core.ms.market_engine.event.OrderExecutedEvent;
import core.ms.market_engine.event.TransactionCreatedEvent;
import core.ms.order.domain.IOrder;
import core.ms.order.domain.ITransaction;
import core.ms.order_book.domain.OrderBookManager;
import core.ms.order_book.domain.value_object.OrderMatch;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MarketEngine {
    private final String engineId;
    private final OrderBookManager orderBookManager;
    private final TransactionProcessor transactionProcessor;
    private final EventPublisher eventPublisher;
    private final LocalDateTime createdAt;

    public MarketEngine(String engineId, OrderBookManager orderBookManager) {
        this.engineId = Objects.requireNonNull(engineId, "Engine ID cannot be null");
        this.orderBookManager = Objects.requireNonNull(orderBookManager, "OrderBookManager cannot be null");
        this.transactionProcessor = new TransactionProcessor();
        this.eventPublisher = new EventPublisher();
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Main business operation: processes an order through the complete workflow.
     */
    public OrderResult processOrder(IOrder order) {
        Objects.requireNonNull(order, "Order cannot be null");

        try {
            // 1. Add order to the appropriate order book
            orderBookManager.addOrderToBook(order);

            // 2. Publish order accepted event
            eventPublisher.publishOrderAccepted(new OrderAcceptedEvent(order, engineId));

            // 3. Find all possible matches across all order books
            List<OrderMatch> matches = orderBookManager.findAllMatches();

            // 4. Process any matches into transactions
            List<ITransaction> transactions = processMatches(matches);

            // 5. Extract transaction IDs for the result
            List<String> transactionIds = transactions.stream()
                    .map(ITransaction::getId)
                    .collect(Collectors.toList());

            // 6. Return appropriate result
            if (transactions.isEmpty()) {
                return OrderResult.accepted(order.getId());
            } else {
                return OrderResult.acceptedWithTransactions(order.getId(), transactionIds);
            }

        } catch (Exception e) {
            return OrderResult.rejected(order.getId(), "Processing failed: " + e.getMessage());
        }
    }

    /**
     * Processes a list of order matches into transactions.
     * Note: Order status updates and quantity management are automatically
     * handled by the Order domain when transactions are created.
     */
    private List<ITransaction> processMatches(List<OrderMatch> matches) {
        List<ITransaction> transactions = new ArrayList<>();

        for (OrderMatch match : matches) {
            if (match.isValid()) {
                try {
                    // Create transaction - this automatically updates orders via Order domain
                    ITransaction transaction = transactionProcessor.createTransaction(match);
                    transactions.add(transaction);

                    // Publish transaction created event
                    eventPublisher.publishTransactionCreated(new TransactionCreatedEvent(transaction, engineId));

                    // Publish order execution events
                    publishOrderExecutionEvents(match);

                } catch (Exception e) {
                    System.err.println("Failed to process match: " + e.getMessage());
                }
            }
        }

        return transactions;
    }

    private void publishOrderExecutionEvents(OrderMatch match) {
        eventPublisher.publishOrderExecuted(new OrderExecutedEvent(
                match.getBuyOrder().getId(),
                match.getMatchableQuantity(),
                match.getBuyOrder().getRemainingQuantity(),
                match.getSuggestedPrice(),
                engineId
        ));

        eventPublisher.publishOrderExecuted(new OrderExecutedEvent(
                match.getSellOrder().getId(),
                match.getMatchableQuantity(),
                match.getSellOrder().getRemainingQuantity(),
                match.getSuggestedPrice(),
                engineId
        ));
    }

    // Getters
    public String getEngineId() {
        return engineId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}