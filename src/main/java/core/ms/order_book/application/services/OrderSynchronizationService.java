package core.ms.order_book.application.services;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order.domain.ports.outbound.OrderRepository;
import core.ms.shared.money.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service responsible for synchronizing orders between Order BC and Order Book BC.
 * Implements smart caching to minimize repository queries while maintaining consistency.
 */
@Service
public class OrderSynchronizationService {

    private static final Logger logger = LoggerFactory.getLogger(OrderSynchronizationService.class);

    @Autowired
    private OrderRepository orderRepository;

    // Configuration
    @Value("${orderbook.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${orderbook.cache.ttl-seconds:60}")
    private int cacheTtlSeconds;

    // Cache structures
    private final Map<String, CachedOrder> orderCache = new ConcurrentHashMap<>();
    private final Map<Symbol, LocalDateTime> lastFullRefresh = new ConcurrentHashMap<>();
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();

    /**
     * Gets an order with caching logic.
     * Fresh load for new orders, cached for known orders.
     */
    public Optional<IOrder> getOrder(String orderId) {
        if (!cacheEnabled) {
            return orderRepository.findById(orderId);
        }

        cacheLock.readLock().lock();
        try {
            CachedOrder cached = orderCache.get(orderId);

            if (cached != null && !cached.isExpired(cacheTtlSeconds)) {
                logger.debug("Cache hit for order: {}", orderId);
                return Optional.of(cached.order);
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        // Cache miss or expired - fetch from repository
        logger.debug("Cache miss for order: {}, fetching from repository", orderId);

        Optional<IOrder> orderOpt = orderRepository.findById(orderId);

        orderOpt.ifPresent(this::cacheOrder);

        return orderOpt;
    }

    /**
     * Gets an order and forces a refresh if it's a critical update.
     */
    public Optional<IOrder> getOrderWithRefresh(String orderId, boolean forceRefresh) {
        if (forceRefresh) {
            evictFromCache(orderId);
        }
        return getOrder(orderId);
    }

    /**
     * Batch fetch orders for a symbol.
     */
    public void refreshOrdersForSymbol(Symbol symbol) {
        logger.info("Refreshing all orders for symbol: {}", symbol.getCode());

        try {
            // Get all orders for the symbol from repository
            var orders = orderRepository.findBySymbol(symbol);

            cacheLock.writeLock().lock();
            try {
                // Update cache with fresh data
                for (IOrder order : orders) {
                    cacheOrder(order);
                }

                lastFullRefresh.put(symbol, LocalDateTime.now());

            } finally {
                cacheLock.writeLock().unlock();
            }

            logger.info("Cached {} orders for symbol: {}", orders.size(), symbol.getCode());

        } catch (Exception e) {
            logger.error("Failed to refresh orders for symbol: {}", symbol.getCode(), e);
        }
    }

    /**
     * Determines if an order update is significant enough to require order book update.
     */
    public boolean isSignificantUpdate(IOrder oldOrder, IOrder newOrder) {
        // Null checks
        if (oldOrder == null || newOrder == null) {
            return true;
        }

        // Status change is always significant
        if (!oldOrder.getStatus().equals(newOrder.getStatus())) {
            return true;
        }

        // Price change is significant
        if (!oldOrder.getPrice().equals(newOrder.getPrice())) {
            return true;
        }

        // Significant quantity change (more than 0.01%)
        BigDecimal oldRemaining = oldOrder.getRemainingQuantity();
        BigDecimal newRemaining = newOrder.getRemainingQuantity();

        if (oldRemaining.compareTo(BigDecimal.ZERO) == 0) {
            return newRemaining.compareTo(BigDecimal.ZERO) != 0;
        }

        BigDecimal change = newRemaining.subtract(oldRemaining).abs();
        BigDecimal threshold = oldRemaining.multiply(BigDecimal.valueOf(0.0001)); // 0.01%

        return change.compareTo(threshold) > 0;
    }

    /**
     * Gets buy orders for a symbol with caching.
     */
    public List<IBuyOrder> getBuyOrdersForSymbol(Symbol symbol) {
        if (shouldRefreshSymbol(symbol)) {
            refreshOrdersForSymbol(symbol);
        }

        return orderRepository.findBuyOrdersBySymbol(symbol);
    }

    /**
     * Gets sell orders for a symbol with caching.
     */
    public List<ISellOrder> getSellOrdersForSymbol(Symbol symbol) {
        if (shouldRefreshSymbol(symbol)) {
            refreshOrdersForSymbol(symbol);
        }

        return orderRepository.findSellOrdersBySymbol(symbol);
    }

    // ===== CACHE MANAGEMENT =====

    private void cacheOrder(IOrder order) {
        if (!cacheEnabled) {
            return;
        }

        cacheLock.writeLock().lock();
        try {
            orderCache.put(order.getId(), new CachedOrder(order));
            logger.trace("Cached order: {}", order.getId());
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    private void evictFromCache(String orderId) {
        cacheLock.writeLock().lock();
        try {
            orderCache.remove(orderId);
            logger.debug("Evicted order from cache: {}", orderId);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Clears the entire cache.
     */
    public void clearCache() {
        cacheLock.writeLock().lock();
        try {
            int size = orderCache.size();
            orderCache.clear();
            lastFullRefresh.clear();
            logger.info("Cleared order cache ({} entries)", size);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Gets cache statistics.
     */
    public CacheStatistics getCacheStatistics() {
        cacheLock.readLock().lock();
        try {
            int totalCached = orderCache.size();
            long expired = orderCache.values().stream()
                    .filter(c -> c.isExpired(cacheTtlSeconds))
                    .count();

            return new CacheStatistics(totalCached, expired, cacheEnabled);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    /**
     * Cleanup expired entries from cache.
     */
    public void cleanupExpiredEntries() {
        if (!cacheEnabled) {
            return;
        }

        cacheLock.writeLock().lock();
        try {
            int before = orderCache.size();

            orderCache.entrySet().removeIf(entry ->
                    entry.getValue().isExpired(cacheTtlSeconds)
            );

            int removed = before - orderCache.size();
            if (removed > 0) {
                logger.debug("Removed {} expired entries from cache", removed);
            }

        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    // ===== HELPER METHODS =====

    private boolean shouldRefreshSymbol(Symbol symbol) {
        LocalDateTime lastRefresh = lastFullRefresh.get(symbol);

        if (lastRefresh == null) {
            return true;
        }

        // Refresh if more than 30 seconds old
        return lastRefresh.isBefore(LocalDateTime.now().minusSeconds(30));
    }

    // ===== INNER CLASSES =====

    /**
     * Wrapper for cached orders with timestamp.
     */
    private static class CachedOrder {
        final IOrder order;
        final LocalDateTime cachedAt;

        CachedOrder(IOrder order) {
            this.order = order;
            this.cachedAt = LocalDateTime.now();
        }

        boolean isExpired(int ttlSeconds) {
            return cachedAt.isBefore(LocalDateTime.now().minusSeconds(ttlSeconds));
        }
    }

    /**
     * Cache statistics for monitoring.
     */
    public static class CacheStatistics {
        private final int totalEntries;
        private final long expiredEntries;
        private final boolean cacheEnabled;

        public CacheStatistics(int totalEntries, long expiredEntries, boolean cacheEnabled) {
            this.totalEntries = totalEntries;
            this.expiredEntries = expiredEntries;
            this.cacheEnabled = cacheEnabled;
        }

        public int getTotalEntries() { return totalEntries; }
        public long getExpiredEntries() { return expiredEntries; }
        public boolean isCacheEnabled() { return cacheEnabled; }
        public double getHitRate() {
            // This would need actual hit/miss tracking
            return 0.0;
        }
    }
}