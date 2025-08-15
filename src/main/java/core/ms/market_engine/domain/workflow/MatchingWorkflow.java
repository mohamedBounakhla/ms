package core.ms.market_engine.domain.workflow;

import core.ms.market_engine.domain.events.MatchingInitiatedEvent;
import core.ms.shared.events.EventBus;
import core.ms.shared.money.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MatchingWorkflow {
    private static final Logger log = LoggerFactory.getLogger(MatchingWorkflow.class);

    private final EventBus eventBus;

    public MatchingWorkflow(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public boolean initiateMatching(String orderId, Symbol symbol) {
        log.debug("Initiating matching for order: {} on symbol: {}", orderId, symbol.getCode());

        // Publish event to trigger order book processing
        eventBus.publish(new MatchingInitiatedEvent(orderId, symbol));

        // The actual matching is handled asynchronously by OrderBook BC
        return true;
    }
}