package core.ms.order_book.tasks;

import core.ms.order_book.application.services.OrderBookApplicationService;
import core.ms.order_book.application.services.OrderSynchronizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderBookMaintenanceTask {

    private static final Logger logger = LoggerFactory.getLogger(OrderBookMaintenanceTask.class);

    @Autowired
    private OrderBookApplicationService orderBookService;

    @Autowired
    private OrderSynchronizationService syncService;



    /**
     * Periodic cleanup of inactive orders.
     * Runs every 30 seconds to ensure order book consistency.
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void cleanupInactiveOrders() {
        try {
            logger.debug("Starting periodic order book cleanup...");

            int removedCount = orderBookService.cleanupInactiveOrders();

            if (removedCount > 0) {
                logger.info("🧹 Cleaned up {} inactive orders", removedCount);
            }

        } catch (Exception e) {
            logger.error("Error during order book cleanup", e);
        }
    }

    /**
     * Health check for order books.
     * Runs every minute to log statistics.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void logOrderBookStatistics() {
        try {
            var overview = orderBookService.getMarketOverview();

            logger.info("📊 Order Book Statistics - Active Symbols: {}, Total Orders: {}",
                    overview.getActiveSymbols().size(),
                    overview.getTotalOrders());

        } catch (Exception e) {
            logger.error("Error logging order book statistics", e);
        }
    }
    /**
     * Periodic cache cleanup.
     * Runs every 2 minutes to remove expired entries.
     */
    @Scheduled(fixedDelay = 120000, initialDelay = 120000)
    public void cleanupCache() {
        try {
            logger.debug("Starting cache cleanup...");

            var statsBefore = syncService.getCacheStatistics();
            syncService.cleanupExpiredEntries();
            var statsAfter = syncService.getCacheStatistics();

            int removed = statsBefore.getTotalEntries() - statsAfter.getTotalEntries();
            if (removed > 0) {
                logger.info("🧹 Removed {} expired cache entries", removed);
            }

        } catch (Exception e) {
            logger.error("Error during cache cleanup", e);
        }
    }
    /**
     * Log cache statistics periodically.
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 300000) // Every 5 minutes
    public void logCacheStatistics() {
        try {
            var stats = syncService.getCacheStatistics();

            logger.info("📊 Cache Statistics - Enabled: {}, Total: {}, Expired: {}",
                    stats.isCacheEnabled(),
                    stats.getTotalEntries(),
                    stats.getExpiredEntries());

        } catch (Exception e) {
            logger.error("Error logging cache statistics", e);
        }
    }
}