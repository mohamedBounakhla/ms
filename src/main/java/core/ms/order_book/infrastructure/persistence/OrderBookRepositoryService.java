package core.ms.order_book.infrastructure.persistence;

import core.ms.order_book.domain.entities.OrderBook;
import core.ms.order_book.domain.entities.OrderBookManager;
import core.ms.order_book.domain.ports.outbound.OrderBookRepository;
import core.ms.shared.money.Symbol;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public class OrderBookRepositoryService implements OrderBookRepository {

    // Single instance of OrderBookManager (could be replaced with actual persistence)
    private final OrderBookManager orderBookManager;

    public OrderBookRepositoryService() {
        // Create a single instance that will act as our "database"
        this.orderBookManager = new OrderBookManager();
    }

    @Override
    public OrderBook save(OrderBook orderBook) {
        // In a real implementation, this would persist to database
        // For now, we ensure it exists in the manager
        Symbol symbol = orderBook.getSymbol();

        if (!orderBookManager.getActiveSymbols().contains(symbol)) {
            orderBookManager.createOrderBook(symbol);
        }

        return orderBook;
    }

    @Override
    public Optional<OrderBook> findBySymbol(Symbol symbol) {
        try {
            OrderBook orderBook = orderBookManager.getOrderBook(symbol);
            return Optional.of(orderBook);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsBySymbol(Symbol symbol) {
        return orderBookManager.getActiveSymbols().contains(symbol);
    }

    @Override
    public boolean deleteBySymbol(Symbol symbol) {
        return orderBookManager.removeOrderBook(symbol);
    }

    @Override
    public Collection<OrderBook> findAll() {
        return orderBookManager.getAllOrderBooks();
    }

    @Override
    public long count() {
        return orderBookManager.getTotalOrderBooks();
    }

    /**
     * Provides access to market overview functionality.
     */
    public OrderBookManager getManager() {
        return orderBookManager;
    }
}