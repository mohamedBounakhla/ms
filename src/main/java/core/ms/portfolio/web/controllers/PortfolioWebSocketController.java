package core.ms.portfolio.web.controllers;

import core.ms.portfolio.application.services.PortfolioApplicationService;
import core.ms.portfolio.domain.ports.inbound.PortfolioSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

@Controller
public class PortfolioWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(PortfolioWebSocketController.class);

    @Autowired
    private PortfolioApplicationService portfolioService;

    /**
     * Subscribe to portfolio updates - returns initial snapshot
     */
    @SubscribeMapping("/topic/portfolio/{portfolioId}")
    public PortfolioSnapshot subscribeToPortfolio(@DestinationVariable String portfolioId) {
        logger.info("Client subscribed to portfolio updates: {}", portfolioId);
        return portfolioService.getPortfolioSnapshot(portfolioId);
    }

    /**
     * Request immediate portfolio refresh
     */
    @MessageMapping("/portfolio.refresh")
    @SendTo("/topic/portfolio/{portfolioId}")
    public PortfolioSnapshot refreshPortfolio(String portfolioId) {
        logger.debug("Manual refresh requested for portfolio: {}", portfolioId);
        return portfolioService.getPortfolioSnapshot(portfolioId);
    }
}