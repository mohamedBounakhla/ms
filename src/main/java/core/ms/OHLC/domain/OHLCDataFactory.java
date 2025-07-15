package core.ms.OHLC.domain;

import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import core.ms.utils.IdGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Factory for creating OHLC domain objects from transaction data
 */
public class OHLCDataFactory {

    private static final IdGenerator ID_GENERATOR = new IdGenerator();

    /**
     * Creates OHLCData from a list of transactions
     */
    public static OHLCData createFromTransactions(Symbol symbol, TimeInterval interval,
                                                  List<TransactionData> transactions) {
        Objects.requireNonNull(symbol, "Symbol cannot be null");
        Objects.requireNonNull(interval, "Interval cannot be null");
        Objects.requireNonNull(transactions, "Transactions cannot be null");

        String ohlcDataId = generateOHLCDataId(symbol, interval);
        OHLCData ohlcData = new OHLCData(ohlcDataId, symbol, interval);

        if (transactions.isEmpty()) {
            return ohlcData;
        }

        // Group transactions by interval periods
        Map<Instant, List<TransactionData>> groupedTransactions = groupTransactionsByInterval(transactions, interval);

        // Create candlesticks for each period
        for (Map.Entry<Instant, List<TransactionData>> entry : groupedTransactions.entrySet()) {
            Instant intervalStart = entry.getKey();
            List<TransactionData> intervalTransactions = entry.getValue();

            Candlestick candlestick = createCandlestick(symbol, interval, intervalStart, intervalTransactions);
            ohlcData.addCandle(candlestick);
        }

        return ohlcData;
    }

