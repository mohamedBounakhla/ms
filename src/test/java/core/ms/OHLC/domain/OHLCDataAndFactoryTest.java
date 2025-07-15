package core.ms.OHLC.domain;

import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OHLCData and Factory Tests")
class OHLCDataAndFactoryTest {

    private Symbol btcUsd;
    private TimeInterval oneMinute;
    private Instant baseTimestamp;
    private OHLCData ohlcData;

    @BeforeEach
    void setUp() {
        btcUsd = Symbol.btcUsd();
        oneMinute = TimeInterval.ONE_MINUTE;
        baseTimestamp = Instant.parse("2024-01-01T12:00:00Z");
        ohlcData = new OHLCData("ohlc-1", btcUsd, oneMinute);
    }

    @Nested
    @DisplayName("OHLCData Aggregate Tests")
    class OHLCDataAggregateTests {

        @Test
        @DisplayName("Should create OHLCData from transactions in single interval")
        void shouldCreateOHLCDataFromTransactionsInSingleInterval() {
            // All transactions within the same 1-minute interval
            List<TransactionData> transactions = Arrays.asList(
                    new TransactionData(baseTimestamp, Money.of("50000.00", Currency.USD), new BigDecimal("0.5"), btcUsd),
                    new TransactionData(baseTimestamp.plus(15, ChronoUnit.SECONDS), Money.of("51000.00", Currency.USD), new BigDecimal("0.3"), btcUsd),
                    new TransactionData(baseTimestamp.plus(45, ChronoUnit.SECONDS), Money.of("50500.00", Currency.USD), new BigDecimal("0.7"), btcUsd)
            );

            OHLCData result = OHLCDataFactory.createFromTransactions(btcUsd, oneMinute, transactions);

            assertNotNull(result);
            assertEquals(btcUsd, result.getSymbol());
            assertEquals(oneMinute, result.getInterval());
            assertEquals(1, result.size()); // All transactions in same interval

            Candlestick candle = result.getAllCandles().get(0);
            assertEquals(Money.of("50000.00", Currency.USD), candle.getOpen()); // First transaction price
            assertEquals(Money.of("50500.00", Currency.USD), candle.getClose()); // Last transaction price
            assertEquals(Money.of("51000.00", Currency.USD), candle.getHigh()); // Highest price
            assertEquals(Money.of("50000.00", Currency.USD), candle.getLow()); // Lowest price
            assertEquals(0, new BigDecimal("1.5").compareTo(candle.getVolume())); // Sum of volumes
        }

        @Test
        @DisplayName("Should create OHLCData successfully")
        void shouldCreateOHLCDataSuccessfully() {
            assertEquals("ohlc-1", ohlcData.getId());
            assertEquals(btcUsd, ohlcData.getSymbol());
            assertEquals(oneMinute, ohlcData.getInterval());
            assertTrue(ohlcData.isEmpty());
            assertEquals(0, ohlcData.size());
            assertNotNull(ohlcData.getLastUpdated());
        }

        @Test
        @DisplayName("Should throw exception for null required fields")
        void shouldThrowExceptionForNullRequiredFields() {
            assertThrows(NullPointerException.class, () ->
                    new OHLCData(null, btcUsd, oneMinute));

            assertThrows(NullPointerException.class, () ->
                    new OHLCData("ohlc-1", null, oneMinute));

            assertThrows(NullPointerException.class, () ->
                    new OHLCData("ohlc-1", btcUsd, null));
        }

        @Test
        @DisplayName("Should add candlesticks successfully")
        void shouldAddCandlesticksSuccessfully() {
            Candlestick candle1 = createTestCandlestick("candle-1", baseTimestamp);
            Candlestick candle2 = createTestCandlestick("candle-2", baseTimestamp.plus(1, ChronoUnit.MINUTES));

            ohlcData.addCandle(candle1);
            ohlcData.addCandle(candle2);

            assertFalse(ohlcData.isEmpty());
            assertEquals(2, ohlcData.size());

            List<Candlestick> candles = ohlcData.getAllCandles();
            assertEquals(2, candles.size());
            // Should be sorted by timestamp
            assertEquals(candle1.getId(), candles.get(0).getId());
            assertEquals(candle2.getId(), candles.get(1).getId());
        }

        @Test
        @DisplayName("Should validate candlestick constraints")
        void shouldValidateCandlestickConstraints() {
            Symbol ethUsd = Symbol.ethUsd();
            Candlestick wrongSymbol = new Candlestick(
                    "candle-1", ethUsd, baseTimestamp, oneMinute,
                    Money.of("3000.00", Currency.USD), Money.of("3100.00", Currency.USD),
                    Money.of("2950.00", Currency.USD), Money.of("3050.00", Currency.USD),
                    new BigDecimal("2.0")
            );

            assertThrows(IllegalArgumentException.class, () ->
                    ohlcData.addCandle(wrongSymbol));

            TimeInterval fiveMinutes = TimeInterval.FIVE_MINUTES;
            Candlestick wrongInterval = new Candlestick(
                    "candle-2", btcUsd, baseTimestamp, fiveMinutes,
                    Money.of("50000.00", Currency.USD), Money.of("51000.00", Currency.USD),
                    Money.of("49500.00", Currency.USD), Money.of("50500.00", Currency.USD),
                    new BigDecimal("1.0")
            );

            assertThrows(IllegalArgumentException.class, () ->
                    ohlcData.addCandle(wrongInterval));
        }

