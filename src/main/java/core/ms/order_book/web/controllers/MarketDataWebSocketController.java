package core.ms.order_book.web.controllers;

import core.ms.order_book.application.dto.query.MarketDepthDTO;
import core.ms.order_book.application.dto.query.OrderBookTickerDTO;
import core.ms.order_book.application.services.OrderBookApplicationService;
import core.ms.order_book.web.mappers.OrderBookWebMapper;
import core.ms.shared.money.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

@Controller
public class MarketDataWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataWebSocketController.class);

    @Autowired
    private OrderBookApplicationService orderBookService;

    @Autowired
    private OrderBookWebMapper webMapper;

    /**
     * Subscribe to order book updates for a specific symbol.
     * Updates are automatically pushed when order book changes.
     */
    @SubscribeMapping("/topic/orderbook/{symbol}")
    public MarketDepthDTO subscribeToOrderBook(@DestinationVariable String symbol) {
        logger.info("Client subscribed to order book: {}", symbol);

        try {
            var domainSymbol = Symbol.createFromCode(symbol);
            var marketDepth = orderBookService.getMarketDepth(domainSymbol, 10);
            return webMapper.toDTO(marketDepth);
        } catch (Exception e) {
            logger.error("Failed to get order book for subscription", e);
            return null;
        }
    }

    /**
     * Subscribe to ticker updates (best bid/ask) for a specific symbol.
     */
    @SubscribeMapping("/topic/ticker/{symbol}")
    public OrderBookTickerDTO subscribeToTicker(@DestinationVariable String symbol) {
        logger.info("Client subscribed to ticker: {}", symbol);

        try {
            var domainSymbol = Symbol.createFromCode(symbol);
            return orderBookService.getOrderBookTicker(domainSymbol);
        } catch (Exception e) {
            logger.error("Failed to get ticker for subscription", e);
            return null;
        }
    }

    /**
     * Request snapshot of order book (client-initiated).
     */
    @MessageMapping("/orderbook/{symbol}")
    @SendTo("/topic/orderbook/{symbol}")
    public MarketDepthDTO getOrderBookSnapshot(@DestinationVariable String symbol) {
        logger.debug("Snapshot request for symbol: {}", symbol);

        try {
            var domainSymbol = Symbol.createFromCode(symbol);
            var marketDepth = orderBookService.getMarketDepth(domainSymbol, 25);
            return webMapper.toDTO(marketDepth);
        } catch (Exception e) {
            logger.error("Failed to get order book snapshot", e);
            return null;
        }
    }
}