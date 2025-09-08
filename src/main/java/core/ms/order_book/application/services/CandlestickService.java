package core.ms.order_book.application.services;

import core.ms.order.domain.entities.ITransaction;
import core.ms.order.domain.ports.inbound.TransactionService;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CandlestickService {

    private static final Logger logger = LoggerFactory.getLogger(CandlestickService.class);

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
        logger.debug("Fetching candlesticks for {} from {} to {} with interval {}",
                symbol, from, to, interval);

        // Validate inputs
        if (to == null) {
            to = LocalDateTime.now();
        }
        if (from == null) {
            from = to.minusDays(1); // Default to last 24 hours
        }

        // Check cache first
        String cacheKey = buildCacheKey(symbol, interval, from, to);
        List<CandlestickDTO> cached = candleCache.get(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            logger.debug("Returning {} cached candles for {}", cached.size(), symbol);
            return cached;
        }

        // Fetch historical transactions
        List<TransactionData> transactions = fetchHistoricalTransactions(symbol, from, to);

        if (transactions.isEmpty()) {
            logger.info("No transactions found for {} between {} and {}", symbol, from, to);
            return new ArrayList<>();
        }

        // Aggregate into candlesticks
        List<CandlestickDTO> candles = aggregateToCandles(transactions, interval, from, to);

        // Add current forming candle if exists
        String builderKey = symbol + "-" + interval;
        CurrentCandleBuilder currentBuilder = activeCandles.get(builderKey);
        if (currentBuilder != null && currentBuilder.hasData()) {
            CandlestickDTO currentCandle = currentBuilder.getCurrentCandle();
            // Only add if it's within our time range
            if (!currentCandle.getTime().isBefore(from) && !currentCandle.getTime().isAfter(to)) {
                candles.add(currentCandle);
            }
        }

        // Cache the results (with short TTL)
        if (!candles.isEmpty()) {
            candleCache.put(cacheKey, candles);
            // Schedule cache cleanup
            scheduleCacheCleanup(cacheKey, 60000); // 1 minute TTL
        }

        logger.debug("Returning {} candles for {}", candles.size(), symbol);
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

        // Clear cache for this symbol as we have new data
        clearCacheForSymbol(symbol);
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
            Symbol symbol = Symbol.createFromCode(symbolCode);

            // Fetch transactions from service
            List<ITransaction> transactions = transactionService.findTransactionsByDateRange(from, to);

            // Filter by symbol and convert
            return transactions.stream()
                    .filter(tx -> tx.getSymbol().equals(symbol))
                    .map(tx -> new TransactionData(
                            tx.getPrice().getAmount(),
                            tx.getQuantity(),
                            tx.getCreatedAt()
                    ))
                    .sorted(Comparator.comparing(TransactionData::timestamp))
                    .collect(Collectors.toList());

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
                                                    LocalDateTime to) {
        if (transactions.isEmpty()) {
            return fillEmptyCandles(from, to, interval);
        }

        // Group by time interval
        Map<LocalDateTime, List<TransactionData>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(
                        tx -> truncateToInterval(tx.timestamp, interval)
                ));

        // Convert to candlesticks
        List<CandlestickDTO> candles = grouped.entrySet().stream()
                .map(entry -> buildCandlestick(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CandlestickDTO::getTime))
                .collect(Collectors.toList());

        // Fill gaps with empty candles if needed
        return fillGaps(candles, from, to, interval);
    }

    /**
     * Build a single candlestick from transactions
     */
    private CandlestickDTO buildCandlestick(LocalDateTime time, List<TransactionData> txs) {
        txs.sort(Comparator.comparing(t -> t.timestamp));

        BigDecimal open = txs.get(0).price;
        BigDecimal close = txs.get(txs.size() - 1).price;
        BigDecimal high = txs.stream().map(t -> t.price).max(BigDecimal::compareTo).orElse(open);
        BigDecimal low = txs.stream().map(t -> t.price).min(BigDecimal::compareTo).orElse(open);
        BigDecimal volume = txs.stream().map(t -> t.quantity).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CandlestickDTO(time, open, high, low, close, volume);
    }

    /**
     * Fill gaps in candle data with empty candles
     */
    private List<CandlestickDTO> fillGaps(List<CandlestickDTO> candles,
                                          LocalDateTime from,
                                          LocalDateTime to,
                                          TimeInterval interval) {
        if (candles.isEmpty()) {
            return fillEmptyCandles(from, to, interval);
        }

        List<CandlestickDTO> filled = new ArrayList<>();
        LocalDateTime current = truncateToInterval(from, interval);
        LocalDateTime end = truncateToInterval(to, interval);

        int candleIndex = 0;
        BigDecimal lastClose = candles.get(0).getOpen(); // Default price

        while (!current.isAfter(end)) {
            if (candleIndex < candles.size() &&
                    candles.get(candleIndex).getTime().equals(current)) {
                // We have data for this period
                CandlestickDTO candle = candles.get(candleIndex);
                filled.add(candle);
                lastClose = candle.getClose();
                candleIndex++;
            } else {
                // Create empty candle with last known price
                filled.add(new CandlestickDTO(
                        current, lastClose, lastClose, lastClose, lastClose, BigDecimal.ZERO
                ));
            }
            current = nextInterval(current, interval);
        }

        return filled;
    }

    /**
     * Create empty candles for periods with no data
     */
    private List<CandlestickDTO> fillEmptyCandles(LocalDateTime from,
                                                  LocalDateTime to,
                                                  TimeInterval interval) {
        List<CandlestickDTO> candles = new ArrayList<>();
        LocalDateTime current = truncateToInterval(from, interval);
        LocalDateTime end = truncateToInterval(to, interval);

        // Use a default price (could be fetched from last known price)
        BigDecimal defaultPrice = new BigDecimal("45000"); // Default BTC price

        while (!current.isAfter(end)) {
            candles.add(new CandlestickDTO(
                    current, defaultPrice, defaultPrice, defaultPrice, defaultPrice, BigDecimal.ZERO
            ));
            current = nextInterval(current, interval);
        }

        return candles;
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
        candleCache.entrySet().removeIf(entry -> entry.getKey().startsWith(symbol + "-"));
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
            this.startTime = truncateToInterval(LocalDateTime.now(), interval);
        }

        synchronized void addTransaction(BigDecimal price, BigDecimal quantity) {
            if (open == null) {
                open = high = low = close = price;
                startTime = truncateToInterval(LocalDateTime.now(), interval);
            } else {
                high = high.max(price);
                low = low.min(price);
                close = price;
            }
            volume = volume.add(quantity);
            transactionCount++;
        }

        boolean hasData() {
            return open != null && transactionCount > 0;
        }

        boolean shouldClose() {
            if (!hasData()) return false;
            LocalDateTime currentInterval = truncateToInterval(LocalDateTime.now(), interval);
            return currentInterval.isAfter(startTime);
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