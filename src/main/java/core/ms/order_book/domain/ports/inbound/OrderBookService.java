package core.ms.order_book.domain.ports.inbound;

import core.ms.order.domain.entities.IOrder;
import core.ms.order_book.application.dto.query.OrderBookStatisticsDTO;
import core.ms.order_book.application.dto.query.OrderBookSummaryDTO;
import core.ms.order_book.application.dto.query.OrderBookTickerDTO;
import core.ms.order_book.domain.events.publish.OrderMatchedEvent;
import core.ms.order_book.domain.value_object.MarketDepth;
import core.ms.order_book.domain.value_object.MarketOverview;
import core.ms.shared.money.Symbol;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface OrderBookService {

    // ===== ORDER BOOK MANAGEMENT =====

    /**
     * Adds an order to the appropriate order book and processes any matches.
     * @param order The order to add
     * @return Result containing match information
     */
    OrderBookOperationResult addOrderToBook(IOrder order);

    /**
     * Removes an order from the order book.
     * @param orderId The order ID to remove
     * @param symbol The symbol of the order book
     * @return Result of the removal operation
     */
    OrderBookOperationResult removeOrderFromBook(String orderId, Symbol symbol);

    /**
     * Processes all pending matches for a specific symbol.
     * @param symbol The symbol to process
     * @return List of match events that were processed
     */
    List<OrderMatchedEvent> processPendingMatches(Symbol symbol);

    /**
     * Processes all pending matches across all order books.
     * @return List of all match events that were processed
     */
    List<OrderMatchedEvent> processAllPendingMatches();

    // ===== MARKET DATA QUERIES =====

    /**
     * Gets market depth for a symbol.
     * @param symbol The symbol to query
     * @param levels Number of price levels to include
     * @return Market depth data
     */
    MarketDepth getMarketDepth(Symbol symbol, int levels);

    /**
     * Gets overview of all markets.
     * @return Market overview data
     */
    MarketOverview getMarketOverview();

    // ===== ORDER BOOK LIFECYCLE =====

    /**
     * Creates a new order book for a symbol.
     * @param symbol The symbol for the new order book
     * @return Result of the creation
     */
    OrderBookOperationResult createOrderBook(Symbol symbol);

    /**
     * Removes an order book.
     * @param symbol The symbol of the order book to remove
     * @return Result of the removal
     */
    OrderBookOperationResult removeOrderBook(Symbol symbol);

    /**
     * Cleans up inactive orders from all order books.
     * @return Number of orders removed
     */
    int cleanupInactiveOrders();
    /**
     * Gets ticker information (best bid/ask) for a symbol.
     */
    OrderBookTickerDTO getOrderBookTicker(Symbol symbol);

    /**
     * Gets detailed statistics for an order book.
     */
    OrderBookStatisticsDTO getOrderBookStatistics(Symbol symbol);

    /**
     * Gets a summary of the order book.
     */
    OrderBookSummaryDTO getOrderBookSummary(Symbol symbol);

    /**
     * Gets all active trading symbols.
     */
    Set<Symbol> getActiveSymbols();

    /**
     * Checks if an order book is active for a symbol.
     */
    boolean isOrderBookActive(Symbol symbol);

    /**
     * Gets the total number of orders across all books.
     */
    int getTotalOrderCount();

    /**
     * Gets statistics for all markets.
     */
    Map<String, OrderBookStatisticsDTO> getAllMarketStatistics();
}