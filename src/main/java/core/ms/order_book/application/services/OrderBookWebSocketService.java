package core.ms.order_book.application.services;

import core.ms.order_book.web.mappers.OrderBookWebMapper;
import core.ms.shared.money.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderBookWebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(OrderBookWebSocketService.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private OrderBookApplicationService orderBookService;

    @Autowired
    private OrderBookWebMapper webMapper;

    /**
     * Broadcasts order book update to all subscribed clients.
     */
    public void broadcastOrderBookUpdate(String symbolCode) {
        try {
            Symbol symbol = webMapper.createSymbol(symbolCode);

            // Get market depth
            var marketDepth = orderBookService.getMarketDepth(symbol, 25);
            var depthDTO = webMapper.toDTO(marketDepth);

            // Broadcast to order book topic
            messagingTemplate.convertAndSend(
                    "/topic/orderbook/" + symbolCode,
                    depthDTO
            );

            // Also broadcast ticker update
            var ticker = orderBookService.getOrderBookTicker(symbol);
            messagingTemplate.convertAndSend(
                    "/topic/ticker/" + symbolCode,
                    ticker
            );

            logger.debug("📡 Broadcasted order book update for symbol: {}", symbolCode);

        } catch (Exception e) {
            logger.error("Failed to broadcast order book update for symbol: {}", symbolCode, e);
        }
    }

    /**
     * Broadcasts ticker update only.
     */
    public void broadcastTickerUpdate(String symbolCode) {
        try {
            Symbol symbol = webMapper.createSymbol(symbolCode);

            var ticker = orderBookService.getOrderBookTicker(symbol);
            messagingTemplate.convertAndSend(
                    "/topic/ticker/" + symbolCode,
                    ticker
            );

            logger.debug("📡 Broadcasted ticker update for symbol: {}", symbolCode);

        } catch (Exception e) {
            logger.error("Failed to broadcast ticker update for symbol: {}", symbolCode, e);
        }
    }

    /**
     * Sends a direct message to a specific user session.
     */
    public void sendToUser(String sessionId, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(sessionId, destination, payload);
    }
}