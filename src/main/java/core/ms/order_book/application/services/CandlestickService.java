package core.ms.order_book.application.services;

import core.ms.order.domain.entities.ITransaction;
import core.ms.order.domain.ports.inbound.TransactionService;
import core.ms.order.infrastructure.persistence.dao.TransactionDAO;
import core.ms.order.infrastructure.persistence.entities.TransactionEntity;
import core.ms.order_book.application.dto.query.CandlestickDTO;
import core.ms.order_book.application.dto.query.CandlestickUpdate;
import core.ms.shared.money.Symbol;
import core.ms.utils.TimeInterval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CandlestickService {

    private static final Logger logger = LoggerFactory.getLogger(CandlestickService.class);


    @Autowired
    private TransactionDAO transactionDAO;  // Add this injection
    @Autowired
    private TransactionService transactionService;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    // Active candle builders for real-time updates
    private final Map<String, CurrentCandleBuilder> activeCandles = new ConcurrentHashMap<>();

    // Cache for recent candles by symbol and interval
    private final Map<String, List<CandlestickDTO>> candleCache = new ConcurrentHashMap<>();

    /**
     * Get historical candlesticks for a symbol and time range
     */
    public List<CandlestickDTO> getCandlesticks(String symbol, TimeInterval interval,
                                                LocalDateTime from, LocalDateTime to) {
        logger.info("getCandlesticks START: symbol={}, interval={}, from={}, to={}",
                symbol, interval, from, to);

        // Validate inputs
        if (to == null) {
            to = LocalDateTime.now();
        }
        if (from == null) {
            from = to.minusDays(1);
        }

        // Check cache first
        String cacheKey = buildCacheKey(symbol, interval, from, to);
        logger.info("Cache key: {}", cacheKey);

        List<CandlestickDTO> cached = candleCache.get(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            logger.info("RETURNING {} CACHED candles", cached.size());
            return cached;
        }

        logger.info("Cache miss, fetching transactions");

        // Fetch historical transactions
        List<TransactionData> transactions = fetchHistoricalTransactions(symbol, from, to);
        logger.info("fetchHistoricalTransactions returned {} transactions", transactions.size());

        if (transactions.isEmpty()) {
            logger.info("No transactions found for {} between {} and {}", symbol, from, to);
            // This might be returning empty candles with same price
            return new ArrayList<>();
        }

        // Aggregate into candlesticks
        logger.info("Calling aggregateToCandles with {} transactions", transactions.size());
        List<CandlestickDTO> candles = aggregateToCandles(transactions, interval, from, to, symbol);

        logger.info("aggregateToCandles returned {} candles", candles.size());

        // Add current forming candle if exists
        String builderKey = symbol + "-" + interval;
        CurrentCandleBuilder currentBuilder = activeCandles.get(builderKey);
        if (currentBuilder != null && currentBuilder.hasData()) {
            logger.info("Adding current candle from builder");
            CandlestickDTO currentCandle = currentBuilder.getCurrentCandle();
            if (!currentCandle.getTime().isBefore(from) && !currentCandle.getTime().isAfter(to)) {
                candles.add(currentCandle);
            }
        }

        // Cache the results
        if (!candles.isEmpty()) {
            logger.info("Caching {} candles", candles.size());
            candleCache.put(cacheKey, candles);
        }

        logger.info("getCandlesticks END: returning {} candles", candles.size());
        return candles;
    }

    /**
     * Handle transaction created events to update real-time candles
     */
    @EventListener
    public void onTransactionCreated(core.ms.order.domain.events.publish.TransactionCreatedEvent event) {
        String symbol = event.getSymbolCode();
        BigDecimal price = event.getExecutionPrice();
        BigDecimal quantity = event.getExecutedQuantity();

        logger.debug("Transaction event received: {} {} @ {}", quantity, symbol, price);

        // Update all interval candles for this symbol
        for (TimeInterval interval : TimeInterval.values()) {
            String key = symbol + "-" + interval;
            CurrentCandleBuilder builder = activeCandles.computeIfAbsent(
                    key, k -> new CurrentCandleBuilder(symbol, interval)
            );

            builder.addTransaction(price, quantity);

            // Broadcast update via WebSocket if available
            if (messagingTemplate != null) {
                try {
                    CandlestickUpdate update = new CandlestickUpdate(
                            symbol,
                            interval.toString(),
                            builder.getCurrentCandle()
                    );

                    String destination = "/topic/ohlc/" + symbol + "/" + interval.toString().toLowerCase();
                    messagingTemplate.convertAndSend(destination, update);

                    logger.trace("Broadcasted candle update to {}", destination);
                } catch (Exception e) {
                    logger.error("Failed to broadcast candle update", e);
                }
            }
        }

        // Clear cache AND notify clients to refetch if needed
        clearCacheAndNotify(symbol);
    }
    private void clearCacheAndNotify(String symbol) {
        // Clear cache
        candleCache.entrySet().removeIf(entry -> entry.getKey().startsWith(symbol + "-"));

        // Send cache invalidation notice via WebSocket
        if (messagingTemplate != null) {
            try {
                // Notify clients that they should refresh their data
                messagingTemplate.convertAndSend(
                        "/topic/ohlc/cache-invalidated/" + symbol,
                        LocalDateTime.now()
                );
                logger.debug("Cache cleared and invalidation notice sent for {}", symbol);
            } catch (Exception e) {
                logger.error("Failed to send cache invalidation notice", e);
            }
        }
    }
    /**
     * Close candles at interval boundaries and create new ones
     */
    @Scheduled(cron = "0 * * * * *") // Every minute
    public void closeMinuteCandles() {
        closeAndBroadcastCandles(TimeInterval.ONE_MINUTE);
    }

    @Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
    public void closeFiveMinuteCandles() {
        closeAndBroadcastCandles(TimeInterval.FIVE_MINUTES);
    }

    @Scheduled(cron = "0 */15 * * * *") // Every 15 minutes
    public void closeFifteenMinuteCandles() {
        closeAndBroadcastCandles(TimeInterval.FIFTEEN_MINUTES);
    }

    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void closeHourlyCandles() {
        closeAndBroadcastCandles(TimeInterval.ONE_HOUR);
    }

    @Scheduled(cron = "0 0 0 * * *") // Every day at midnight
    public void closeDailyCandles() {
        closeAndBroadcastCandles(TimeInterval.ONE_DAY);
    }

    private void closeAndBroadcastCandles(TimeInterval interval) {
        activeCandles.entrySet().stream()
                .filter(e -> e.getKey().endsWith("-" + interval))
                .forEach(entry -> {
                    CurrentCandleBuilder builder = entry.getValue();
                    if (builder.shouldClose()) {
                        CandlestickDTO completed = builder.closeAndReset();

                        if (completed != null && messagingTemplate != null) {
                            // Broadcast completed candle
                            String destination = "/topic/ohlc/complete/" + builder.symbol + "/" + interval.toString().toLowerCase();
                            messagingTemplate.convertAndSend(destination, completed);
                            logger.debug("Closed and broadcasted {} candle for {}", interval, builder.symbol);
                        }
                    }
                });
    }

    /**
     * Fetch historical transactions from the transaction service
     */
    private List<TransactionData> fetchHistoricalTransactions(String symbolCode,
                                                              LocalDateTime from,
                                                              LocalDateTime to) {
        try {
            logger.info("Fetching transactions for {} from {} to {}", symbolCode, from, to);

            // Tiche !!!!!
            List<TransactionEntity> allEntities = transactionDAO.findByCreatedAtBetween(from, to);
            logger.info("Found {} transaction entities in date range", allEntities.size());

            List<TransactionData> filtered = allEntities.stream()
                    .filter(entity -> symbolCode.equals(entity.getSymbolCode()))
                    .map(entity -> new TransactionData(
                            entity.getPrice(),
                            entity.getQuantity(),
                            entity.getCreatedAt()
                    ))
                    .sorted(Comparator.comparing(TransactionData::timestamp))
                    .collect(Collectors.toList());

            logger.info("After filtering: {} transactions for {}", filtered.size(), symbolCode);
            return filtered;

        } catch (Exception e) {
            logger.error("Failed to fetch historical transactions for {}", symbolCode, e);
            return new ArrayList<>();
        }
    }

    /**
     * Aggregate transactions into candlesticks
     */
    private List<CandlestickDTO> aggregateToCandles(List<TransactionData> transactions,
                                                    TimeInterval interval,
                                                    LocalDateTime from,
                                                    LocalDateTime to,
                                                    String symbol) {
        logger.info("aggregateToCandles: {} transactions for {}", transactions.size(), symbol);

        if (transactions.isEmpty()) {
            logger.info("No transactions, returning empty list");
            return new ArrayList<>();  // Just return empty instead of filling
        }

        // Group by time interval
        Map<LocalDateTime, List<TransactionData>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(
                        tx -> truncateToInterval(tx.timestamp, interval)
                ));

        logger.info("Grouped into {} time intervals", grouped.size());

        // Convert to candlesticks
        List<CandlestickDTO> candles = grouped.entrySet().stream()
                .map(entry -> buildCandlestick(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CandlestickDTO::getTime))
                .collect(Collectors.toList());

        logger.info("Built {} candles", candles.size());
        return candles;  // Return actual candles only
    }
    /**
     * Build a single candlestick from transactions
     */
    private CandlestickDTO buildCandlestick(LocalDateTime time, List<TransactionData> txs) {
        // ADD THIS DEBUG LOG
        logger.debug("Building candle for {} with {} transactions", time, txs.size());
        for (TransactionData tx : txs) {
            logger.debug("  - Transaction at {} price: {}", tx.timestamp, tx.price);
        }

        txs.sort(Comparator.comparing(t -> t.timestamp));

        BigDecimal open = txs.get(0).price;
        BigDecimal close = txs.get(txs.size() - 1).price;
        BigDecimal high = txs.stream().map(t -> t.price).max(BigDecimal::compareTo).orElse(open);
        BigDecimal low = txs.stream().map(t -> t.price).min(BigDecimal::compareTo).orElse(open);
        BigDecimal volume = txs.stream().map(t -> t.quantity).reduce(BigDecimal.ZERO, BigDecimal::add);

        // ADD THIS DEBUG LOG
        logger.debug("Candle OHLC - O:{} H:{} L:{} C:{} V:{}", open, high, low, close, volume);

        return new CandlestickDTO(time, open, high, low, close, volume);
    }

    /**
     * Fill gaps in candle data with empty candles

    private List<CandlestickDTO> fillGaps(List<CandlestickDTO> candles,
                                          LocalDateTime from,
                                          LocalDateTime to,
                                          TimeInterval interval,
                                          String symbol) {
        if (candles.isEmpty()) {
            return fillEmptyCandles(from, to, interval, symbol);
        }

        List<CandlestickDTO> filled = new ArrayList<>();
        LocalDateTime current = truncateToInterval(from, interval);
        LocalDateTime end = truncateToInterval(to, interval);

        BigDecimal lastClose = null;
        if (!candles.isEmpty() && candles.get(0).getTime().isAfter(current)) {
            // There's a gap at the beginning - get last known price before 'from'
            lastClose = getLastKnownPrice(symbol, from);
        }

        // If still no price, use first candle's open
        if (lastClose == null && !candles.isEmpty()) {
            lastClose = candles.get(0).getOpen();
        }

        // If still no price, we can't fill gaps
        if (lastClose == null) {
            return candles; // Return as-is
        }

        int candleIndex = 0;

        while (!current.isAfter(end)) {
            if (candleIndex < candles.size() &&
                    candles.get(candleIndex).getTime().equals(current)) {
                // We have data for this period
                CandlestickDTO candle = candles.get(candleIndex);
                filled.add(candle);
                lastClose = candle.getClose(); // Update lastClose with actual close
                candleIndex++;
            } else {
                // Create empty candle with last known close price
                filled.add(new CandlestickDTO(
                        current, lastClose, lastClose, lastClose, lastClose, BigDecimal.ZERO
                ));
            }
            current = nextInterval(current, interval);
        }

        return filled;
    } */
    private List<CandlestickDTO> fillGaps(List<CandlestickDTO> candles,
                                          LocalDateTime from,
                                          LocalDateTime to,
                                          TimeInterval interval,
                                          String symbol) {
        // Just return the actual candles without filling gaps
        return candles;
    }
    /**
     * Create empty candles for periods with no data
     */
    private List<CandlestickDTO> fillEmptyCandles(LocalDateTime from,
                                                  LocalDateTime to,
                                                  TimeInterval interval,
                                                  String symbolCode) {
        List<CandlestickDTO> candles = new ArrayList<>();
        LocalDateTime current = truncateToInterval(from, interval);
        LocalDateTime end = truncateToInterval(to, interval);

        // Get last known price from transactions
        BigDecimal lastKnownPrice = getLastKnownPrice(symbolCode, from);

        // If no historical price exists at all, return empty list
        if (lastKnownPrice == null) {
            logger.debug("No historical price found for {} before {}", symbolCode, from);
            return candles;
        }

        while (!current.isAfter(end)) {
            candles.add(new CandlestickDTO(
                    current, lastKnownPrice, lastKnownPrice, lastKnownPrice, lastKnownPrice, BigDecimal.ZERO
            ));
            current = nextInterval(current, interval);
        }

        return candles;
    }
    private BigDecimal getLastKnownPrice(String symbolCode, LocalDateTime beforeTime) {
        try {
            Symbol symbol = Symbol.createFromCode(symbolCode);

            // Get the most recent transaction before the requested time
            List<ITransaction> recentTransactions = transactionService
                    .findTransactionsByDateRange(beforeTime.minusDays(30), beforeTime);

            // Filter by symbol and get the last one
            Optional<ITransaction> lastTransaction = recentTransactions.stream()
                    .filter(tx -> tx.getSymbol().getCode().equals(symbolCode))
                    .max(Comparator.comparing(ITransaction::getCreatedAt));

            if (lastTransaction.isPresent()) {
                return lastTransaction.get().getPrice().getAmount();
            }

            // If no recent transaction, try to get ANY transaction for this symbol
            List<ITransaction> allTransactions = transactionService.findTransactionsBySymbol(symbol);
            if (!allTransactions.isEmpty()) {
                // Sort by date and get the most recent
                return allTransactions.stream()
                        .max(Comparator.comparing(ITransaction::getCreatedAt))
                        .map(tx -> tx.getPrice().getAmount())
                        .orElse(null);
            }

            return null;
        } catch (Exception e) {
            logger.error("Failed to get last known price for {}", symbolCode, e);
            return null;
        }
    }
    /**
     * Truncate time to interval boundary
     */
    private LocalDateTime truncateToInterval(LocalDateTime time, TimeInterval interval) {
        return switch (interval) {
            case ONE_MINUTE -> time.truncatedTo(ChronoUnit.MINUTES);
            case FIVE_MINUTES -> time.truncatedTo(ChronoUnit.MINUTES)
                    .withMinute((time.getMinute() / 5) * 5);
            case FIFTEEN_MINUTES -> time.truncatedTo(ChronoUnit.MINUTES)
                    .withMinute((time.getMinute() / 15) * 15);
            case ONE_HOUR -> time.truncatedTo(ChronoUnit.HOURS);
            case ONE_DAY -> time.truncatedTo(ChronoUnit.DAYS);
        };
    }

    /**
     * Get next interval time
     */
    private LocalDateTime nextInterval(LocalDateTime time, TimeInterval interval) {
        return switch (interval) {
            case ONE_MINUTE -> time.plusMinutes(1);
            case FIVE_MINUTES -> time.plusMinutes(5);
            case FIFTEEN_MINUTES -> time.plusMinutes(15);
            case ONE_HOUR -> time.plusHours(1);
            case ONE_DAY -> time.plusDays(1);
        };
    }

    /**
     * Build cache key
     */
    private String buildCacheKey(String symbol, TimeInterval interval,
                                 LocalDateTime from, LocalDateTime to) {
        return String.format("%s-%s-%s-%s", symbol, interval,
                from.toEpochSecond(java.time.ZoneOffset.UTC),
                to.toEpochSecond(java.time.ZoneOffset.UTC));
    }

    /**
     * Clear cache for a symbol
     */
    private void clearCacheForSymbol(String symbol) {
        clearCacheAndNotify(symbol);
    }

    /**
     * Schedule cache cleanup
     */
    private void scheduleCacheCleanup(String key, long delayMs) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                candleCache.remove(key);
            }
        }, delayMs);
    }

    /**
     * Cleanup old cached data periodically
     */
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void cleanupCache() {
        int sizeBefore = candleCache.size();
        candleCache.clear();
        logger.debug("Cleared candle cache, removed {} entries", sizeBefore);
    }

    // Inner classes
    private static class CurrentCandleBuilder {
        private final String symbol;
        private final TimeInterval interval;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private BigDecimal volume = BigDecimal.ZERO;
        private LocalDateTime startTime;
        private int transactionCount = 0;

        CurrentCandleBuilder(String symbol, TimeInterval interval) {
            this.symbol = symbol;
            this.interval = interval;
            this.startTime = truncateToInterval(LocalDateTime.now(ZoneOffset.UTC), interval);
        }

        synchronized void addTransaction(BigDecimal price, BigDecimal quantity) {
            // Check if we should have rolled over to a new interval
            LocalDateTime currentInterval = truncateToInterval(LocalDateTime.now(ZoneOffset.UTC), interval);
            if (currentInterval.isAfter(startTime)) {
                startTime = currentInterval;
                open = high = low = close = price;
                volume = quantity;
                transactionCount = 1;
            } else {
                // Add to current interval
                if (open == null) {
                    open = high = low = close = price;
                } else {
                    high = high.max(price);
                    low = low.min(price);
                    close = price;
                }
                volume = volume.add(quantity);
                transactionCount++;
            }
        }

        boolean shouldClose() {
            if (!hasData()) return false;
            LocalDateTime currentInterval = truncateToInterval(LocalDateTime.now(), interval);
            return currentInterval.isAfter(startTime);
        }

        // Keep other methods unchanged
        boolean hasData() {
            return open != null && transactionCount > 0;
        }

        CandlestickDTO getCurrentCandle() {
            if (!hasData()) return null;
            return new CandlestickDTO(startTime, open, high, low, close, volume);
        }

        CandlestickDTO closeAndReset() {
            CandlestickDTO candle = getCurrentCandle();
            // Reset for next period
            open = high = low = close = null;
            volume = BigDecimal.ZERO;
            transactionCount = 0;
            startTime = truncateToInterval(LocalDateTime.now(), interval);
            return candle;
        }

        // Include the truncateToInterval method here for consistency
        private LocalDateTime truncateToInterval(LocalDateTime time, TimeInterval interval) {
            return switch (interval) {
                case ONE_MINUTE -> time.truncatedTo(ChronoUnit.MINUTES);
                case FIVE_MINUTES -> time.truncatedTo(ChronoUnit.MINUTES)
                        .withMinute((time.getMinute() / 5) * 5);
                case FIFTEEN_MINUTES -> time.truncatedTo(ChronoUnit.MINUTES)
                        .withMinute((time.getMinute() / 15) * 15);
                case ONE_HOUR -> time.truncatedTo(ChronoUnit.HOURS);
                case ONE_DAY -> time.truncatedTo(ChronoUnit.DAYS);
            };
        }
    }

    private record TransactionData(BigDecimal price, BigDecimal quantity, LocalDateTime timestamp) {}
}