package core.ms.order_book.application.services;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order_book.application.dto.query.OrderBookStatisticsDTO;
import core.ms.order_book.application.dto.query.OrderBookSummaryDTO;
import core.ms.order_book.application.dto.query.OrderBookTickerDTO;
import core.ms.order_book.domain.entities.OrderBook;
import core.ms.order_book.domain.events.publish.OrderMatchedEvent;
import core.ms.order_book.domain.ports.inbound.OrderBookOperationResult;
import core.ms.order_book.domain.ports.inbound.OrderBookService;
import core.ms.order_book.domain.ports.outbound.OrderBookRepository;
import core.ms.order_book.domain.ports.outbound.OrderMatchEventPublisher;
import core.ms.order_book.domain.value_object.MarketDepth;
import core.ms.order_book.domain.value_object.MarketOverview;
import core.ms.order_book.infrastructure.persistence.OrderBookRepositoryJpaImpl;
import core.ms.shared.events.EventContext;
import core.ms.shared.money.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Service
public class OrderBookApplicationService implements OrderBookService {

    private static final Logger logger = LoggerFactory.getLogger(OrderBookApplicationService.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long LOCK_TIMEOUT_SECONDS = 5;

    // Symbol-level locks for order book operations
    private final Map<Symbol, ReadWriteLock> symbolLocks = new ConcurrentHashMap<>();

    // Track pending matches per symbol to avoid duplicate processing
    private final Map<Symbol, Set<String>> processingMatches = new ConcurrentHashMap<>();

    private final OrderBookRepository orderBookRepository;
    private final OrderMatchEventPublisher eventPublisher;

    @Autowired
    public OrderBookApplicationService(
            OrderBookRepository orderBookRepository,
            OrderMatchEventPublisher eventPublisher) {
        this.orderBookRepository = Objects.requireNonNull(orderBookRepository);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    /**
     * Adds order to book with thread-safe matching.
     * Uses write lock to ensure atomic order addition and matching.
     */
    @Override
    public OrderBookOperationResult addOrderToBook(IOrder order) {
        Objects.requireNonNull(order, "Order cannot be null");

        Symbol symbol = order.getSymbol();
        String correlationId = EventContext.getCurrentCorrelationId();
        var lock = getWriteLock(symbol);

        try {
            // Acquire write lock with timeout
            if (!lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                logger.warn("Failed to acquire write lock for symbol: {} within timeout", symbol.getCode());
                return OrderBookOperationResult.builder()
                        .success(false)
                        .message("Order book is busy, please try again")
                        .orderId(order.getId())
                        .build();
            }

            try {
                logger.info("📚 Adding order {} to book for symbol: {}",
                        order.getId(), symbol.getCode());

                // Get or create order book
                OrderBook orderBook = getOrCreateOrderBookInternal(symbol);

                // Log state before
                logOrderBookState(orderBook, "BEFORE ADD");

                // Add order based on type
                if (order instanceof IBuyOrder buyOrder) {
                    orderBook.addOrder(buyOrder);
                } else if (order instanceof ISellOrder sellOrder) {
                    orderBook.addOrder(sellOrder);
                } else {
                    throw new IllegalArgumentException("Unknown order type: " + order.getClass());
                }

                // Log state after
                logOrderBookState(orderBook, "AFTER ADD");

                // Save order book state
                orderBookRepository.save(orderBook);

                // Process matches if any
                List<OrderMatchedEvent> matchEvents = processMatchesInternal(orderBook, correlationId);

                if (!matchEvents.isEmpty()) {
                    logger.info("🎯 Found {} matches for order {}",
                            matchEvents.size(), order.getId());

                    // Publish match events asynchronously
                    publishMatchEventsAsync(matchEvents);
                } else {
                    logger.info("❌ No matches found for order {}", order.getId());
                }

                return OrderBookOperationResult.builder()
                        .success(true)
                        .message("Order added to book")
                        .orderId(order.getId())
                        .build();

            } finally {
                lock.unlock();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while waiting for lock on symbol: {}", symbol.getCode());
            return OrderBookOperationResult.builder()
                    .success(false)
                    .message("Operation interrupted")
                    .orderId(order.getId())
                    .build();
        } catch (Exception e) {
            logger.error("Failed to add order to book", e);
            return OrderBookOperationResult.builder()
                    .success(false)
                    .message("Failed to add order: " + e.getMessage())
                    .orderId(order.getId())
                    .build();
        }
    }

    /**
     * Removes order from book with thread safety.
     */
    @Override
    public OrderBookOperationResult removeOrderFromBook(String orderId, Symbol symbol) {
        var lock = getWriteLock(symbol);

        try {
            if (!lock.tryLock(1, TimeUnit.SECONDS)) { // Shorter timeout
                return OrderBookOperationResult.builder()
                        .success(false)
                        .message("Order book is busy")
                        .orderId(orderId)
                        .build();
            }

            try {
                // Use in-memory operation only
                OrderBook orderBook = getOrderBookFromMemory(symbol);
                if (orderBook == null) {
                    return OrderBookOperationResult.builder()
                            .success(false)
                            .message("Order book not found")
                            .orderId(orderId)
                            .build();
                }

                boolean removed = orderBook.removeOrderById(orderId);

                if (removed) {
                    logger.info("✅ Order {} removed from book", orderId);
                    return OrderBookOperationResult.builder()
                            .success(true)
                            .message("Order removed")
                            .orderId(orderId)
                            .build();
                }

                return OrderBookOperationResult.builder()
                        .success(false)
                        .message("Order not found in book")
                        .orderId(orderId)
                        .build();

            } finally {
                lock.unlock();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return OrderBookOperationResult.builder()
                    .success(false)
                    .message("Operation interrupted")
                    .orderId(orderId)
                    .build();
        }
    }
    private OrderBook getOrderBookFromMemory(Symbol symbol) {
        if (orderBookRepository instanceof OrderBookRepositoryJpaImpl repo) {
            return repo.getManager().getOrderBook(symbol);
        }
        throw new UnsupportedOperationException("Memory access not available");
    }
    /**
     * Processes pending matches for a symbol.
     * Uses separate transaction to avoid blocking order operations.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED)
    public void processPendingMatches(Symbol symbol) {
        Objects.requireNonNull(symbol, "Symbol cannot be null");

        var lock = getReadLock(symbol);

        try {
            if (!lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                logger.warn("Could not acquire read lock for symbol: {}", symbol.getCode());
                return;
            }

            try {
                Optional<OrderBook> orderBookOpt = orderBookRepository.findBySymbol(symbol);
                if (orderBookOpt.isEmpty()) {
                    return;
                }

                OrderBook orderBook = orderBookOpt.get();
                String correlationId = EventContext.getCurrentCorrelationId();

                List<OrderMatchedEvent> matchEvents = processMatchesInternal(orderBook, correlationId);

                if (!matchEvents.isEmpty()) {
                    logger.info("📤 Publishing {} pending matches for symbol {}",
                            matchEvents.size(), symbol.getCode());
                    publishMatchEventsAsync(matchEvents);
                }

            } finally {
                lock.unlock();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while processing matches for symbol: {}", symbol.getCode());
        } catch (Exception e) {
            logger.error("Error processing matches for symbol: {}", symbol.getCode(), e);
        }
    }

    /**
     * Processes all pending matches across all books.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED)
    public void processAllPendingMatches() {
        logger.info("🔄 Processing all pending matches");

        Collection<OrderBook> orderBooks = orderBookRepository.findAll();
        int totalMatches = 0;

        for (OrderBook orderBook : orderBooks) {
            try {
                Symbol symbol = orderBook.getSymbol();
                var lock = getReadLock(symbol);

                if (lock.tryLock(1, TimeUnit.SECONDS)) {
                    try {
                        String correlationId = EventContext.getCurrentCorrelationId();
                        List<OrderMatchedEvent> events = processMatchesInternal(orderBook, correlationId);

                        if (!events.isEmpty()) {
                            totalMatches += events.size();
                            publishMatchEventsAsync(events);
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (Exception e) {
                logger.error("Error processing matches for book: {}",
                        orderBook.getSymbol().getCode(), e);
            }
        }

        if (totalMatches > 0) {
            logger.info("✅ Published {} total matches", totalMatches);
        }
    }

    /**
     * Internal method to process matches with deduplication.
     */
    private List<OrderMatchedEvent> processMatchesInternal(OrderBook orderBook, String correlationId) {
        if (!orderBook.hasRecentMatches()) {
            return Collections.emptyList();
        }

        List<OrderMatchedEvent> rawEvents = orderBook.consumeRecentMatchEvents();
        Symbol symbol = orderBook.getSymbol();

        // Get or create processing set for this symbol
        Set<String> processing = processingMatches.computeIfAbsent(
                symbol, k -> ConcurrentHashMap.newKeySet()
        );

        // Filter out already processing matches
        List<OrderMatchedEvent> uniqueEvents = new ArrayList<>();
        for (OrderMatchedEvent event : rawEvents) {
            String matchKey = createMatchKey(event);
            if (processing.add(matchKey)) {
                uniqueEvents.add(event);
            } else {
                logger.debug("Skipping duplicate match: {}", matchKey);
            }
        }

        // Schedule cleanup of processing set
        scheduleMatchCleanup(symbol, uniqueEvents);

        return uniqueEvents;
    }

    /**
     * Publishes match events asynchronously.
     */
    private void publishMatchEventsAsync(List<OrderMatchedEvent> events) {
        // Use CompletableFuture for true async
        CompletableFuture.runAsync(() -> {
            try {
                eventPublisher.publishOrderMatchedEvents(events);
            } catch (Exception e) {
                logger.error("Failed to publish {} match events", events.size(), e);
                // Don't propagate - let order book continue
            }
        });
    }

    /**
     * Creates unique key for match deduplication.
     */
    private String createMatchKey(OrderMatchedEvent event) {
        return String.format("%s-%s-%s",
                event.getBuyOrderId(),
                event.getSellOrderId(),
                event.getMatchedQuantity());
    }

    /**
     * Schedules cleanup of processed matches.
     */
    private void scheduleMatchCleanup(Symbol symbol, List<OrderMatchedEvent> events) {
        // Clean up after 30 seconds to allow for processing
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Set<String> processing = processingMatches.get(symbol);
                if (processing != null) {
                    for (OrderMatchedEvent event : events) {
                        processing.remove(createMatchKey(event));
                    }
                }
            }
        }, 30000);
    }

    /**
     * Gets or creates order book with internal locking.
     */
    private OrderBook getOrCreateOrderBookInternal(Symbol symbol) {
        return orderBookRepository.findBySymbol(symbol)
                .orElseGet(() -> {
                    OrderBook newOrderBook = new OrderBook(symbol);
                    return orderBookRepository.save(newOrderBook);
                });
    }

    /**
     * Gets write lock for symbol.
     */
    private java.util.concurrent.locks.Lock getWriteLock(Symbol symbol) {
        return symbolLocks.computeIfAbsent(symbol, k -> new ReentrantReadWriteLock())
                .writeLock();
    }

    /**
     * Gets read lock for symbol.
     */
    private java.util.concurrent.locks.Lock getReadLock(Symbol symbol) {
        return symbolLocks.computeIfAbsent(symbol, k -> new ReentrantReadWriteLock())
                .readLock();
    }

    /**
     * Logs order book state for debugging.
     */
    private void logOrderBookState(OrderBook orderBook, String phase) {
        logger.debug("📊 ORDER BOOK STATE - {}:", phase);
        logger.debug("   Total Orders: {}", orderBook.getOrderCount());
        logger.debug("   Best Bid: {}",
                orderBook.getBestBid().map(Object::toString).orElse("NONE"));
        logger.debug("   Best Ask: {}",
                orderBook.getBestAsk().map(Object::toString).orElse("NONE"));
    }

    // ===== READ-ONLY METHODS (Use Read Locks) =====

    @Override
    @Transactional(readOnly = true)
    public MarketDepth getMarketDepth(Symbol symbol, int levels) {
        var lock = getReadLock(symbol);
        try {
            lock.lock();
            OrderBook orderBook = getOrCreateOrderBookInternal(symbol);
            return orderBook.getMarketDepth(levels);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderBookTickerDTO getOrderBookTicker(Symbol symbol) {
        var lock = getReadLock(symbol);
        try {
            lock.lock();
            OrderBook orderBook = getOrCreateOrderBookInternal(symbol);

            OrderBookTickerDTO ticker = new OrderBookTickerDTO();
            ticker.setSymbol(symbol.getCode());
            ticker.setCurrency(symbol.getQuoteCurrency());
            ticker.setTimestamp(LocalDateTime.now());

            orderBook.getBestBid().ifPresent(bid -> {
                ticker.setBidPrice(bid.getAmount());
                orderBook.getBestBuyOrder().ifPresent(order ->
                        ticker.setBidQuantity(order.getRemainingQuantity())
                );
            });

            orderBook.getBestAsk().ifPresent(ask -> {
                ticker.setAskPrice(ask.getAmount());
                orderBook.getBestSellOrder().ifPresent(order ->
                        ticker.setAskQuantity(order.getRemainingQuantity())
                );
            });

            orderBook.getSpread().ifPresent(spread ->
                    ticker.setSpread(spread.getAmount())
            );

            return ticker;
        } finally {
            lock.unlock();
        }
    }

    // ... other read-only methods remain similar but use read locks ...

    @Override
    @Transactional(readOnly = true)
    public MarketOverview getMarketOverview() {
        if (orderBookRepository instanceof OrderBookRepositoryJpaImpl repo) {
            return repo.getManager().getMarketOverview();
        }
        throw new UnsupportedOperationException("Market overview not available");
    }

    @Override
    public OrderBookOperationResult createOrderBook(Symbol symbol) {
        var lock = getWriteLock(symbol);
        try {
            lock.lock();

            if (orderBookRepository.existsBySymbol(symbol)) {
                return OrderBookOperationResult.builder()
                        .success(false)
                        .message("Order book already exists")
                        .build();
            }

            OrderBook orderBook = new OrderBook(symbol);
            orderBookRepository.save(orderBook);

            return OrderBookOperationResult.builder()
                    .success(true)
                    .message("Order book created")
                    .build();

        } finally {
            lock.unlock();
        }
    }

    @Override
    public int cleanupInactiveOrders() {
        int totalRemoved = 0;

        for (OrderBook orderBook : orderBookRepository.findAll()) {
            Symbol symbol = orderBook.getSymbol();
            var lock = getWriteLock(symbol);

            try {
                if (lock.tryLock(1, TimeUnit.SECONDS)) {
                    try {
                        int beforeCount = orderBook.getOrderCount();
                        orderBook.removeInactiveOrders();
                        int afterCount = orderBook.getOrderCount();
                        totalRemoved += (beforeCount - afterCount);

                        if (beforeCount != afterCount) {
                            orderBookRepository.save(orderBook);
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (Exception e) {
                logger.error("Error cleaning up orders for symbol: {}",
                        symbol.getCode(), e);
            }
        }

        if (totalRemoved > 0) {
            logger.info("Cleaned up {} inactive orders", totalRemoved);
        }

        return totalRemoved;
    }

    // Implement remaining interface methods...

    @Override
    @Transactional(readOnly = true)
    public OrderBookStatisticsDTO getOrderBookStatistics(Symbol symbol) {
        var lock = getReadLock(symbol);
        try {
            lock.lock();
            OrderBook orderBook = getOrCreateOrderBookInternal(symbol);

            OrderBookStatisticsDTO stats = new OrderBookStatisticsDTO();
            // ... populate stats ...
            return stats;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderBookSummaryDTO getOrderBookSummary(Symbol symbol) {
        var lock = getReadLock(symbol);
        try {
            lock.lock();
            OrderBook orderBook = getOrCreateOrderBookInternal(symbol);

            OrderBookSummaryDTO summary = new OrderBookSummaryDTO();
            summary.setSymbol(symbol.getCode());
            summary.setBidLevels(orderBook.getBidLevels().size());
            summary.setAskLevels(orderBook.getAskLevels().size());
            summary.setTotalBidVolume(orderBook.getTotalBidVolume());
            summary.setTotalAskVolume(orderBook.getTotalAskVolume());
            summary.setTimestamp(LocalDateTime.now());

            // Calculate imbalance
            BigDecimal totalVolume = summary.getTotalBidVolume().add(summary.getTotalAskVolume());
            if (totalVolume.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal volumeDiff = summary.getTotalBidVolume().subtract(summary.getTotalAskVolume());
                BigDecimal imbalance = volumeDiff.divide(totalVolume, 4, RoundingMode.HALF_UP);
                summary.setImbalance(imbalance);
            } else {
                summary.setImbalance(BigDecimal.ZERO);
            }

            return summary;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Symbol> getActiveSymbols() {
        return orderBookRepository.findAll().stream()
                .map(OrderBook::getSymbol)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOrderBookActive(Symbol symbol) {
        return orderBookRepository.existsBySymbol(symbol);
    }

    @Override
    @Transactional(readOnly = true)
    public int getTotalOrderCount() {
        return orderBookRepository.findAll().stream()
                .mapToInt(OrderBook::getOrderCount)
                .sum();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, OrderBookStatisticsDTO> getAllMarketStatistics() {
        Map<String, OrderBookStatisticsDTO> allStats = new HashMap<>();

        for (OrderBook orderBook : orderBookRepository.findAll()) {
            OrderBookStatisticsDTO stats = getOrderBookStatistics(orderBook.getSymbol());
            allStats.put(orderBook.getSymbol().getCode(), stats);
        }

        return allStats;
    }

    @Override
    public OrderBookOperationResult removeOrderBook(Symbol symbol) {
        var lock = getWriteLock(symbol);
        try {
            lock.lock();

            boolean removed = orderBookRepository.deleteBySymbol(symbol);

            if (removed) {
                // Clean up locks
                symbolLocks.remove(symbol);
                processingMatches.remove(symbol);

                return OrderBookOperationResult.builder()
                        .success(true)
                        .message("Order book removed")
                        .build();
            }

            return OrderBookOperationResult.builder()
                    .success(false)
                    .message("Order book not found")
                    .build();

        } finally {
            lock.unlock();
        }
    }
}