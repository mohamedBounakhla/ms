package core.ms.OHLC.domain;


import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OHLC Domain Tests")
class OHLCDomainTest {

    private Symbol btcUsd;
    private TimeInterval oneMinute;
    private Instant baseTimestamp;
    private Money openPrice;
    private Money highPrice;
    private Money lowPrice;
    private Money closePrice;
    private BigDecimal volume;

    @BeforeEach
    void setUp() {
        btcUsd = Symbol.btcUsd();
        oneMinute = TimeInterval.ONE_MINUTE;
        baseTimestamp = Instant.parse("2024-01-01T12:00:00Z");
        openPrice = Money.of("50000.00", Currency.USD);
        highPrice = Money.of("52000.00", Currency.USD);
        lowPrice = Money.of("49500.00", Currency.USD);
        closePrice = Money.of("51500.00", Currency.USD);
        volume = new BigDecimal("1.5");
    }

    @Nested
    @DisplayName("TimeInterval Tests")
    class TimeIntervalTests {

        @Test
        @DisplayName("Should have correct codes and durations")
        void shouldHaveCorrectCodesAndDurations() {
            assertEquals("1m", TimeInterval.ONE_MINUTE.getCode());
            assertEquals("5m", TimeInterval.FIVE_MINUTES.getCode());
            assertEquals("1h", TimeInterval.ONE_HOUR.getCode());
            assertEquals("1d", TimeInterval.ONE_DAY.getCode());

            assertEquals(60_000L, TimeInterval.ONE_MINUTE.getMilliseconds());
            assertEquals(300_000L, TimeInterval.FIVE_MINUTES.getMilliseconds());
            assertEquals(3_600_000L, TimeInterval.ONE_HOUR.getMilliseconds());
        }

        @Test
        @DisplayName("Should compare intervals correctly")
        void shouldCompareIntervalsCorrectly() {
            assertTrue(TimeInterval.ONE_HOUR.isHigherThan(TimeInterval.ONE_MINUTE));
            assertTrue(TimeInterval.ONE_MINUTE.isLowerThan(TimeInterval.ONE_HOUR));
            assertFalse(TimeInterval.ONE_MINUTE.isHigherThan(TimeInterval.ONE_HOUR));
        }

        @Test
        @DisplayName("Should align timestamps correctly")
        void shouldAlignTimestampsCorrectly() {
            // Test 1-minute alignment
            Instant unaligned = Instant.parse("2024-01-01T12:34:45Z");
            Instant aligned = TimeInterval.ONE_MINUTE.alignTimestamp(unaligned);
            assertEquals(Instant.parse("2024-01-01T12:34:00Z"), aligned);

            // Test 1-hour alignment
            Instant hourAligned = TimeInterval.ONE_HOUR.alignTimestamp(unaligned);
            assertEquals(Instant.parse("2024-01-01T12:00:00Z"), hourAligned);
        }

        @Test
        @DisplayName("Should calculate next and previous timestamps")
        void shouldCalculateNextAndPreviousTimestamps() {
            Instant current = Instant.parse("2024-01-01T12:00:00Z");

            Instant next = TimeInterval.ONE_MINUTE.getNextTimestamp(current);
            assertEquals(Instant.parse("2024-01-01T12:01:00Z"), next);

            Instant previous = TimeInterval.ONE_MINUTE.getPreviousTimestamp(current);
            assertEquals(Instant.parse("2024-01-01T11:59:00Z"), previous);
        }
    }

    @Nested
    @DisplayName("Candlestick Tests")
    class CandlestickTests {

        @Test
        @DisplayName("Should create candlestick successfully")
        void shouldCreateCandlestickSuccessfully() {
            Candlestick candle = new Candlestick(
                    "candle-1", btcUsd, baseTimestamp, oneMinute,
                    openPrice, highPrice, lowPrice, closePrice, volume
            );

            assertEquals("candle-1", candle.getId());
            assertEquals(btcUsd, candle.getSymbol());
            assertEquals(baseTimestamp, candle.getTimestamp());
            assertEquals(oneMinute, candle.getInterval());
            assertEquals(openPrice, candle.getOpen());
            assertEquals(highPrice, candle.getHigh());
            assertEquals(lowPrice, candle.getLow());
            assertEquals(closePrice, candle.getClose());
            assertEquals(0, volume.compareTo(candle.getVolume()));
        }

