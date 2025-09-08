package core.ms.portfolio.application.services;

import core.ms.portfolio.domain.Portfolio;
import core.ms.portfolio.domain.events.publish.PortfolioUpdateEvent;
import core.ms.portfolio.domain.events.publish.PortfolioUpdateEvent.UpdateType;
import core.ms.portfolio.domain.ports.inbound.PortfolioSnapshot;
import core.ms.portfolio.domain.ports.outbound.PortfolioRepository;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class PortfolioUpdateBroadcaster {

    private static final Logger logger = LoggerFactory.getLogger(PortfolioUpdateBroadcaster.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private PortfolioRepository portfolioRepository;  // Use repository directly, not the service

    /**
     * Broadcast portfolio update after cash change
     */
    public void broadcastCashUpdate(String portfolioId) {
        try {
            Portfolio portfolio = portfolioRepository.findById(portfolioId).orElse(null);
            if (portfolio == null) {
                logger.warn("Portfolio not found for broadcast: {}", portfolioId);
                return;
            }

            Map<Currency, BigDecimal> cashBalances = new HashMap<>();
            for (Currency currency : Currency.values()) {
                Money balance = portfolio.getTotalCash(currency);
                if (balance.isPositive()) {
                    cashBalances.put(currency, balance.getAmount());
                }
            }

            PortfolioUpdateEvent event = new PortfolioUpdateEvent(
                    portfolioId,
                    UpdateType.CASH_CHANGE,
                    cashBalances,
                    null
            );

            messagingTemplate.convertAndSend("/topic/portfolio/" + portfolioId, event);
            logger.debug("Broadcasted cash update for portfolio: {}", portfolioId);

        } catch (Exception e) {
            logger.error("Failed to broadcast cash update for portfolio: {}", portfolioId, e);
        }
    }

    /**
     * Broadcast portfolio update after position change
     */
    public void broadcastPositionUpdate(String portfolioId) {
        try {
            Portfolio portfolio = portfolioRepository.findById(portfolioId).orElse(null);
            if (portfolio == null) {
                logger.warn("Portfolio not found for broadcast: {}", portfolioId);
                return;
            }

            Map<String, BigDecimal> positions = new HashMap<>();
            for (Symbol symbol : portfolio.getPositionSymbols()) {
                BigDecimal quantity = portfolio.getTotalAssets(symbol);
                if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                    positions.put(symbol.getCode(), quantity);
                }
            }

            PortfolioUpdateEvent event = new PortfolioUpdateEvent(
                    portfolioId,
                    UpdateType.POSITION_CHANGE,
                    null,
                    positions
            );

            messagingTemplate.convertAndSend("/topic/portfolio/" + portfolioId, event);
            logger.debug("Broadcasted position update for portfolio: {}", portfolioId);

        } catch (Exception e) {
            logger.error("Failed to broadcast position update for portfolio: {}", portfolioId, e);
        }
    }

    /**
     * Broadcast complete portfolio update with both cash and positions
     */
    public void broadcastFullUpdate(String portfolioId) {
        try {
            Portfolio portfolio = portfolioRepository.findById(portfolioId).orElse(null);
            if (portfolio == null) {
                logger.warn("Portfolio not found for broadcast: {}", portfolioId);
                return;
            }

            // Gather cash balances
            Map<Currency, BigDecimal> cashBalances = new HashMap<>();
            for (Currency currency : Currency.values()) {
                Money balance = portfolio.getTotalCash(currency);
                if (balance.isPositive()) {
                    cashBalances.put(currency, balance.getAmount());
                }
            }

            // Gather positions
            Map<String, BigDecimal> positions = new HashMap<>();
            for (Symbol symbol : portfolio.getPositionSymbols()) {
                BigDecimal quantity = portfolio.getTotalAssets(symbol);
                if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                    positions.put(symbol.getCode(), quantity);
                }
            }

            PortfolioUpdateEvent event = new PortfolioUpdateEvent(
                    portfolioId,
                    UpdateType.FULL_UPDATE,
                    cashBalances,
                    positions
            );

            messagingTemplate.convertAndSend("/topic/portfolio/" + portfolioId, event);
            logger.debug("Broadcasted full update for portfolio: {}", portfolioId);

        } catch (Exception e) {
            logger.error("Failed to broadcast full update for portfolio: {}", portfolioId, e);
        }
    }
}