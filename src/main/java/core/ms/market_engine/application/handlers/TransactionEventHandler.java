package core.ms.market_engine.application.handlers;

import core.ms.portfolio.domain.events.subscribe.TransactionCreatedEvent;
import core.ms.shared.events.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventHandler {
    private static final Logger log = LoggerFactory.getLogger(TransactionEventHandler.class);

    private final EventBus eventBus;

    public TransactionEventHandler(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @EventListener
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        log.info("Transaction created: {} for order: {}",
                event.getTransactionId(), event.getOrderId());

        // Additional processing if needed
        // Could trigger portfolio updates, notifications, etc.
    }
}