        @Test
        @DisplayName("Should validate OHLC constraints")
        void shouldValidateOHLCConstraints() {
            // High < Low should throw
            assertThrows(IllegalArgumentException.class, () ->
                    new Candlestick("candle-1", btcUsd, baseTimestamp, oneMinute,
                            openPrice, Money.of("48000.00", Currency.USD), lowPrice, closePrice, volume)
            );

            // Open > High should throw
            assertThrows(IllegalArgumentException.class, () ->
                    new Candlestick("candle-1", btcUsd, baseTimestamp, oneMinute,
                            Money.of("53000.00", Currency.USD), highPrice, lowPrice, closePrice, volume)
            );

            // Close < Low should throw
            assertThrows(IllegalArgumentException.class, () ->
                    new Candlestick("candle-1", btcUsd, baseTimestamp, oneMinute,
                            openPrice, highPrice, lowPrice, Money.of("49000.00", Currency.USD), volume)
            );
        }

        @Test
        @DisplayName("Should validate currency consistency")
        void shouldValidateCurrencyConsistency() {
            Money eurPrice = Money.of("45000.00", Currency.EUR);

            assertThrows(IllegalArgumentException.class, () ->
                    new Candlestick("candle-1", btcUsd, baseTimestamp, oneMinute,
                            eurPrice, highPrice, lowPrice, closePrice, volume)
            );
        }

        @Test
        @DisplayName("Should validate volume constraints")
        void shouldValidateVolumeConstraints() {
            assertThrows(IllegalArgumentException.class, () ->
                    new Candlestick("candle-1", btcUsd, baseTimestamp, oneMinute,
                            openPrice, highPrice, lowPrice, closePrice, new BigDecimal("-1.0"))
            );
        }

        @Test
        @DisplayName("Should calculate business logic correctly")
        void shouldCalculateBusinessLogicCorrectly() {
            // Bullish candle (close > open)
            Candlestick bullishCandle = new Candlestick(
                    "candle-1", btcUsd, baseTimestamp, oneMinute,
                    openPrice, highPrice, lowPrice, closePrice, volume
            );

            assertTrue(bullishCandle.isBullish());
            assertFalse(bullishCandle.isBearish());
            assertFalse(bullishCandle.isDoji());

            // Bearish candle (close < open)
            Candlestick bearishCandle = new Candlestick(
                    "candle-2", btcUsd, baseTimestamp, oneMinute,
                    closePrice, highPrice, lowPrice, openPrice, volume
            );

            assertFalse(bearishCandle.isBullish());
            assertTrue(bearishCandle.isBearish());
            assertFalse(bearishCandle.isDoji());

            // Doji candle (close = open)
            Candlestick dojiCandle = new Candlestick(
                    "candle-3", btcUsd, baseTimestamp, oneMinute,
                    openPrice, highPrice, lowPrice, openPrice, volume
            );

            assertFalse(dojiCandle.isBullish());
            assertFalse(dojiCandle.isBearish());
            assertTrue(dojiCandle.isDoji());
        }

        @Test
        @DisplayName("Should calculate price metrics correctly")
        void shouldCalculatePriceMetricsCorrectly() {
            Candlestick candle = new Candlestick(
                    "candle-1", btcUsd, baseTimestamp, oneMinute,
                    openPrice, highPrice, lowPrice, closePrice, volume
            );

            // Body size = |close - open| = |51500 - 50000| = 1500
            Money expectedBodySize = Money.of("1500.00", Currency.USD);
            assertEquals(expectedBodySize, candle.getBodySize());

            // Range = high - low = 52000 - 49500 = 2500
            Money expectedRange = Money.of("2500.00", Currency.USD);
            assertEquals(expectedRange, candle.getRange());

            // Upper shadow = high - max(open, close) = 52000 - 51500 = 500
            Money expectedUpperShadow = Money.of("500.00", Currency.USD);
            assertEquals(expectedUpperShadow, candle.getUpperShadow());

            // Lower shadow = min(open, close) - low = 50000 - 49500 = 500
            Money expectedLowerShadow = Money.of("500.00", Currency.USD);
            assertEquals(expectedLowerShadow, candle.getLowerShadow());
        }

        @Test
        @DisplayName("Should calculate typical and weighted prices")
        void shouldCalculateTypicalAndWeightedPrices() {
            Candlestick candle = new Candlestick(
                    "candle-1", btcUsd, baseTimestamp, oneMinute,
                    openPrice, highPrice, lowPrice, closePrice, volume
            );

            // Typical price = (high + low + close) / 3 = (52000 + 49500 + 51500) / 3 = 51000
            Money expectedTypicalPrice = Money.of("51000.00", Currency.USD);
            assertEquals(expectedTypicalPrice, candle.getTypicalPrice());

            // Weighted price should be the same as typical price when volume > 0
            assertEquals(expectedTypicalPrice, candle.getWeightedPrice());
        }