        @Test
        @DisplayName("Should get candlesticks by time range")
        void shouldGetCandlesticksByTimeRange() {
            Candlestick candle1 = createTestCandlestick("candle-1", baseTimestamp);
            Candlestick candle2 = createTestCandlestick("candle-2", baseTimestamp.plus(5, ChronoUnit.MINUTES));
            Candlestick candle3 = createTestCandlestick("candle-3", baseTimestamp.plus(10, ChronoUnit.MINUTES));

            ohlcData.addCandle(candle1);
            ohlcData.addCandle(candle2);
            ohlcData.addCandle(candle3);

            Instant startTime = baseTimestamp.plus(2, ChronoUnit.MINUTES);
            Instant endTime = baseTimestamp.plus(8, ChronoUnit.MINUTES);

            List<Candlestick> filteredCandles = ohlcData.getCandlesByTimeRange(startTime, endTime);

            assertEquals(1, filteredCandles.size());
            assertEquals("candle-2", filteredCandles.get(0).getId());
        }

        @Test
        @DisplayName("Should validate time range parameters")
        void shouldValidateTimeRangeParameters() {
            Instant startTime = baseTimestamp.plus(1, ChronoUnit.HOURS);
            Instant endTime = baseTimestamp;

            assertThrows(IllegalArgumentException.class, () ->
                    ohlcData.getCandlesByTimeRange(startTime, endTime));
        }

        @Test
        @DisplayName("Should get latest candlestick")
        void shouldGetLatestCandlestick() {
            assertTrue(ohlcData.getLatestCandle().isEmpty());

            Candlestick candle1 = createTestCandlestick("candle-1", baseTimestamp);
            Candlestick candle2 = createTestCandlestick("candle-2", baseTimestamp.plus(5, ChronoUnit.MINUTES));

            ohlcData.addCandle(candle1);
            ohlcData.addCandle(candle2);

            Optional<Candlestick> latest = ohlcData.getLatestCandle();
            assertTrue(latest.isPresent());
            assertEquals("candle-2", latest.get().getId());
        }

        @Test
        @DisplayName("Should calculate price statistics")
        void shouldCalculatePriceStatistics() {
            Candlestick candle1 = createCandlestickWithPrices("candle-1", baseTimestamp,
                    "50000", "52000", "49000", "51000");
            Candlestick candle2 = createCandlestickWithPrices("candle-2", baseTimestamp.plus(1, ChronoUnit.MINUTES),
                    "51000", "53000", "50500", "52500");

            ohlcData.addCandle(candle1);
            ohlcData.addCandle(candle2);

            Optional<Money> highest = ohlcData.getHighestPrice();
            Optional<Money> lowest = ohlcData.getLowestPrice();

            assertTrue(highest.isPresent());
            assertTrue(lowest.isPresent());
            assertEquals(Money.of("53000.00", Currency.USD), highest.get());
            assertEquals(Money.of("49000.00", Currency.USD), lowest.get());
        }

        @Test
        @DisplayName("Should calculate average volume")
        void shouldCalculateAverageVolume() {
            assertTrue(ohlcData.getAverageVolume().compareTo(BigDecimal.ZERO) == 0);

            Candlestick candle1 = createCandlestickWithVolume("candle-1", baseTimestamp, "2.0");
            Candlestick candle2 = createCandlestickWithVolume("candle-2", baseTimestamp.plus(1, ChronoUnit.MINUTES), "4.0");

            ohlcData.addCandle(candle1);
            ohlcData.addCandle(candle2);

            BigDecimal averageVolume = ohlcData.getAverageVolume();
            assertEquals(0, new BigDecimal("3.00").compareTo(averageVolume));
        }

        @Test
        @DisplayName("Should get date range")
        void shouldGetDateRange() {
            assertTrue(ohlcData.getDateRange().isEmpty());

            Candlestick candle1 = createTestCandlestick("candle-1", baseTimestamp);
            Candlestick candle2 = createTestCandlestick("candle-2", baseTimestamp.plus(10, ChronoUnit.MINUTES));

            ohlcData.addCandle(candle1);
            ohlcData.addCandle(candle2);

            Optional<DateRange> dateRange = ohlcData.getDateRange();
            assertTrue(dateRange.isPresent());
            assertEquals(baseTimestamp, dateRange.get().getStart());
            assertEquals(baseTimestamp.plus(10, ChronoUnit.MINUTES), dateRange.get().getEnd());
        }

        @Test
        @DisplayName("Should convert to chart series")
        void shouldConvertToChartSeries() {
            Candlestick candle1 = createTestCandlestick("candle-1", baseTimestamp);
            Candlestick candle2 = createTestCandlestick("candle-2", baseTimestamp.plus(1, ChronoUnit.MINUTES));

            ohlcData.addCandle(candle1);
            ohlcData.addCandle(candle2);

            ChartSeries series = ohlcData.toChartSeries();

            assertEquals("ohlc-1", series.getId());
            assertEquals("BTC 1m", series.getName());
            assertEquals("BTC", series.getSymbol());
            assertEquals("1m", series.getInterval());
            assertEquals(2, series.getData().size());
            assertFalse(series.isEmpty());
        }

        @Test
        @DisplayName("Should have proper string representation")
        void shouldHaveProperStringRepresentation() {
            String result = ohlcData.toString();
            assertTrue(result.contains("ohlc-1"));
            assertTrue(result.contains("BTC"));
            assertTrue(result.contains("1m"));
            assertTrue(result.contains("0 candlesticks"));
        }
    }

