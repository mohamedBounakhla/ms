package core.ms.market_engine.application.handlers;

import core.ms.market_engine.domain.services.MarketOrchestrator;
import core.ms.portfolio.domain.events.publish.BuyOrderRequestedEvent;
import core.ms.portfolio.domain.events.publish.SellOrderRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventHandler {
    private static final Logger log = LoggerFactory.getLogger(OrderEventHandler.class);

    private final MarketOrchestrator orchestrator;

    public OrderEventHandler(MarketOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @EventListener
    public void handleBuyOrderRequested(BuyOrderRequestedEvent event) {
        log.info("Received buy order request from portfolio: {}", event.getPortfolioId());

        // Process the buy order through the market engine
        orchestrator.processNewOrder(
                event.getReservationId(), // Using reservation ID as order reference
                event.getPortfolioId(),
                event.getSymbol()
        );
    }

    @EventListener
    public void handleSellOrderRequested(SellOrderRequestedEvent event) {
        log.info("Received sell order request from portfolio: {}", event.getPortfolioId());

        // Process the sell order through the market engine
        orchestrator.processNewOrder(
                event.getReservationId(),
                event.getPortfolioId(),
                event.getSymbol()
        );
    }
}