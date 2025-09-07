package core.ms.order_book.infrastructure.persistence;

import core.ms.order_book.domain.entities.OrderBook;
import core.ms.order_book.domain.entities.OrderBookManager;
import core.ms.order_book.domain.ports.outbound.OrderBookRepository;
import core.ms.order_book.infrastructure.persistence.DAO.OrderBookDAO;
import core.ms.order_book.infrastructure.persistence.entities.OrderBookEntity;
import core.ms.shared.money.Symbol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

@Repository
@Primary  // This will be the primary implementation if both exist
@Transactional
public class OrderBookRepositoryJpaImpl implements OrderBookRepository {

    private final OrderBookManager orderBookManager;

    @Autowired
    private OrderBookDAO orderBookDAO;

    public OrderBookRepositoryJpaImpl() {
        this.orderBookManager = new OrderBookManager();
    }

    @Override
    public OrderBook save(OrderBook orderBook) {
        Symbol symbol = orderBook.getSymbol();

        // Don't update DB metadata on every save - only periodically
        boolean shouldUpdateMetadata = shouldUpdateDatabaseMetadata();

        if (shouldUpdateMetadata) {
            // Only update metadata occasionally
            OrderBookEntity entity = orderBookDAO.findBySymbolCode(symbol.getCode())
                    .orElse(new OrderBookEntity(
                            symbol.getCode(),
                            symbol.getName(),
                            symbol.getBaseCurrency().name(),
                            symbol.getQuoteCurrency().name()
                    ));

            entity.setLastActivity(Instant.now());
            entity.setTotalOrders(orderBook.getOrderCount());
            entity.setTotalVolume(orderBook.getTotalBidVolume().add(orderBook.getTotalAskVolume()));

            orderBookDAO.save(entity);
        }

        // Keep in-memory state updated
        if (!orderBookManager.getActiveSymbols().contains(symbol)) {
            orderBookManager.createOrderBook(symbol);
        }

        return orderBook;
    }

    private volatile long lastMetadataUpdate = 0;
    private static final long METADATA_UPDATE_INTERVAL = 5000; // 5 seconds

    private boolean shouldUpdateDatabaseMetadata() {
        long now = System.currentTimeMillis();
        if (now - lastMetadataUpdate > METADATA_UPDATE_INTERVAL) {
            lastMetadataUpdate = now;
            return true;
        }
        return false;
    }

    @Override
    public Optional<OrderBook> findBySymbol(Symbol symbol) {
        // Check if exists in database
        Optional<OrderBookEntity> entityOpt = orderBookDAO.findBySymbolCode(symbol.getCode());

        if (entityOpt.isEmpty() || !entityOpt.get().isActive()) {
            return Optional.empty();
        }

        // Get or create from manager
        try {
            OrderBook orderBook = orderBookManager.getOrderBook(symbol);
            return Optional.of(orderBook);
        } catch (Exception e) {
            // If not in manager but in DB, create it
            orderBookManager.createOrderBook(symbol);
            return Optional.of(orderBookManager.getOrderBook(symbol));
        }
    }

    @Override
    public boolean existsBySymbol(Symbol symbol) {
        return orderBookDAO.existsBySymbolCode(symbol.getCode()) &&
                orderBookManager.getActiveSymbols().contains(symbol);
    }

    @Override
    public boolean deleteBySymbol(Symbol symbol) {
        // Mark as inactive in database
        Optional<OrderBookEntity> entityOpt = orderBookDAO.findBySymbolCode(symbol.getCode());
        if (entityOpt.isPresent()) {
            OrderBookEntity entity = entityOpt.get();
            entity.setActive(false);
            orderBookDAO.save(entity);
        }

        // Remove from manager
        return orderBookManager.removeOrderBook(symbol);
    }

    @Override
    public Collection<OrderBook> findAll() {
        // Return only active order books from manager
        return orderBookManager.getAllOrderBooks();
    }

    @Override
    public long count() {
        return orderBookDAO.countActive();
    }

    /**
     * Initializes order books from database on startup
     */
    @jakarta.annotation.PostConstruct
    public void initializeFromDatabase() {
        // Load all active order books from database
        orderBookDAO.findByActiveTrue().forEach(entity -> {
            Symbol symbol = Symbol.createFromCode(entity.getSymbolCode());
            if (!orderBookManager.getActiveSymbols().contains(symbol)) {
                orderBookManager.createOrderBook(symbol);
            }
        });
    }

   

    /**
     * Provides access to the manager for market overview functionality
     */
    public OrderBookManager getManager() {
        return orderBookManager;
    }
}