        @Test
        @DisplayName("Should convert to chart data correctly")
        void shouldConvertToChartDataCorrectly() {
            Candlestick candle = new Candlestick(
                    "candle-1", btcUsd, baseTimestamp, oneMinute,
                    openPrice, highPrice, lowPrice, closePrice, volume
            );

            ChartData chartData = candle.toChartData();

            assertEquals(baseTimestamp.getEpochSecond(), chartData.getTime());
            assertEquals(50000.0, chartData.getOpen(), 0.01);
            assertEquals(52000.0, chartData.getHigh(), 0.01);
            assertEquals(49500.0, chartData.getLow(), 0.01);
            assertEquals(51500.0, chartData.getClose(), 0.01);
            assertEquals(1.5, chartData.getVolume(), 0.01);
        }

        @Test
        @DisplayName("Should have proper string representation")
        void shouldHaveProperStringRepresentation() {
            Candlestick candle = new Candlestick(
                    "candle-1", btcUsd, baseTimestamp, oneMinute,
                    openPrice, highPrice, lowPrice, closePrice, volume
            );

            String result = candle.toString();
            assertTrue(result.contains("candle-1"));
            assertTrue(result.contains("BTC"));
            assertTrue(result.contains("1m"));
            assertTrue(result.contains("📈")); // Bullish emoji
        }
    }

    @Nested
    @DisplayName("OHLC Value Object Tests")
    class OHLCTests {

        @Test
        @DisplayName("Should create OHLC successfully")
        void shouldCreateOHLCSuccessfully() {
            OHLC ohlc = new OHLC(openPrice, highPrice, lowPrice, closePrice);

            assertEquals(openPrice, ohlc.getOpen());
            assertEquals(highPrice, ohlc.getHigh());
            assertEquals(lowPrice, ohlc.getLow());
            assertEquals(closePrice, ohlc.getClose());
            assertTrue(ohlc.isValid());
        }

        @Test
        @DisplayName("Should validate OHLC constraints")
        void shouldValidateOHLCConstraints() {
            // High < Low should be invalid
            assertThrows(IllegalArgumentException.class, () ->
                    new OHLC(openPrice, Money.of("48000.00", Currency.USD), lowPrice, closePrice)
            );

            // Create invalid OHLC and check isValid()
            assertThrows(IllegalArgumentException.class, () ->
                    new OHLC(Money.of("53000.00", Currency.USD), highPrice, lowPrice, closePrice)
            );
        }
    }

    @Nested
    @DisplayName("DateRange Tests")
    class DateRangeTests {

        @Test
        @DisplayName("Should create date range successfully")
        void shouldCreateDateRangeSuccessfully() {
            Instant start = baseTimestamp;
            Instant end = baseTimestamp.plus(1, ChronoUnit.HOURS);

            DateRange range = new DateRange(start, end);

            assertEquals(start, range.getStart());
            assertEquals(end, range.getEnd());
            assertEquals(3600, range.getDuration().getSeconds());
        }

        @Test
        @DisplayName("Should validate date range constraints")
        void shouldValidateDateRangeConstraints() {
            Instant start = baseTimestamp;
            Instant end = baseTimestamp.minus(1, ChronoUnit.HOURS);

            assertThrows(IllegalArgumentException.class, () ->
                    new DateRange(start, end)
            );
        }

        @Test
        @DisplayName("Should check if contains timestamp")
        void shouldCheckIfContainsTimestamp() {
            Instant start = baseTimestamp;
            Instant end = baseTimestamp.plus(1, ChronoUnit.HOURS);
            DateRange range = new DateRange(start, end);

            assertTrue(range.contains(baseTimestamp));
            assertTrue(range.contains(baseTimestamp.plus(30, ChronoUnit.MINUTES)));
            assertTrue(range.contains(end));
            assertFalse(range.contains(baseTimestamp.minus(1, ChronoUnit.MINUTES)));
            assertFalse(range.contains(end.plus(1, ChronoUnit.MINUTES)));
        }

        @Test
        @DisplayName("Should check overlaps correctly")
        void shouldCheckOverlapsCorrectly() {
            DateRange range1 = new DateRange(
                    baseTimestamp,
                    baseTimestamp.plus(1, ChronoUnit.HOURS)
            );

            DateRange range2 = new DateRange(
                    baseTimestamp.plus(30, ChronoUnit.MINUTES),
                    baseTimestamp.plus(90, ChronoUnit.MINUTES)
            );

            DateRange range3 = new DateRange(
                    baseTimestamp.plus(2, ChronoUnit.HOURS),
                    baseTimestamp.plus(3, ChronoUnit.HOURS)
            );

            assertTrue(range1.overlaps(range2));
            assertTrue(range2.overlaps(range1));
            assertFalse(range1.overlaps(range3));
            assertFalse(range3.overlaps(range1));
        }
    }

    @Nested
    @DisplayName("PriceRange Tests")
    class PriceRangeTests {

