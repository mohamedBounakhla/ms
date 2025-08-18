package core.ms.order.application.event_handlers;

import core.ms.order.domain.events.publish.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class OrderEventLogger {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventLogger.class);

    @EventListener
    @Async
    public void handleOrderCreated(OrderCreatedEvent event) {
        logger.info("Order Created - ID: {}, Type: {}, Symbol: {}, Quantity: {}, Price: {}, Portfolio: {}",
                event.getOrderId(),
                event.getOrderType(),
                event.getSymbol().getFullSymbol(),
                event.getQuantity(),
                event.getPrice(),
                event.getPortfolioId()
        );
    }

    @EventListener
    @Async
    public void handleOrderCancelled(OrderCancelledEvent event) {
        logger.info("Order Cancelled - ID: {}, Portfolio: {}, Cancelled Qty: {}, Reason: {}",
                event.getOrderId(),
                event.getPortfolioId(),
                event.getCancelledQuantity(),
                event.getReason()
        );
    }

    @EventListener
    @Async
    public void handleOrderUpdated(OrderUpdatedEvent event) {
        logger.info("Order Updated - ID: {}, Type: {}, Old Price: {}, New Price: {}",
                event.getOrderId(),
                event.getUpdateType(),
                event.getOldPrice(),
                event.getNewPrice()
        );
    }

    @EventListener
    @Async
    public void handleOrderPartiallyFilled(OrderPartiallyFilledEvent event) {
        logger.info("Order Partially Filled - ID: {}, Filled: {}, Total Filled: {}, Remaining: {}",
                event.getOrderId(),
                event.getFilledQuantity(),
                event.getTotalFilledQuantity(),
                event.getRemainingQuantity()
        );
    }

    @EventListener
    @Async
    public void handleOrderFilled(OrderFilledEvent event) {
        logger.info("Order Filled - ID: {}, Type: {}, Quantity: {}, Avg Price: {}",
                event.getOrderId(),
                event.getOrderType(),
                event.getFilledQuantity(),
                event.getAverageExecutionPrice()
        );
    }

    @EventListener
    @Async
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        logger.info("Transaction Created - ID: {}, Buy Order: {}, Sell Order: {}, Quantity: {}, Price: {}, Total: {}",
                event.getTransactionId(),
                event.getBuyOrderId(),
                event.getSellOrderId(),
                event.getExecutedQuantity(),
                event.getExecutionPrice(),
                event.getTotalValue()
        );
    }
}