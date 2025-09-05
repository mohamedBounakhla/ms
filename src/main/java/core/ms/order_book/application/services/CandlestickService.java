package core.ms.order_book.application.services;

import core.ms.order.domain.entities.ITransaction;
import core.ms.order.domain.ports.inbound.TransactionService;
import core.ms.order_book.application.dto.query.CandlestickDTO;
import core.ms.order_book.application.dto.query.CandlestickUpdate;
import core.ms.order_book.domain.events.subscribe.TransactionCreatedEvent;
import core.ms.shared.money.Symbol;
import core.ms.utils.TimeInterval;
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

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Active candle builders for real-time updates
    private final Map<String, CurrentCandleBuilder> activeCandles = new ConcurrentHashMap<>();

    // Cache for recent transactions
    private final Map<String, List<TransactionData>> recentTransactions = new ConcurrentHashMap<>();

    /**
     * Get historical candlesticks with optional real-time current candle
     */
    public List<CandlestickDTO> getCandlesticks(String symbol, TimeInterval interval,
                                                LocalDateTime from, LocalDateTime to) {

        // Fetch historical transactions directly from service
        List<TransactionData> historicalData = fetchHistoricalTransactions(symbol, from, to);

        // Aggregate into candlesticks
        List<CandlestickDTO> candles = aggregateToCandles(historicalData, interval);

        // Add current forming candle if exists
        CurrentCandleBuilder currentBuilder = activeCandles.get(symbol + "-" + interval);
        if (currentBuilder != null && currentBuilder.hasData()) {
            candles.add(currentBuilder.getCurrentCandle());
        }

        return candles;
    }

    /**
     * Handle incoming transactions from events
     */
    @EventListener
    public void onTransactionCreated(TransactionCreatedEvent event) {
        String symbol = event.getSymbol().getCode();

        // Store transaction for historical queries
        recentTransactions.computeIfAbsent(symbol, k -> new ArrayList<>())
                .add(new TransactionData(
                        event.getExecutionPrice().getAmount(),
                        event.getExecutedQuantity(),
                        event.getOccurredAt()
                ));

        // Clean up old entries if list gets too large
        List<TransactionData> transactions = recentTransactions.get(symbol);
        if (transactions.size() > 1000) {
            // Keep only last 1000 transactions
            List<TransactionData> trimmed = new ArrayList<>(
                    transactions.subList(transactions.size() - 1000, transactions.size())
            );
            recentTransactions.put(symbol, trimmed);
        }

        // Update all interval candles for this symbol
        for (TimeInterval interval : TimeInterval.values()) {
            String key = symbol + "-" + interval;
            CurrentCandleBuilder builder = activeCandles.computeIfAbsent(
                    key, k -> new CurrentCandleBuilder(symbol, interval)
            );

            builder.addTransaction(
                    event.getExecutionPrice().getAmount(),
                    event.getExecutedQuantity()
            );

            // Broadcast update
            CandlestickUpdate update = new CandlestickUpdate(
                    symbol,
                    interval.toString(),
                    builder.getCurrentCandle()
            );

            messagingTemplate.convertAndSend(
                    "/topic/candles/" + symbol + "/" + interval,
                    update
            );
        }
    }

    /**
     * Close candles at interval boundaries
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
                    if (builder.hasData()) {
                        CandlestickDTO completed = builder.closeAndReset();

                        // Broadcast completed candle
                        messagingTemplate.convertAndSend(
                                "/topic/candles/complete/" + builder.symbol + "/" + interval,
                                completed
                        );
                    }
                });
    }

    /**
     * Fetch historical transactions using direct service call
     */
    private List<TransactionData> fetchHistoricalTransactions(String symbol,
                                                              LocalDateTime from,
                                                              LocalDateTime to) {
        // Convert string symbol to domain Symbol
        Symbol domainSymbol = Symbol.createFromCode(symbol);

        // Direct service call - no REST, no authentication issues
        List<ITransaction> transactions = transactionService.findTransactionsByDateRange(from, to);

        // Filter by symbol and convert to internal data structure
        return transactions.stream()
                .filter(tx -> tx.getSymbol().equals(domainSymbol))
                .map(tx -> new TransactionData(
                        tx.getPrice().getAmount(),
                        tx.getQuantity(),
                        tx.getCreatedAt()
                ))
                .sorted(Comparator.comparing(TransactionData::timestamp))
                .collect(Collectors.toList());
    }

    private List<CandlestickDTO> aggregateToCandles(List<TransactionData> transactions,
                                                    TimeInterval interval) {
        if (transactions.isEmpty()) {
            return new ArrayList<>();
        }

        // Group by time interval
        Map<LocalDateTime, List<TransactionData>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(
                        tx -> truncateToInterval(tx.timestamp, interval)
                ));

        // Convert to candlesticks
        return grouped.entrySet().stream()
                .map(entry -> buildCandlestick(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CandlestickDTO::getTime))
                .collect(Collectors.toList());
    }

    private CandlestickDTO buildCandlestick(LocalDateTime time, List<TransactionData> txs) {
        txs.sort(Comparator.comparing(t -> t.timestamp));

        BigDecimal open = txs.get(0).price;
        BigDecimal close = txs.get(txs.size() - 1).price;
        BigDecimal high = txs.stream().map(t -> t.price).max(BigDecimal::compareTo).orElse(open);
        BigDecimal low = txs.stream().map(t -> t.price).min(BigDecimal::compareTo).orElse(open);
        BigDecimal volume = txs.stream().map(t -> t.quantity).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CandlestickDTO(time, open, high, low, close, volume);
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

    /**
     * Cleanup old cached transactions periodically
     */
    @Scheduled(fixedDelay = 3600000) // Every hour
    public void cleanupOldTransactions() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        recentTransactions.forEach((symbol, transactions) -> {
            List<TransactionData> filtered = transactions.stream()
                    .filter(tx -> tx.timestamp.isAfter(cutoff))
                    .collect(Collectors.toList());
            recentTransactions.put(symbol, filtered);
        });
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

        CurrentCandleBuilder(String symbol, TimeInterval interval) {
            this.symbol = symbol;
            this.interval = interval;
            this.startTime = LocalDateTime.now();
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
        }

        boolean hasData() {
            return open != null;
        }

        CandlestickDTO getCurrentCandle() {
            return new CandlestickDTO(startTime, open, high, low, close, volume);
        }

        CandlestickDTO closeAndReset() {
            CandlestickDTO candle = getCurrentCandle();
            open = high = low = close = null;
            volume = BigDecimal.ZERO;
            startTime = LocalDateTime.now();
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