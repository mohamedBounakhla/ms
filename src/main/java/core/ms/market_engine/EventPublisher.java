package core.ms.market_engine;

import core.ms.market_engine.event.OrderAcceptedEvent;
import core.ms.market_engine.event.OrderExecutedEvent;
import core.ms.market_engine.event.TransactionCreatedEvent;
import core.ms.order.domain.ITransaction;

import java.time.LocalDateTime;
import java.util.Objects;

public class EventPublisher {

    /**
     * Publishes order accepted event.
     */
    public void publishOrderAccepted(OrderAcceptedEvent event) {
        Objects.requireNonNull(event, "Event cannot be null");
        logEvent("ORDER_ACCEPTED",
                "Order " + event.getOrder().getId() + " accepted for symbol " + event.getOrder().getSymbol().getCode());
    }

    /**
     * Publishes transaction created event.
     */
    public void publishTransactionCreated(TransactionCreatedEvent event) {
        Objects.requireNonNull(event, "Event cannot be null");
        ITransaction tx = event.getTransaction();
        logEvent("TRANSACTION_CREATED",
                "Transaction " + tx.getId() + " created: " + tx.getQuantity() + " " +
                        tx.getSymbol().getCode() + " @ " + tx.getPrice().toPlainString());
    }

    /**
     * Publishes order executed event.
     */
    public void publishOrderExecuted(OrderExecutedEvent event) {
        Objects.requireNonNull(event, "Event cannot be null");
        logEvent("ORDER_EXECUTED",
                "Order " + event.getOrderId() + " executed: " + event.getExecutedQuantity() +
                        " @ " + event.getExecutionPrice().toPlainString() +
                        " (remaining: " + event.getRemainingQuantity() + ")");
    }

    /**
     * Simple logging mechanism.
     */
    private void logEvent(String eventType, String message) {
        System.out.println("[" + LocalDateTime.now() + "] " + eventType + ": " + message);
    }
}