    @Nested
    @DisplayName("OHLCDataFactory Tests")
    class OHLCDataFactoryTests {

        @Test
        @DisplayName("Should create OHLCData from transactions")
        void shouldCreateOHLCDataFromTransactions() {
            List<TransactionData> transactions = Arrays.asList(
                    new TransactionData(baseTimestamp, Money.of("50000.00", Currency.USD), new BigDecimal("0.5"), btcUsd),
                    new TransactionData(baseTimestamp.plus(30, ChronoUnit.SECONDS), Money.of("51000.00", Currency.USD), new BigDecimal("0.3"), btcUsd),
                    new TransactionData(baseTimestamp.plus(90, ChronoUnit.SECONDS), Money.of("50500.00", Currency.USD), new BigDecimal("0.7"), btcUsd)
            );

            OHLCData result = OHLCDataFactory.createFromTransactions(btcUsd, oneMinute, transactions);

            assertNotNull(result);
            assertEquals(btcUsd, result.getSymbol());
            assertEquals(oneMinute, result.getInterval());
            assertEquals(2, result.size()); // Transactions span 2 intervals: 12:00:00 and 12:01:00

            List<Candlestick> candles = result.getAllCandles();

            // First candlestick (12:00:00 interval) - 2 transactions
            Candlestick candle1 = candles.get(0);
            assertEquals(Money.of("50000.00", Currency.USD), candle1.getOpen()); // First transaction
            assertEquals(Money.of("51000.00", Currency.USD), candle1.getClose()); // Last transaction in interval
            assertEquals(Money.of("51000.00", Currency.USD), candle1.getHigh()); // Highest in interval
            assertEquals(Money.of("50000.00", Currency.USD), candle1.getLow()); // Lowest in interval
            assertEquals(0, new BigDecimal("0.8").compareTo(candle1.getVolume())); // 0.5 + 0.3

            // Second candlestick (12:01:00 interval) - 1 transaction
            Candlestick candle2 = candles.get(1);
            assertEquals(Money.of("50500.00", Currency.USD), candle2.getOpen()); // Only transaction
            assertEquals(Money.of("50500.00", Currency.USD), candle2.getClose()); // Same transaction
            assertEquals(Money.of("50500.00", Currency.USD), candle2.getHigh()); // Same transaction
            assertEquals(Money.of("50500.00", Currency.USD), candle2.getLow()); // Same transaction
            assertEquals(0, new BigDecimal("0.7").compareTo(candle2.getVolume())); // Only volume
        }

        @Test
        @DisplayName("Should create candlestick from transactions")
        void shouldCreateCandlestickFromTransactions() {
            List<TransactionData> transactions = Arrays.asList(
                    new TransactionData(baseTimestamp, Money.of("50000.00", Currency.USD), new BigDecimal("0.5"), btcUsd),
                    new TransactionData(baseTimestamp.plus(15, ChronoUnit.SECONDS), Money.of("51500.00", Currency.USD), new BigDecimal("0.3"), btcUsd),
                    new TransactionData(baseTimestamp.plus(45, ChronoUnit.SECONDS), Money.of("49500.00", Currency.USD), new BigDecimal("0.2"), btcUsd),
                    new TransactionData(baseTimestamp.plus(50, ChronoUnit.SECONDS), Money.of("50500.00", Currency.USD), new BigDecimal("0.4"), btcUsd)
            );

            Candlestick candle = OHLCDataFactory.createCandlestick(btcUsd, oneMinute, baseTimestamp, transactions);

            assertEquals(btcUsd, candle.getSymbol());
            assertEquals(oneMinute, candle.getInterval());
            assertEquals(oneMinute.alignTimestamp(baseTimestamp), candle.getTimestamp());

            // OHLC should be calculated correctly
            assertEquals(Money.of("50000.00", Currency.USD), candle.getOpen()); // First transaction
            assertEquals(Money.of("51500.00", Currency.USD), candle.getHigh()); // Highest price
            assertEquals(Money.of("49500.00", Currency.USD), candle.getLow()); // Lowest price
            assertEquals(Money.of("50500.00", Currency.USD), candle.getClose()); // Last transaction

            // Volume should be sum of all transaction volumes
            assertEquals(0, new BigDecimal("1.4").compareTo(candle.getVolume()));
        }

        @Test
        @DisplayName("Should aggregate transactions into OHLC")
        void shouldAggregateTransactionsIntoOHLC() {
            List<TransactionData> transactions = Arrays.asList(
                    new TransactionData(baseTimestamp, Money.of("100.00", Currency.USD), new BigDecimal("1.0"), btcUsd),
                    new TransactionData(baseTimestamp.plus(10, ChronoUnit.SECONDS), Money.of("105.00", Currency.USD), new BigDecimal("1.0"), btcUsd),
                    new TransactionData(baseTimestamp.plus(20, ChronoUnit.SECONDS), Money.of("95.00", Currency.USD), new BigDecimal("1.0"), btcUsd),
                    new TransactionData(baseTimestamp.plus(30, ChronoUnit.SECONDS), Money.of("102.00", Currency.USD), new BigDecimal("1.0"), btcUsd)
            );

            OHLC ohlc = OHLCDataFactory.aggregateTransactions(transactions);

            assertEquals(Money.of("100.00", Currency.USD), ohlc.getOpen());
            assertEquals(Money.of("105.00", Currency.USD), ohlc.getHigh());
            assertEquals(Money.of("95.00", Currency.USD), ohlc.getLow());
            assertEquals(Money.of("102.00", Currency.USD), ohlc.getClose());
            assertTrue(ohlc.isValid());
        }

