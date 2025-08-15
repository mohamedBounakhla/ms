package core.ms.market_engine.application.handlers;

import core.ms.market_engine.domain.services.MarketOrchestrator;
import core.ms.order_book.domain.events.OrderMatchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OrderBookEventHandler {
    private static final Logger log = LoggerFactory.getLogger(OrderBookEventHandler.class);

    private final MarketOrchestrator orchestrator;

    public OrderBookEventHandler(MarketOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @EventListener
    public void handleOrderMatched(OrderMatchedEvent event) {
        log.info("Received order matched event: {} <-> {}",
                event.getBuyOrderId(), event.getSellOrderId());

        // Process the match for settlement
        String matchId = event.getBuyOrderId() + "-" + event.getSellOrderId() + "-" +
                event.getOccurredAt().toString();

        orchestrator.processMatch(
                event.getBuyOrderId(),
                event.getSellOrderId(),
                matchId
        );
    }
}