    /**
     * Creates a single candlestick from transactions within a time period
     */
    public static Candlestick createCandlestick(Symbol symbol, TimeInterval interval,
                                                Instant timestamp, List<TransactionData> transactions) {
        Objects.requireNonNull(symbol, "Symbol cannot be null");
        Objects.requireNonNull(interval, "Interval cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        Objects.requireNonNull(transactions, "Transactions cannot be null");

        if (transactions.isEmpty()) {
            throw new IllegalArgumentException("Cannot create candlestick from empty transactions");
        }

        // Validate all transactions are for the same symbol
        validateTransactionsSymbol(transactions, symbol);

        // Calculate OHLC and volume
        OHLC ohlc = calculateOHLC(transactions);
        BigDecimal volume = calculateVolume(transactions);

        // Align timestamp to interval boundary
        Instant alignedTimestamp = interval.alignTimestamp(timestamp);

        String candlestickId = generateCandlestickId(symbol, interval, alignedTimestamp);

        return new Candlestick(
                candlestickId,
                symbol,
                alignedTimestamp,
                interval,
                ohlc.getOpen(),
                ohlc.getHigh(),
                ohlc.getLow(),
                ohlc.getClose(),
                volume
        );
    }

    /**
     * Aggregates multiple transactions into OHLC values
     */
    public static OHLC aggregateTransactions(List<TransactionData> transactions) {
        Objects.requireNonNull(transactions, "Transactions cannot be null");

        if (transactions.isEmpty()) {
            throw new IllegalArgumentException("Cannot aggregate empty transactions");
        }

        return calculateOHLC(transactions);
    }

    /**
     * Creates metadata for OHLCData
     */
    public static ChartMetadata createMetadata(OHLCData ohlcData) {
        Objects.requireNonNull(ohlcData, "OHLCData cannot be null");

        Optional<DateRange> dateRange = ohlcData.getDateRange();
        Optional<Money> highestPrice = ohlcData.getHighestPrice();
        Optional<Money> lowestPrice = ohlcData.getLowestPrice();

        PriceRange priceRange = null;
        if (highestPrice.isPresent() && lowestPrice.isPresent()) {
            priceRange = new PriceRange(lowestPrice.get(), highestPrice.get());
        }

        VolumeRange volumeRange = createVolumeRange(ohlcData);

        return new ChartMetadata(
                ohlcData.getSymbol(),
                ohlcData.getInterval(),
                dateRange.orElse(null),
                ohlcData.size(),
                priceRange,
                volumeRange
        );
    }

    /**
     * Creates a price range from a list of candlesticks
     */
    public static PriceRange createPriceRange(List<Candlestick> candles) {
        Objects.requireNonNull(candles, "Candles cannot be null");

        if (candles.isEmpty()) {
            throw new IllegalArgumentException("Cannot create price range from empty candles");
        }

        Money min = candles.stream()
                .map(Candlestick::getLow)
                .reduce((money1, money2) -> money1.isLessThan(money2) ? money1 : money2)
                .orElseThrow(() -> new IllegalStateException("No minimum price found"));

        Money max = candles.stream()
                .map(Candlestick::getHigh)
                .reduce((money1, money2) -> money1.isGreaterThan(money2) ? money1 : money2)
                .orElseThrow(() -> new IllegalStateException("No maximum price found"));

        return new PriceRange(min, max);
    }

    /**
     * Creates a volume range from OHLCData
     */
    public static VolumeRange createVolumeRange(OHLCData ohlcData) {
        Objects.requireNonNull(ohlcData, "OHLCData cannot be null");

        List<Candlestick> candles = ohlcData.getAllCandles();

        if (candles.isEmpty()) {
            return new VolumeRange(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal minVolume = candles.stream()
                .map(Candlestick::getVolume)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxVolume = candles.stream()
                .map(Candlestick::getVolume)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal avgVolume = ohlcData.getAverageVolume();

        return new VolumeRange(minVolume, maxVolume, avgVolume);
    }

    // ===== PRIVATE HELPER METHODS =====

    private static Map<Instant, List<TransactionData>> groupTransactionsByInterval(
            List<TransactionData> transactions, TimeInterval interval) {

        return transactions.stream()
                .collect(Collectors.groupingBy(
                        transaction -> interval.alignTimestamp(transaction.getTimestamp()),
                        TreeMap::new, // Keep sorted by timestamp
                        Collectors.toList()
                ));
    }

    private static OHLC calculateOHLC(List<TransactionData> transactions) {
        // Sort transactions by timestamp to get proper sequence
        List<TransactionData> sortedTransactions = transactions.stream()
                .sorted(Comparator.comparing(TransactionData::getTimestamp))
                .collect(Collectors.toList());

        // First transaction provides the open price
        Money open = sortedTransactions.get(0).getPrice();

        // Last transaction provides the close price
        Money close = sortedTransactions.get(sortedTransactions.size() - 1).getPrice();

        // Find highest and lowest prices
        Money high = sortedTransactions.stream()
                .map(TransactionData::getPrice)
                .reduce(open, (money1, money2) -> money1.isGreaterThan(money2) ? money1 : money2);

        Money low = sortedTransactions.stream()
                .map(TransactionData::getPrice)
                .reduce(open, (money1, money2) -> money1.isLessThan(money2) ? money1 : money2);

        return new OHLC(open, high, low, close);
    }

    private static BigDecimal calculateVolume(List<TransactionData> transactions) {
        return transactions.stream()
                .map(TransactionData::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void validateTransactionsSymbol(List<TransactionData> transactions, Symbol expectedSymbol) {
        boolean allMatch = transactions.stream()
                .allMatch(transaction -> transaction.getSymbol().equals(expectedSymbol));

        if (!allMatch) {
            throw new IllegalArgumentException("All transactions must be for the same symbol: " + expectedSymbol.getCode());
        }
    }

    private static String generateOHLCDataId(Symbol symbol, TimeInterval interval) {
        return String.format("OHLC-%s-%s-%d",
                symbol.getCode(),
                interval.getCode(),
                System.currentTimeMillis());
    }

    private static String generateCandlestickId(Symbol symbol, TimeInterval interval, Instant timestamp) {
        return String.format("CANDLE-%s-%s-%d",
                symbol.getCode(),
                interval.getCode(),
                timestamp.getEpochSecond());
    }
}

// ===== USAGE EXAMPLE =====

/*
// Example usage in an application service:

@Service
public class OHLCUpdateService {

    @EventHandler
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        ITransaction transaction = event.getTransaction();

        // Convert to TransactionData
        TransactionData transactionData = new TransactionData(
            transaction.getCreatedAt(),
            transaction.getPrice(),
            transaction.getQuantity(),
            transaction.getSymbol()
        );

        // Get or create OHLCData
        OHLCData ohlcData = ohlcRepository.findBySymbolAndInterval(
            transaction.getSymbol(),
            TimeInterval.ONE_MINUTE
        ).orElseGet(() -> new OHLCData(
            IdGenerator.generateId(),
            transaction.getSymbol(),
            TimeInterval.ONE_MINUTE
        ));

        // Create candlestick from this transaction
        Instant intervalTimestamp = TimeInterval.ONE_MINUTE.alignTimestamp(transaction.getCreatedAt());

        // In real implementation, you'd collect all transactions for this interval
        List<TransactionData> intervalTransactions = List.of(transactionData);

        Candlestick candlestick = OHLCDataFactory.createCandlestick(
            transaction.getSymbol(),
            TimeInterval.ONE_MINUTE,
            intervalTimestamp,
            intervalTransactions
        );

        ohlcData.addCandle(candlestick);
        ohlcRepository.save(ohlcData);
    }
}
*/