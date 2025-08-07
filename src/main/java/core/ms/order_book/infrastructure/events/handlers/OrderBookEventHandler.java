package core.ms.order_book.infrastructure.events.handlers;

import core.ms.order_book.application.services.OrderBookApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Infrastructure adapter for handling domain events.
 * Acts as an adapter between the event bus and the application service.
 *
 * TO BE ACTIVATED when Market Engine is operational.
 * Currently commented out - uncomment when Market Engine publishes events.
 */
@Component
public class OrderBookEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderBookEventHandler.class);

    @Autowired
    private OrderBookApplicationService orderBookApplicationService;

    /**
     * Infrastructure adapter: Listen to external event and delegate to application service
     */
    /*
    @EventListener
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        log.debug("Transaction created event received: {}", event.getTransactionId());

        // Delegate to application service (use case)
        int removed = orderBookApplicationService.cleanupInactiveOrders();

        if (removed > 0) {
            log.debug("Cleaned up {} orders after transaction {}", removed, event.getTransactionId());
        }
    }
    */

    /*
    @EventListener
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        if (event.getNewStatus().isTerminal()) {
            log.debug("Order {} became terminal, delegating cleanup to application service", event.getOrderId());

            // Delegate to application service
            orderBookApplicationService.cleanupInactiveOrders();
        }
    }
    */
}