        @Test
        @DisplayName("Should create price range successfully")
        void shouldCreatePriceRangeSuccessfully() {
            PriceRange range = new PriceRange(lowPrice, highPrice);

            assertEquals(lowPrice, range.getMin());
            assertEquals(highPrice, range.getMax());
            assertEquals(Money.of("2500.00", Currency.USD), range.getRange());
        }

        @Test
        @DisplayName("Should validate price range constraints")
        void shouldValidatePriceRangeConstraints() {
            // Min > Max should throw
            assertThrows(IllegalArgumentException.class, () ->
                    new PriceRange(highPrice, lowPrice)
            );

            // Different currencies should throw
            Money eurPrice = Money.of("45000.00", Currency.EUR);
            assertThrows(IllegalArgumentException.class, () ->
                    new PriceRange(eurPrice, highPrice)
            );
        }

        @Test
        @DisplayName("Should check if contains price")
        void shouldCheckIfContainsPrice() {
            PriceRange range = new PriceRange(lowPrice, highPrice);

            assertTrue(range.contains(lowPrice));
            assertTrue(range.contains(highPrice));
            assertTrue(range.contains(Money.of("51000.00", Currency.USD)));
            assertFalse(range.contains(Money.of("49000.00", Currency.USD)));
            assertFalse(range.contains(Money.of("53000.00", Currency.USD)));
        }

        @Test
        @DisplayName("Should calculate percentage position")
        void shouldCalculatePercentagePosition() {
            PriceRange range = new PriceRange(lowPrice, highPrice);

            // At minimum: 0%
            assertEquals(0, range.getPercentagePosition(lowPrice).compareTo(BigDecimal.ZERO));

            // At maximum: 100%
            assertEquals(0, range.getPercentagePosition(highPrice).compareTo(BigDecimal.ONE));

            // At midpoint: 50%
            Money midPrice = Money.of("51000.00", Currency.USD);
            BigDecimal percentage = range.getPercentagePosition(midPrice);
            assertEquals(0, percentage.compareTo(new BigDecimal("0.6000"))); // (51000-49500)/(52000-49500) = 0.6
        }
    }

    @Nested
    @DisplayName("VolumeRange Tests")
    class VolumeRangeTests {

        @Test
        @DisplayName("Should create volume range successfully")
        void shouldCreateVolumeRangeSuccessfully() {
            BigDecimal min = new BigDecimal("1.0");
            BigDecimal max = new BigDecimal("5.0");
            BigDecimal avg = new BigDecimal("3.0");

            VolumeRange range = new VolumeRange(min, max, avg);

            assertEquals(0, min.compareTo(range.getMin()));
            assertEquals(0, max.compareTo(range.getMax()));
            assertEquals(0, avg.compareTo(range.getAverage()));
            assertEquals(0, new BigDecimal("4.0").compareTo(range.getRange()));
        }

        @Test
        @DisplayName("Should validate volume range constraints")
        void shouldValidateVolumeRangeConstraints() {
            BigDecimal min = new BigDecimal("5.0");
            BigDecimal max = new BigDecimal("1.0");
            BigDecimal avg = new BigDecimal("3.0");

            // Min > Max should throw
            assertThrows(IllegalArgumentException.class, () ->
                    new VolumeRange(min, max, avg)
            );

            // Negative values should throw
            assertThrows(IllegalArgumentException.class, () ->
                    new VolumeRange(new BigDecimal("-1.0"), max, avg)
            );
        }
    }

    @Nested
    @DisplayName("Chart Data Tests")
    class ChartDataTests {

        @Test
        @DisplayName("Should create OHLC chart data successfully")
        void shouldCreateOHLCChartDataSuccessfully() {
            long time = baseTimestamp.getEpochSecond();
            OHLCChartData chartData = new OHLCChartData(time, 50000.0, 52000.0, 49500.0, 51500.0, 1.5);

            assertEquals(time, chartData.getTime());
            assertEquals(50000.0, chartData.getOpen(), 0.01);
            assertEquals(52000.0, chartData.getHigh(), 0.01);
            assertEquals(49500.0, chartData.getLow(), 0.01);
            assertEquals(51500.0, chartData.getClose(), 0.01);
            assertEquals(1.5, chartData.getVolume(), 0.01);
        }

        @Test
        @DisplayName("Should have proper string representation")
        void shouldHaveProperStringRepresentation() {
            long time = baseTimestamp.getEpochSecond();
            OHLCChartData chartData = new OHLCChartData(time, 50000.0, 52000.0, 49500.0, 51500.0, 1.5);

            String result = chartData.toString();
            assertTrue(result.contains("ChartData"));
            assertTrue(result.contains("50000"));
            assertTrue(result.contains("52000"));
        }
    }
}