        @Test
        @DisplayName("Should create price range from candlesticks")
        void shouldCreatePriceRangeFromCandlesticks() {
            List<Candlestick> candles = Arrays.asList(
                    createCandlestickWithPrices("candle-1", baseTimestamp, "50000", "52000", "49000", "51000"),
                    createCandlestickWithPrices("candle-2", baseTimestamp.plus(1, ChronoUnit.MINUTES), "51000", "53000", "50500", "52500")
            );

            PriceRange priceRange = OHLCDataFactory.createPriceRange(candles);

            assertEquals(Money.of("49000.00", Currency.USD), priceRange.getMin());
            assertEquals(Money.of("53000.00", Currency.USD), priceRange.getMax());
            assertEquals(Money.of("4000.00", Currency.USD), priceRange.getRange());
        }

        @Test
        @DisplayName("Should validate factory inputs")
        void shouldValidateFactoryInputs() {
            // Null parameters should throw
            assertThrows(NullPointerException.class, () ->
                    OHLCDataFactory.createFromTransactions(null, oneMinute, Arrays.asList()));

            assertThrows(NullPointerException.class, () ->
                    OHLCDataFactory.createCandlestick(btcUsd, null, baseTimestamp, Arrays.asList()));

            // Empty transactions should throw for candlestick creation
            assertThrows(IllegalArgumentException.class, () ->
                    OHLCDataFactory.createCandlestick(btcUsd, oneMinute, baseTimestamp, Arrays.asList()));

            // Empty candles should throw for price range
            assertThrows(IllegalArgumentException.class, () ->
                    OHLCDataFactory.createPriceRange(Arrays.asList()));
        }

        @Test
        @DisplayName("Should validate transaction symbol consistency")
        void shouldValidateTransactionSymbolConsistency() {
            Symbol ethUsd = Symbol.ethUsd();
            List<TransactionData> mixedSymbols = Arrays.asList(
                    new TransactionData(baseTimestamp, Money.of("50000.00", Currency.USD), new BigDecimal("0.5"), btcUsd),
                    new TransactionData(baseTimestamp, Money.of("3000.00", Currency.USD), new BigDecimal("1.0"), ethUsd)
            );

            assertThrows(IllegalArgumentException.class, () ->
                    OHLCDataFactory.createCandlestick(btcUsd, oneMinute, baseTimestamp, mixedSymbols));
        }

