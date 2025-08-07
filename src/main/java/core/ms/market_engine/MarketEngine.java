package core.ms.market_engine;


import core.ms.market_engine.event.OrderAcceptedEvent;
import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.entities.ITransaction;
import core.ms.order_book.domain.entities.OrderBookManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
     * TODO: This needs to be reimplemented with the new matching architecture
     */
    public OrderResult processOrder(IOrder order) {
        Objects.requireNonNull(order, "Order cannot be null");

        try {
            // 1. Add order to the appropriate order book
            orderBookManager.addOrderToBook(order);

            // 2. Publish order accepted event
            eventPublisher.publishOrderAccepted(new OrderAcceptedEvent(order, engineId));

            // TODO: MATCHING MECHANISM NEEDS TO BE REIMPLEMENTED
            // The old OrderMatch class and findAllMatches() method no longer exist
            // This needs to be integrated with the new matching architecture:
            // - TwoPointerMatchingAlgorithm
            // - PriceTimePriorityMatching
            // - MatchCandidateExtractor

            /*
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
            */

            // TEMPORARY: Just return accepted without matching
            return OrderResult.accepted(order.getId());

        } catch (Exception e) {
            return OrderResult.rejected(order.getId(), "Processing failed: " + e.getMessage());
        }
    }

    /**
     * Processes a list of order matches into transactions.
     * TODO: Needs to be reimplemented with new matching architecture
     */
    private List<ITransaction> processMatches(Object matches) {  // Changed from List<OrderMatch> to Object
        // COMMENTED OUT - OrderMatch class no longer exists
        /*
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
        */

        // TEMPORARY: Return empty list
        return new ArrayList<>();
    }

    private void publishOrderExecutionEvents(Object match) {  // Changed from OrderMatch to Object
        // COMMENTED OUT - OrderMatch class no longer exists
        /*
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
        */
    }

    // Getters
    public String getEngineId() {
        return engineId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}