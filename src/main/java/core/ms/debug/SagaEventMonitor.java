package core.ms.debug;

import core.ms.portfolio.domain.events.publish.OrderRequestedEvent;
import core.ms.order.domain.events.publish.OrderCreatedEvent;
import core.ms.order.domain.events.publish.OrderCreationFailedEvent;
import core.ms.order.domain.events.publish.TransactionCreatedEvent;
import core.ms.order_book.domain.events.publish.OrderMatchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Debug component to monitor all saga events and verify the flow is working.
 * This helps diagnose if events are being published and received correctly.
 */
@Component
public class SagaEventMonitor {

    private static final Logger logger = LoggerFactory.getLogger(SagaEventMonitor.class);

    // Counters for monitoring
    private final AtomicInteger orderRequestedCount = new AtomicInteger(0);
    private final AtomicInteger orderCreatedCount = new AtomicInteger(0);
    private final AtomicInteger orderMatchedCount = new AtomicInteger(0);
    private final AtomicInteger transactionCreatedCount = new AtomicInteger(0);
    private final AtomicInteger orderFailedCount = new AtomicInteger(0);

    @EventListener
    public void onOrderRequested(OrderRequestedEvent event) {
        int count = orderRequestedCount.incrementAndGet();
        logger.warn("🚀 [SAGA MONITOR] #{} OrderRequestedEvent - Correlation: {}, Portfolio: {}, Type: {}, Symbol: {}, Quantity: {}",
                count, event.getCorrelationId(), event.getPortfolioId(),
                event.getOrderType(), event.getSymbol().getCode(), event.getQuantity());
    }

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        int count = orderCreatedCount.incrementAndGet();
        logger.warn("✅ [SAGA MONITOR] #{} OrderCreatedEvent - Correlation: {}, Order: {}, Portfolio: {}, Reservation: {}",
                count, event.getCorrelationId(), event.getOrderId(),
                event.getPortfolioId(), event.getReservationId());
    }

    @EventListener
    public void onOrderMatched(OrderMatchedEvent event) {
        int count = orderMatchedCount.incrementAndGet();
        logger.warn("🔄 [SAGA MONITOR] #{} OrderMatchedEvent - Correlation: {}, Buy: {}, Sell: {}, Quantity: {}",
                count, event.getCorrelationId(), event.getBuyOrderId(),
                event.getSellOrderId(), event.getMatchedQuantity());
    }

    @EventListener
    public void onTransactionCreated(TransactionCreatedEvent event) {
        int count = transactionCreatedCount.incrementAndGet();
        logger.warn("💰 [SAGA MONITOR] #{} TransactionCreatedEvent - Correlation: {}, TX: {}, Buy: {}, Sell: {}",
                count, event.getCorrelationId(), event.getTransactionId(),
                event.getBuyOrderId(), event.getSellOrderId());
    }

    @EventListener
    public void onOrderCreationFailed(OrderCreationFailedEvent event) {
        int count = orderFailedCount.incrementAndGet();
        logger.error("❌ [SAGA MONITOR] #{} OrderCreationFailedEvent - Correlation: {}, Reservation: {}, Reason: {}",
                count, event.getCorrelationId(), event.getReservationId(), event.getReason());
    }

    /**
     * Get current statistics for debugging
     */
    public void printStatistics() {
        logger.warn("📊 [SAGA STATISTICS] Requested: {}, Created: {}, Matched: {}, Transactions: {}, Failed: {}",
                orderRequestedCount.get(), orderCreatedCount.get(),
                orderMatchedCount.get(), transactionCreatedCount.get(),
                orderFailedCount.get());
    }

    /**
     * Reset counters for testing
     */
    public void resetCounters() {
        orderRequestedCount.set(0);
        orderCreatedCount.set(0);
        orderMatchedCount.set(0);
        transactionCreatedCount.set(0);
        orderFailedCount.set(0);
        logger.warn("🔄 [SAGA MONITOR] Counters reset");
    }

    // Getters for testing
    public int getOrderRequestedCount() { return orderRequestedCount.get(); }
    public int getOrderCreatedCount() { return orderCreatedCount.get(); }
    public int getOrderMatchedCount() { return orderMatchedCount.get(); }
    public int getTransactionCreatedCount() { return transactionCreatedCount.get(); }
    public int getOrderFailedCount() { return orderFailedCount.get(); }
}