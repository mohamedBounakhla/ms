package core.ms.market_engine.domain.services;

import core.ms.market_engine.domain.workflow.MatchingWorkflow;
import core.ms.market_engine.domain.workflow.OrderProcessingWorkflow;
import core.ms.market_engine.domain.workflow.SettlementWorkflow;
import core.ms.shared.money.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class MarketOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(MarketOrchestrator.class);

    private final OrderProcessingWorkflow orderProcessingWorkflow;
    private final MatchingWorkflow matchingWorkflow;
    private final SettlementWorkflow settlementWorkflow;

    public MarketOrchestrator(
            OrderProcessingWorkflow orderProcessingWorkflow,
            MatchingWorkflow matchingWorkflow,
            SettlementWorkflow settlementWorkflow) {
        this.orderProcessingWorkflow = Objects.requireNonNull(orderProcessingWorkflow);
        this.matchingWorkflow = Objects.requireNonNull(matchingWorkflow);
        this.settlementWorkflow = Objects.requireNonNull(settlementWorkflow);
    }

    /**
     * Process a new order through the complete workflow
     */
    public void processNewOrder(String orderId, String portfolioId, Symbol symbol) {
        log.info("Processing new order: {} for portfolio: {}", orderId, portfolioId);

        try {
            // Step 1: Process order (validation, reservation)
            boolean orderProcessed = orderProcessingWorkflow.processOrder(orderId, portfolioId);

            if (!orderProcessed) {
                log.warn("Order processing failed for: {}", orderId);
                return;
            }

            // Step 2: Send to order book for matching
            boolean matchingInitiated = matchingWorkflow.initiateMatching(orderId, symbol);

            if (!matchingInitiated) {
                log.warn("Matching initiation failed for order: {}", orderId);
                return;
            }

            log.info("Order {} successfully processed and sent for matching", orderId);

        } catch (Exception e) {
            log.error("Error processing order: {}", orderId, e);
            throw new MarketEngineException("Failed to process order: " + orderId, e);
        }
    }

    /**
     * Process matched orders (settlement)
     */
    public void processMatch(String buyOrderId, String sellOrderId, String matchId) {
        log.info("Processing match: {} between buy: {} and sell: {}",
                matchId, buyOrderId, sellOrderId);

        try {
            boolean settled = settlementWorkflow.settleMatch(buyOrderId, sellOrderId, matchId);

            if (settled) {
                log.info("Match {} successfully settled", matchId);
            } else {
                log.warn("Settlement failed for match: {}", matchId);
            }

        } catch (Exception e) {
            log.error("Error settling match: {}", matchId, e);
            throw new MarketEngineException("Failed to settle match: " + matchId, e);
        }
    }

    public static class MarketEngineException extends RuntimeException {
        public MarketEngineException(String message) {
            super(message);
        }

        public MarketEngineException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}