        @Test
        @DisplayName("Should handle empty transactions for OHLCData creation")
        void shouldHandleEmptyTransactionsForOHLCDataCreation() {
            OHLCData result = OHLCDataFactory.createFromTransactions(btcUsd, oneMinute, Arrays.asList());

            assertNotNull(result);
            assertEquals(btcUsd, result.getSymbol());
            assertEquals(oneMinute, result.getInterval());
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("TransactionData Tests")
    class TransactionDataTests {

        @Test
        @DisplayName("Should create transaction data successfully")
        void shouldCreateTransactionDataSuccessfully() {
            Money price = Money.of("50000.00", Currency.USD);
            BigDecimal quantity = new BigDecimal("0.5");

            TransactionData data = new TransactionData(baseTimestamp, price, quantity, btcUsd);

            assertEquals(baseTimestamp, data.getTimestamp());
            assertEquals(price, data.getPrice());
            assertEquals(0, quantity.compareTo(data.getQuantity()));
            assertEquals(btcUsd, data.getSymbol());
            assertEquals(Money.of("25000.00", Currency.USD), data.getTotalValue());
        }

        @Test
        @DisplayName("Should validate transaction data constraints")
        void shouldValidateTransactionDataConstraints() {
            Money price = Money.of("50000.00", Currency.USD);

            // Null parameters should throw
            assertThrows(NullPointerException.class, () ->
                    new TransactionData(null, price, new BigDecimal("0.5"), btcUsd));

            // Zero or negative quantity should throw
            assertThrows(IllegalArgumentException.class, () ->
                    new TransactionData(baseTimestamp, price, BigDecimal.ZERO, btcUsd));

            assertThrows(IllegalArgumentException.class, () ->
                    new TransactionData(baseTimestamp, price, new BigDecimal("-0.5"), btcUsd));
        }
    }

    @Nested
    @DisplayName("Additional Business Rule Tests")
    class AdditionalBusinessRuleTests {

        @Test
        @DisplayName("Should handle edge case: identical OHLC prices")
        void shouldHandleIdenticalOHLCPrices() {
            Money samePrice = Money.of("50000.00", Currency.USD);
            BigDecimal volume = new BigDecimal("1.0");

            // All prices identical - should be valid
            Candlestick flatCandle = new Candlestick(
                    "flat-candle", btcUsd, baseTimestamp, oneMinute,
                    samePrice, samePrice, samePrice, samePrice, volume
            );

            assertTrue(flatCandle.isDoji());
            assertEquals(Money.zero(Currency.USD), flatCandle.getBodySize());
            assertEquals(Money.zero(Currency.USD), flatCandle.getRange());
            assertEquals(Money.zero(Currency.USD), flatCandle.getUpperShadow());
            assertEquals(Money.zero(Currency.USD), flatCandle.getLowerShadow());
        }

        @Test
        @DisplayName("Should handle zero volume transactions")
        void shouldHandleZeroVolumeTransactions() {
            Money openPrice = Money.of("50000.00", Currency.USD);
            Money highPrice = Money.of("52000.00", Currency.USD);
            Money lowPrice = Money.of("49500.00", Currency.USD);
            Money closePrice = Money.of("51500.00", Currency.USD);

            // Zero volume should be allowed but handled correctly
            Candlestick zeroVolumeCandle = new Candlestick(
                    "zero-vol", btcUsd, baseTimestamp, oneMinute,
                    openPrice, highPrice, lowPrice, closePrice, BigDecimal.ZERO
            );

            assertEquals(BigDecimal.ZERO, zeroVolumeCandle.getVolume());
            // Weighted price should fall back to typical price when volume is zero
            assertEquals(zeroVolumeCandle.getTypicalPrice(), zeroVolumeCandle.getWeightedPrice());
        }

        @Test
        @DisplayName("Should handle concurrent candlestick additions to OHLCData")
        void shouldHandleConcurrentCandlestickAdditions() {
            // Test that OHLCData maintains consistency during concurrent operations
            OHLCData testOhlcData = new OHLCData("test-ohlc", btcUsd, oneMinute);

            // Add multiple candlesticks in different order
            Candlestick candle3 = createTestCandlestick("candle-3", baseTimestamp.plus(2, ChronoUnit.MINUTES));
            Candlestick candle1 = createTestCandlestick("candle-1", baseTimestamp);
            Candlestick candle2 = createTestCandlestick("candle-2", baseTimestamp.plus(1, ChronoUnit.MINUTES));

            testOhlcData.addCandle(candle3); // Add out of order
            testOhlcData.addCandle(candle1);
            testOhlcData.addCandle(candle2);

            List<Candlestick> sortedCandles = testOhlcData.getAllCandles();
            // Should be automatically sorted by timestamp
            assertEquals("candle-1", sortedCandles.get(0).getId());
            assertEquals("candle-2", sortedCandles.get(1).getId());
            assertEquals("candle-3", sortedCandles.get(2).getId());
        }

        @Test
        @DisplayName("Should validate interval boundary alignment")
        void shouldValidateIntervalBoundaryAlignment() {
            // Test that timestamps are properly aligned to interval boundaries
            Instant unalignedTime = Instant.parse("2024-01-01T12:34:45Z");
            Money price = Money.of("50000.00", Currency.USD);
            BigDecimal volume = new BigDecimal("1.0");

            // Factory should align timestamps automatically
            List<TransactionData> transactions = Arrays.asList(
                    new TransactionData(unalignedTime, price, volume, btcUsd)
            );

            Candlestick candle = OHLCDataFactory.createCandlestick(
                    btcUsd, TimeInterval.ONE_MINUTE, unalignedTime, transactions
            );

            // Should be aligned to minute boundary
            Instant expectedAligned = Instant.parse("2024-01-01T12:34:00Z");
            assertEquals(expectedAligned, candle.getTimestamp());
        }

        @Test
        @DisplayName("Should handle very large volume aggregations")
        void shouldHandleVeryLargeVolumeAggregations() {
            // Test handling of large volume numbers (institutional trades)
            BigDecimal largeVolume1 = new BigDecimal("1000000.123456789");
            BigDecimal largeVolume2 = new BigDecimal("2000000.987654321");
            Money openPrice = Money.of("50000.00", Currency.USD);
            Money closePrice = Money.of("51500.00", Currency.USD);

            List<TransactionData> largeVolumeTransactions = Arrays.asList(
                    new TransactionData(baseTimestamp, openPrice, largeVolume1, btcUsd),
                    new TransactionData(baseTimestamp.plus(30, ChronoUnit.SECONDS), closePrice, largeVolume2, btcUsd)
            );

            Candlestick candle = OHLCDataFactory.createCandlestick(
                    btcUsd, oneMinute, baseTimestamp, largeVolumeTransactions
            );

            BigDecimal expectedTotal = largeVolume1.add(largeVolume2);
            assertEquals(0, expectedTotal.compareTo(candle.getVolume()));
        }

        @Test
        @DisplayName("Should validate chart data conversion precision")
        void shouldValidateChartDataConversionPrecision() {
            // Test that converting to ChartData doesn't lose significant precision
            Money precisePrice = Money.of("50000.123456", Currency.USD);
            BigDecimal volume = new BigDecimal("1.5");

            Candlestick preciseCandle = new Candlestick(
                    "precise", btcUsd, baseTimestamp, oneMinute,
                    precisePrice, precisePrice, precisePrice, precisePrice, volume
            );

            ChartData chartData = preciseCandle.toChartData();

            // Should maintain reasonable precision (within double limits)
            assertEquals(50000.123456, chartData.getOpen(), 0.000001);
            assertEquals(50000.123456, chartData.getHigh(), 0.000001);
            assertEquals(50000.123456, chartData.getLow(), 0.000001);
            assertEquals(50000.123456, chartData.getClose(), 0.000001);
        }

        @Test
        @DisplayName("Should handle market gap scenarios")
        void shouldHandleMarketGapScenarios() {
            // Test gap up/down scenarios where open != previous close
            Money previousClose = Money.of("50000.00", Currency.USD);
            Money gapUpOpen = Money.of("52000.00", Currency.USD); // Gap up
            Money gapUpHigh = Money.of("53000.00", Currency.USD);
            Money gapUpLow = Money.of("51500.00", Currency.USD);
            Money gapUpClose = Money.of("52500.00", Currency.USD);
            BigDecimal volume = new BigDecimal("1.0");

            // Gap should be allowed - no validation between consecutive candles
            Candlestick gapCandle = new Candlestick(
                    "gap-candle", btcUsd, baseTimestamp, oneMinute,
                    gapUpOpen, gapUpHigh, gapUpLow, gapUpClose, volume
            );

            assertTrue(gapCandle.isBullish());
            // Open should be allowed to be different from any previous close
            assertNotEquals(previousClose, gapCandle.getOpen());
        }

        @Test
        @DisplayName("Should validate metadata calculation consistency")
        void shouldValidateMetadataCalculationConsistency() {
            // Test that metadata calculations are consistent with raw data
            OHLCData testOhlcData = new OHLCData("meta-test", btcUsd, oneMinute);

            Candlestick candle1 = createCandlestickWithPrices("c1", baseTimestamp, "50000", "52000", "49000", "51000");
            Candlestick candle2 = createCandlestickWithPrices("c2", baseTimestamp.plus(1, ChronoUnit.MINUTES), "51000", "53000", "50500", "52500");

            testOhlcData.addCandle(candle1);
            testOhlcData.addCandle(candle2);

            ChartMetadata metadata = OHLCDataFactory.createMetadata(testOhlcData);

            // Metadata should match direct calculations
            assertEquals(testOhlcData.getSymbol(), metadata.getSymbol());
            assertEquals(testOhlcData.getInterval(), metadata.getInterval());
            assertEquals(testOhlcData.size(), metadata.getTotalCandles());

            // Price range should match highest/lowest from candles
            assertEquals(testOhlcData.getHighestPrice().get(), metadata.getPriceRange().getMax());
            assertEquals(testOhlcData.getLowestPrice().get(), metadata.getPriceRange().getMin());
        }
    }

    // Helper methods
    private Candlestick createTestCandlestick(String id, Instant timestamp) {
        return new Candlestick(
                id, btcUsd, timestamp, oneMinute,
                Money.of("50000.00", Currency.USD),
                Money.of("51000.00", Currency.USD),
                Money.of("49500.00", Currency.USD),
                Money.of("50500.00", Currency.USD),
                new BigDecimal("1.0")
        );
    }

    private Candlestick createCandlestickWithPrices(String id, Instant timestamp,
                                                    String open, String high, String low, String close) {
        return new Candlestick(
                id, btcUsd, timestamp, oneMinute,
                Money.of(open, Currency.USD),
                Money.of(high, Currency.USD),
                Money.of(low, Currency.USD),
                Money.of(close, Currency.USD),
                new BigDecimal("1.0")
        );
    }

    private Candlestick createCandlestickWithVolume(String id, Instant timestamp, String volume) {
        return new Candlestick(
                id, btcUsd, timestamp, oneMinute,
                Money.of("50000.00", Currency.USD),
                Money.of("51000.00", Currency.USD),
                Money.of("49500.00", Currency.USD),
                Money.of("50500.00", Currency.USD),
                new BigDecimal(volume)
        );
    }
    @Nested
    @DisplayName("Advanced Business Rule Tests")
    class AdvancedBusinessRuleTests {

        @Test
        @DisplayName("Should enforce proper chronological order in transactions")
        void shouldEnforceProperChronologicalOrderInTransactions() {
            // Test that out-of-order transactions within same interval are handled correctly
            List<TransactionData> outOfOrderTransactions = Arrays.asList(
                    new TransactionData(baseTimestamp.plus(45, ChronoUnit.SECONDS), Money.of("51000.00", Currency.USD), new BigDecimal("0.3"), btcUsd), // Last
                    new TransactionData(baseTimestamp, Money.of("50000.00", Currency.USD), new BigDecimal("0.5"), btcUsd), // First
                    new TransactionData(baseTimestamp.plus(30, ChronoUnit.SECONDS), Money.of("52000.00", Currency.USD), new BigDecimal("0.2"), btcUsd)  // Middle
            );

            Candlestick candle = OHLCDataFactory.createCandlestick(btcUsd, oneMinute, baseTimestamp, outOfOrderTransactions);

            // Should use chronological order for OHLC: first transaction = open, last = close
            assertEquals(Money.of("50000.00", Currency.USD), candle.getOpen()); // Earliest timestamp
            assertEquals(Money.of("51000.00", Currency.USD), candle.getClose()); // Latest timestamp
            assertEquals(Money.of("52000.00", Currency.USD), candle.getHigh()); // Highest price
            assertEquals(Money.of("50000.00", Currency.USD), candle.getLow()); // Lowest price
        }

        @Test
        @DisplayName("Should validate cross-interval transaction boundaries")
        void shouldValidateCrossIntervalTransactionBoundaries() {
            // Test transactions exactly at interval boundaries
            Instant intervalBoundary = TimeInterval.ONE_MINUTE.alignTimestamp(baseTimestamp);
            Instant nextBoundary = intervalBoundary.plus(1, ChronoUnit.MINUTES);

            List<TransactionData> boundaryTransactions = Arrays.asList(
                    new TransactionData(intervalBoundary, Money.of("50000.00", Currency.USD), new BigDecimal("0.5"), btcUsd),
                    new TransactionData(nextBoundary, Money.of("51000.00", Currency.USD), new BigDecimal("0.3"), btcUsd)
            );

            OHLCData result = OHLCDataFactory.createFromTransactions(btcUsd, oneMinute, boundaryTransactions);

            // Should create separate candles for each interval
            assertEquals(2, result.size());

            List<Candlestick> candles = result.getAllCandles();
            assertEquals(intervalBoundary, candles.get(0).getTimestamp());
            assertEquals(nextBoundary, candles.get(1).getTimestamp());
        }

        @Test
        @DisplayName("Should handle weekend and holiday gaps")
        void shouldHandleWeekendAndHolidayGaps() {
            // Test large time gaps between candles (weekend, holidays)
            Instant friday = Instant.parse("2024-01-05T16:00:00Z"); // Friday 4 PM
            Instant monday = Instant.parse("2024-01-08T09:00:00Z"); // Monday 9 AM

            OHLCData testOhlcData = new OHLCData("weekend-test", btcUsd, oneMinute);

            Candlestick fridayCandle = createTestCandlestick("friday", friday);
            Candlestick mondayCandle = createTestCandlestick("monday", monday);

            testOhlcData.addCandle(fridayCandle);
            testOhlcData.addCandle(mondayCandle);

            // Should handle large gaps without issues
            assertEquals(2, testOhlcData.size());

            Optional<DateRange> dateRange = testOhlcData.getDateRange();
            assertTrue(dateRange.isPresent());
            assertTrue(dateRange.get().getDuration().toDays() >= 2); // Multi-day gap
        }

        @Test
        @DisplayName("Should validate extreme market conditions")
        void shouldValidateExtremeMarketConditions() {
            // Test market crash scenario (90% drop)
            Money precrashPrice = Money.of("50000.00", Currency.USD);
            Money crashPrice = Money.of("5000.00", Currency.USD); // 90% drop

            // Should allow extreme but valid price movements
            assertDoesNotThrow(() -> new Candlestick(
                    "crash-candle", btcUsd, baseTimestamp, oneMinute,
                    precrashPrice, precrashPrice, crashPrice, crashPrice, new BigDecimal("100000.0") // High volume during crash
            ));

            // Test market pump scenario (1000% increase)
            Money pumpPrice = Money.of("500000.00", Currency.USD); // 10x increase

            assertDoesNotThrow(() -> new Candlestick(
                    "pump-candle", btcUsd, baseTimestamp, oneMinute,
                    precrashPrice, pumpPrice, precrashPrice, pumpPrice, new BigDecimal("50000.0")
            ));
        }

        @Test
        @DisplayName("Should handle empty intervals in continuous data")
        void shouldHandleEmptyIntervalsInContinuousData() {
            // Test scenario where some intervals have no transactions
            List<TransactionData> sparseTransactions = Arrays.asList(
                    new TransactionData(baseTimestamp, Money.of("50000.00", Currency.USD), new BigDecimal("0.5"), btcUsd),
                    // No transactions at baseTimestamp + 1 minute
                    new TransactionData(baseTimestamp.plus(2, ChronoUnit.MINUTES), Money.of("51000.00", Currency.USD), new BigDecimal("0.3"), btcUsd),
                    // No transactions at baseTimestamp + 3 minutes
                    new TransactionData(baseTimestamp.plus(4, ChronoUnit.MINUTES), Money.of("52000.00", Currency.USD), new BigDecimal("0.7"), btcUsd)
            );

            OHLCData result = OHLCDataFactory.createFromTransactions(btcUsd, oneMinute, sparseTransactions);

            // Should only create candles for intervals with transactions
            assertEquals(3, result.size()); // Only intervals with actual transactions

            List<Candlestick> candles = result.getAllCandles();
            assertEquals(baseTimestamp, candles.get(0).getTimestamp());
            assertEquals(baseTimestamp.plus(2, ChronoUnit.MINUTES), candles.get(1).getTimestamp());
            assertEquals(baseTimestamp.plus(4, ChronoUnit.MINUTES), candles.get(2).getTimestamp());
        }

        @Test
        @DisplayName("Should validate different currency pairs consistency")
        void shouldValidateDifferentCurrencyPairsConsistency() {
            // Test EUR-based symbol
            Symbol btcEur = Symbol.btcEur();
            Money eurPrice = Money.of("45000.00", Currency.EUR);

            OHLCData eurOhlcData = new OHLCData("btc-eur-ohlc", btcEur, oneMinute);

            Candlestick eurCandle = new Candlestick(
                    "eur-candle", btcEur, baseTimestamp, oneMinute,
                    eurPrice, eurPrice.add(Money.of("1000", Currency.EUR)),
                    eurPrice.subtract(Money.of("500", Currency.EUR)), eurPrice,
                    new BigDecimal("1.0")
            );

            // Should work with different quote currencies
            assertDoesNotThrow(() -> eurOhlcData.addCandle(eurCandle));

            // But mixing currencies should fail
            Money usdPrice = Money.of("50000.00", Currency.USD);

            assertThrows(IllegalArgumentException.class, () -> new Candlestick(
                    "mixed-currency", btcEur, baseTimestamp, oneMinute,
                    usdPrice, usdPrice, usdPrice, usdPrice, new BigDecimal("1.0")
            ));
        }

        @Test
        @DisplayName("Should handle microsecond-level transaction timing")
        void shouldHandleMicrosecondLevelTransactionTiming() {
            // Test very close transaction times (microseconds apart)
            List<TransactionData> microTransactions = Arrays.asList(
                    new TransactionData(baseTimestamp, Money.of("50000.00", Currency.USD), new BigDecimal("0.1"), btcUsd),
                    new TransactionData(baseTimestamp.plusNanos(1000), Money.of("50001.00", Currency.USD), new BigDecimal("0.2"), btcUsd), // 1 microsecond later
                    new TransactionData(baseTimestamp.plusNanos(2000), Money.of("49999.00", Currency.USD), new BigDecimal("0.3"), btcUsd)  // 2 microseconds later
            );

            Candlestick candle = OHLCDataFactory.createCandlestick(btcUsd, oneMinute, baseTimestamp, microTransactions);

            // Should handle nanosecond precision correctly
            assertEquals(Money.of("50000.00", Currency.USD), candle.getOpen()); // First
            assertEquals(Money.of("49999.00", Currency.USD), candle.getClose()); // Last
            assertEquals(Money.of("50001.00", Currency.USD), candle.getHigh()); // Highest
            assertEquals(Money.of("49999.00", Currency.USD), candle.getLow()); // Lowest
        }

        @Test
        @DisplayName("Should validate reasonable interval progression")
        void shouldValidateReasonableIntervalProgression() {
            // Test that all interval types work correctly
            for (TimeInterval interval : TimeInterval.values()) {
                Instant aligned = interval.alignTimestamp(baseTimestamp);
                Instant next = interval.getNextTimestamp(aligned);
                Instant previous = interval.getPreviousTimestamp(aligned);

                // Next should be exactly one interval later
                assertEquals(interval.getDuration(), Duration.between(aligned, next));

                // Previous should be exactly one interval earlier
                assertEquals(interval.getDuration(), Duration.between(previous, aligned));

                // Alignment should be idempotent
                assertEquals(aligned, interval.alignTimestamp(aligned));
            }
        }

        @Test
        @DisplayName("Should enforce business rule: no future timestamps")
        void shouldEnforceNoFutureTimestamps() {
            // Test that future timestamps are handled appropriately
            Instant futureTime = Instant.now().plus(1, ChronoUnit.DAYS);

            // Should allow future timestamps (for testing, simulations, etc.)
            // But document that in production, validation might be needed
            assertDoesNotThrow(() -> new TransactionData(
                    futureTime, Money.of("50000.00", Currency.USD), new BigDecimal("1.0"), btcUsd
            ));

            assertDoesNotThrow(() -> createTestCandlestick("future-candle", futureTime));
        }

        @Test
        @DisplayName("Should validate aggregation consistency across different intervals")
        void shouldValidateAggregationConsistencyAcrossDifferentIntervals() {
            // Test that 1-minute data aggregates correctly into 5-minute data
            List<TransactionData> transactions = Arrays.asList(
                    // Minute 1
                    new TransactionData(baseTimestamp, Money.of("50000.00", Currency.USD), new BigDecimal("1.0"), btcUsd),
                    new TransactionData(baseTimestamp.plus(30, ChronoUnit.SECONDS), Money.of("50500.00", Currency.USD), new BigDecimal("0.5"), btcUsd),
                    // Minute 2
                    new TransactionData(baseTimestamp.plus(60, ChronoUnit.SECONDS), Money.of("51000.00", Currency.USD), new BigDecimal("2.0"), btcUsd),
                    // Minute 3
                    new TransactionData(baseTimestamp.plus(120, ChronoUnit.SECONDS), Money.of("51500.00", Currency.USD), new BigDecimal("1.5"), btcUsd),
                    // Minute 4
                    new TransactionData(baseTimestamp.plus(180, ChronoUnit.SECONDS), Money.of("52000.00", Currency.USD), new BigDecimal("0.8"), btcUsd),
                    // Minute 5
                    new TransactionData(baseTimestamp.plus(240, ChronoUnit.SECONDS), Money.of("51800.00", Currency.USD), new BigDecimal("1.2"), btcUsd)
            );

            // Create 1-minute candles
            OHLCData oneMinuteData = OHLCDataFactory.createFromTransactions(btcUsd, TimeInterval.ONE_MINUTE, transactions);

            // Create 5-minute candles from same data
            OHLCData fiveMinuteData = OHLCDataFactory.createFromTransactions(btcUsd, TimeInterval.FIVE_MINUTES, transactions);

            // 1-minute should have multiple candles
            assertTrue(oneMinuteData.size() > 1);

            // 5-minute should aggregate all into one candle (within 5-minute window)
            assertEquals(1, fiveMinuteData.size());

            Candlestick fiveMinCandle = fiveMinuteData.getAllCandles().get(0);

            // 5-minute candle should have:
            // - Open: first transaction price
            // - Close: last transaction price
            // - High: highest across all transactions
            // - Low: lowest across all transactions
            // - Volume: sum of all volumes
            assertEquals(Money.of("50000.00", Currency.USD), fiveMinCandle.getOpen());
            assertEquals(Money.of("51800.00", Currency.USD), fiveMinCandle.getClose());
            assertEquals(Money.of("52000.00", Currency.USD), fiveMinCandle.getHigh());
            assertEquals(Money.of("50000.00", Currency.USD), fiveMinCandle.getLow());
            assertEquals(0, new BigDecimal("7.0").compareTo(fiveMinCandle.getVolume())); // Sum of all volumes
        }

    }

}