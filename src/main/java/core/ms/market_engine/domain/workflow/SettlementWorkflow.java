package core.ms.market_engine.domain.workflow;

import core.ms.market_engine.domain.events.SettlementCompletedEvent;
import core.ms.market_engine.domain.events.SettlementInitiatedEvent;
import core.ms.shared.events.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SettlementWorkflow {
    private static final Logger log = LoggerFactory.getLogger(SettlementWorkflow.class);

    private final EventBus eventBus;

    public SettlementWorkflow(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public boolean settleMatch(String buyOrderId, String sellOrderId, String matchId) {
        log.debug("Starting settlement for match: {}", matchId);

        // Step 1: Initiate settlement
        eventBus.publish(new SettlementInitiatedEvent(buyOrderId, sellOrderId, matchId));

        // Step 2: Execute reservations (handled by Portfolio BC via events)

        // Step 3: Create transaction (handled by Order BC via events)

        // Step 4: Complete settlement
        eventBus.publish(new SettlementCompletedEvent(matchId));

        return true;
    }
}