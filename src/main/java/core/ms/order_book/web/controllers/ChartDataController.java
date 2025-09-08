package core.ms.order_book.web.controllers;

import core.ms.order_book.application.dto.query.CandlestickDTO;
import core.ms.order_book.application.services.CandlestickService;
import core.ms.shared.web.ApiResponse;
import core.ms.utils.TimeInterval;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST API for chart data and OHLC candlesticks
 */
@RestController
@RequestMapping("/api/v1/charts")
@Validated
@Tag(name = "Chart Data", description = "OHLC candlestick data endpoints")
public class ChartDataController {

    private static final Logger logger = LoggerFactory.getLogger(ChartDataController.class);

    @Autowired
    private CandlestickService candlestickService;

    /**
     * Get OHLC candlestick data for a symbol
     */
    @GetMapping("/ohlc/{symbol}")
    @Operation(
            summary = "Get OHLC candlestick data",
            description = "Returns candlestick data for charting. Intervals: 1m, 5m, 15m, 1h, 1d"
    )
    public ResponseEntity<ApiResponse<List<CandlestickDTO>>> getOHLCData(
            @PathVariable
            @NotBlank(message = "Symbol cannot be blank")
            @Pattern(regexp = "^[A-Z0-9]+$", message = "Invalid symbol format")
            @Parameter(description = "Trading symbol (e.g., BTCUSD)")
            String symbol,

            @RequestParam
            @Pattern(regexp = "^(1m|5m|15m|1h|1d)$", message = "Invalid interval. Use: 1m, 5m, 15m, 1h, or 1d")
            @Parameter(description = "Time interval for candles")
            String interval,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "Start time (ISO format). Default: 24 hours ago")
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "End time (ISO format). Default: now")
            LocalDateTime to) {

        try {
            // Set defaults
            if (to == null) {
                to = LocalDateTime.now();
            }
            if (from == null) {
                // Default range based on interval
                from = getDefaultFromTime(to, interval);
            }

            // Validate time range
            if (from.isAfter(to)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("'from' time cannot be after 'to' time"));
            }

            logger.debug("OHLC request: {} {} from {} to {}", symbol, interval, from, to);

            // Convert interval and fetch data
            TimeInterval timeInterval = convertToTimeInterval(interval);
            List<CandlestickDTO> candles = candlestickService.getCandlesticks(
                    symbol, timeInterval, from, to
            );

            logger.info("Returning {} candles for {} {}", candles.size(), symbol, interval);

            return ResponseEntity.ok(
                    ApiResponse.success(
                            String.format("Retrieved %d candles", candles.size()),
                            candles
                    )
            );

        } catch (IllegalArgumentException e) {
            logger.error("Invalid parameter: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching OHLC data", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch chart data"));
        }
    }

    /**
     * Get the latest candle for a symbol
     */
    @GetMapping("/ohlc/{symbol}/latest")
    @Operation(
            summary = "Get latest candle",
            description = "Returns the most recent candle for a symbol and interval"
    )
    public ResponseEntity<ApiResponse<CandlestickDTO>> getLatestCandle(
            @PathVariable
            @NotBlank(message = "Symbol cannot be blank")
            @Pattern(regexp = "^[A-Z0-9]+$", message = "Invalid symbol format")
            String symbol,

            @RequestParam(defaultValue = "1m")
            @Pattern(regexp = "^(1m|5m|15m|1h|1d)$", message = "Invalid interval")
            String interval) {

        try {
            TimeInterval timeInterval = convertToTimeInterval(interval);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime from = now.minusMinutes(getIntervalMinutes(timeInterval));

            List<CandlestickDTO> candles = candlestickService.getCandlesticks(
                    symbol, timeInterval, from, now
            );

            if (candles.isEmpty()) {
                return ResponseEntity.ok(
                        ApiResponse.success("No data available", null)
                );
            }

            CandlestickDTO latest = candles.get(candles.size() - 1);
            return ResponseEntity.ok(
                    ApiResponse.success("Latest candle retrieved", latest)
            );

        } catch (Exception e) {
            logger.error("Error fetching latest candle", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch latest candle"));
        }
    }

    /**
     * Get supported intervals
     */
    @GetMapping("/intervals")
    @Operation(
            summary = "Get supported intervals",
            description = "Returns list of supported time intervals for candlesticks"
    )
    public ResponseEntity<ApiResponse<List<IntervalInfo>>> getSupportedIntervals() {
        List<IntervalInfo> intervals = List.of(
                new IntervalInfo("1m", "1 Minute", 1),
                new IntervalInfo("5m", "5 Minutes", 5),
                new IntervalInfo("15m", "15 Minutes", 15),
                new IntervalInfo("1h", "1 Hour", 60),
                new IntervalInfo("1d", "1 Day", 1440)
        );

        return ResponseEntity.ok(
                ApiResponse.success("Supported intervals", intervals)
        );
    }

    // Helper methods

    private TimeInterval convertToTimeInterval(String interval) {
        return switch (interval.toLowerCase()) {
            case "1m" -> TimeInterval.ONE_MINUTE;
            case "5m" -> TimeInterval.FIVE_MINUTES;
            case "15m" -> TimeInterval.FIFTEEN_MINUTES;
            case "1h" -> TimeInterval.ONE_HOUR;
            case "1d" -> TimeInterval.ONE_DAY;
            default -> throw new IllegalArgumentException("Invalid interval: " + interval);
        };
    }

    private LocalDateTime getDefaultFromTime(LocalDateTime to, String interval) {
        // Set sensible defaults based on interval
        return switch (interval.toLowerCase()) {
            case "1m" -> to.minusHours(2);      // 2 hours of 1-minute candles
            case "5m" -> to.minusHours(12);     // 12 hours of 5-minute candles
            case "15m" -> to.minusDays(2);      // 2 days of 15-minute candles
            case "1h" -> to.minusDays(7);       // 1 week of hourly candles
            case "1d" -> to.minusDays(30);      // 30 days of daily candles
            default -> to.minusDays(1);         // Default to 24 hours
        };
    }

    private int getIntervalMinutes(TimeInterval interval) {
        return switch (interval) {
            case ONE_MINUTE -> 1;
            case FIVE_MINUTES -> 5;
            case FIFTEEN_MINUTES -> 15;
            case ONE_HOUR -> 60;
            case ONE_DAY -> 1440;
        };
    }

    /**
     * DTO for interval information
     */
    public static class IntervalInfo {
        private String code;
        private String label;
        private int minutes;

        public IntervalInfo(String code, String label, int minutes) {
            this.code = code;
            this.label = label;
            this.minutes = minutes;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public int getMinutes() { return minutes; }
        public void setMinutes(int minutes) { this.minutes = minutes; }
    }
}