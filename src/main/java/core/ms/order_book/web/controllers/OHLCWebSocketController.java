package core.ms.order_book.web.controllers;

import core.ms.order_book.application.dto.query.CandlestickDTO;
import core.ms.order_book.application.services.CandlestickService;
import core.ms.utils.TimeInterval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WebSocket controller for OHLC/Candlestick data streaming
 */
@Controller
public class OHLCWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(OHLCWebSocketController.class);

    @Autowired
    private CandlestickService candlestickService;

    /**
     * Subscribe to real-time OHLC updates for a specific symbol and interval.
     * Client will receive updates whenever new transactions occur.
     *
     * Subscription paths:
     * - /topic/ohlc/{symbol}/1m - 1 minute candles
     * - /topic/ohlc/{symbol}/5m - 5 minute candles
     * - /topic/ohlc/{symbol}/15m - 15 minute candles
     * - /topic/ohlc/{symbol}/1h - 1 hour candles
     * - /topic/ohlc/{symbol}/1d - 1 day candles
     */
    @SubscribeMapping("/topic/ohlc/{symbol}/{interval}")
    public CandlestickDTO subscribeToOHLC(
            @DestinationVariable String symbol,
            @DestinationVariable String interval) {

        logger.info("Client subscribed to OHLC updates: {} {}", symbol, interval);

        try {
            TimeInterval timeInterval = parseInterval(interval);

            // Get the most recent candle to send immediately
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime from = now.minusMinutes(getIntervalMinutes(timeInterval));

            List<CandlestickDTO> candles = candlestickService.getCandlesticks(
                    symbol, timeInterval, from, now
            );

            if (!candles.isEmpty()) {
                // Return the most recent candle
                return candles.get(candles.size() - 1);
            }

            // Return empty candle if no data
            return new CandlestickDTO(
                    truncateToInterval(now, timeInterval),
                    null, null, null, null, null
            );

        } catch (Exception e) {
            logger.error("Failed to get initial OHLC data for subscription", e);
            return null;
        }
    }

    /**
     * Subscribe to completed candle notifications.
     * Clients receive a message when a candle period closes.
     *
     * Subscription paths:
     * - /topic/ohlc/complete/{symbol}/1m
     * - /topic/ohlc/complete/{symbol}/5m
     * - /topic/ohlc/complete/{symbol}/15m
     * - /topic/ohlc/complete/{symbol}/1h
     * - /topic/ohlc/complete/{symbol}/1d
     */
    @SubscribeMapping("/topic/ohlc/complete/{symbol}/{interval}")
    public void subscribeToCompletedCandles(
            @DestinationVariable String symbol,
            @DestinationVariable String interval) {

        logger.info("Client subscribed to completed candles: {} {}", symbol, interval);
        // No initial data needed - client will receive notifications when candles close
    }

    /**
     * Request historical OHLC data (client-initiated).
     *
     * Usage: Send message to /app/ohlc/{symbol}/{interval}/history
     * with a payload containing 'from' and 'to' timestamps
     */
    @MessageMapping("/ohlc/{symbol}/{interval}/history")
    @SendTo("/topic/ohlc/{symbol}/{interval}/history")
    public List<CandlestickDTO> getHistoricalOHLC(
            @DestinationVariable String symbol,
            @DestinationVariable String interval,
            OHLCHistoryRequest request) {

        logger.debug("Historical OHLC request: {} {} from {} to {}",
                symbol, interval, request.getFrom(), request.getTo());

        try {
            TimeInterval timeInterval = parseInterval(interval);

            LocalDateTime from = request.getFrom() != null ?
                    request.getFrom() : LocalDateTime.now().minusDays(1);
            LocalDateTime to = request.getTo() != null ?
                    request.getTo() : LocalDateTime.now();

            return candlestickService.getCandlesticks(symbol, timeInterval, from, to);

        } catch (Exception e) {
            logger.error("Failed to get historical OHLC data", e);
            return List.of();
        }
    }

    /**
     * Request latest candle snapshot (client-initiated).
     *
     * Usage: Send message to /app/ohlc/{symbol}/{interval}/latest
     */
    @MessageMapping("/ohlc/{symbol}/{interval}/latest")
    @SendTo("/topic/ohlc/{symbol}/{interval}/latest")
    public CandlestickDTO getLatestCandle(
            @DestinationVariable String symbol,
            @DestinationVariable String interval) {

        logger.debug("Latest candle request: {} {}", symbol, interval);

        try {
            TimeInterval timeInterval = parseInterval(interval);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime from = now.minusMinutes(getIntervalMinutes(timeInterval));

            List<CandlestickDTO> candles = candlestickService.getCandlesticks(
                    symbol, timeInterval, from, now
            );

            if (!candles.isEmpty()) {
                return candles.get(candles.size() - 1);
            }

            return null;

        } catch (Exception e) {
            logger.error("Failed to get latest candle", e);
            return null;
        }
    }

    // Helper methods

    private TimeInterval parseInterval(String interval) {
        return switch (interval.toLowerCase()) {
            case "1m" -> TimeInterval.ONE_MINUTE;
            case "5m" -> TimeInterval.FIVE_MINUTES;
            case "15m" -> TimeInterval.FIFTEEN_MINUTES;
            case "1h" -> TimeInterval.ONE_HOUR;
            case "1d" -> TimeInterval.ONE_DAY;
            default -> throw new IllegalArgumentException("Invalid interval: " + interval);
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

    private LocalDateTime truncateToInterval(LocalDateTime time, TimeInterval interval) {
        return switch (interval) {
            case ONE_MINUTE -> time.truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
            case FIVE_MINUTES -> time.truncatedTo(java.time.temporal.ChronoUnit.MINUTES)
                    .withMinute((time.getMinute() / 5) * 5);
            case FIFTEEN_MINUTES -> time.truncatedTo(java.time.temporal.ChronoUnit.MINUTES)
                    .withMinute((time.getMinute() / 15) * 15);
            case ONE_HOUR -> time.truncatedTo(java.time.temporal.ChronoUnit.HOURS);
            case ONE_DAY -> time.truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        };
    }

    /**
     * Request object for historical OHLC data
     */
    public static class OHLCHistoryRequest {
        private LocalDateTime from;
        private LocalDateTime to;

        public LocalDateTime getFrom() {
            return from;
        }

        public void setFrom(LocalDateTime from) {
            this.from = from;
        }

        public LocalDateTime getTo() {
            return to;
        }

        public void setTo(LocalDateTime to) {
            this.to = to;
        }
    }
}