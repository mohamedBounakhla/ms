package core.ms.market_engine.application.services;

import core.ms.market_engine.application.dto.MarketStatusDTO;
import core.ms.market_engine.application.dto.OrderFlowDTO;
import core.ms.market_engine.domain.services.MarketOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MarketEngineApplicationService {
    private static final Logger log = LoggerFactory.getLogger(MarketEngineApplicationService.class);

    private final MarketOrchestrator orchestrator;
    private final AtomicLong processedOrders = new AtomicLong(0);
    private final AtomicLong processedMatches = new AtomicLong(0);

    public MarketEngineApplicationService(MarketOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Get current market engine status
     */
    public MarketStatusDTO getMarketStatus() {
        return new MarketStatusDTO(
                "OPERATIONAL",
                processedOrders.get(),
                processedMatches.get(),
                LocalDateTime.now()
        );
    }

    /**
     * Get order flow statistics
     */
    public OrderFlowDTO getOrderFlow() {
        // This would aggregate real-time data
        return new OrderFlowDTO(
                processedOrders.get(),
                processedMatches.get(),
                0L, // pending orders
                LocalDateTime.now()
        );
    }

    /**
     * Manual order processing (for admin/testing)
     */
    public void manualProcessOrder(String orderId, String portfolioId, String symbolCode) {
        log.info("Manual order processing initiated for: {}", orderId);

        // Convert symbol code to Symbol object
        var symbol = createSymbol(symbolCode);

        orchestrator.processNewOrder(orderId, portfolioId, symbol);
        processedOrders.incrementAndGet();
    }

    private core.ms.shared.money.Symbol createSymbol(String symbolCode) {
        return switch (symbolCode.toUpperCase()) {
            case "BTC" -> core.ms.shared.money.Symbol.btcUsd();
            case "ETH" -> core.ms.shared.money.Symbol.ethUsd();
            case "EURUSD" -> core.ms.shared.money.Symbol.eurUsd();
            case "GBPUSD" -> core.ms.shared.money.Symbol.gbpUsd();
            default -> throw new IllegalArgumentException("Unsupported symbol: " + symbolCode);
        };
    }
}