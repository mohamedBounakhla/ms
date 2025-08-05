package core.ms.order_book.domain.ports.outbound;

import core.ms.order_book.domain.entities.OrderBook;
import core.ms.shared.money.Symbol;

import java.util.Collection;
import java.util.Optional;

public interface OrderBookRepository {

    /**
     * Saves or updates an order book.
     */
    OrderBook save(OrderBook orderBook);

    /**
     * Finds an order book by symbol.
     */
    Optional<OrderBook> findBySymbol(Symbol symbol);

    /**
     * Checks if an order book exists for a symbol.
     */
    boolean existsBySymbol(Symbol symbol);

    /**
     * Deletes an order book by symbol.
     */
    boolean deleteBySymbol(Symbol symbol);

    /**
     * Gets all order books.
     */
    Collection<OrderBook> findAll();

    /**
     * Gets total count of order books.
     */
    long count();
}