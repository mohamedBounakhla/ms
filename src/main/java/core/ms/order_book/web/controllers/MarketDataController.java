package core.ms.order_book.web.controllers;

import core.ms.order_book.application.dto.query.MarketDepthDTO;
import core.ms.order_book.application.dto.query.MarketOverviewDTO;
import core.ms.order_book.application.dto.query.OrderBookStatisticsDTO;
import core.ms.order_book.application.dto.query.OrderBookSummaryDTO;
import core.ms.order_book.application.services.OrderBookApplicationService;
import core.ms.order_book.web.mappers.OrderBookWebMapper;
import core.ms.shared.money.Symbol;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Public REST API for market data queries.
 * All endpoints are READ-ONLY and provide market information.
 * Real-time updates are available via WebSocket endpoints.
 */
@RestController
@RequestMapping("/api/v1/market-data")
@Validated
@Tag(name = "Market Data", description = "Public market data endpoints")
public class MarketDataController {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataController.class);

    @Autowired
    private OrderBookApplicationService orderBookService;

    @Autowired
    private OrderBookWebMapper webMapper;

    // ===== MARKET DEPTH =====

    @GetMapping("/depth/{symbol}")
    @Operation(summary = "Get order book depth",
            description = "Returns bid/ask levels for a symbol")
    public ResponseEntity<MarketDepthDTO> getMarketDepth(
            @PathVariable
            @NotBlank(message = "Symbol cannot be blank")
            @Pattern(regexp = "^[A-Z0-9]+$", message = "Invalid symbol format")
            String symbol,
            @RequestParam(defaultValue = "10")
            @Min(1) @Max(50)
            int levels) {

        logger.debug("Market depth request - Symbol: {}, Levels: {}", symbol, levels);

        try {
            Symbol domainSymbol = Symbol.createFromCode(symbol);
            var marketDepth = orderBookService.getMarketDepth(domainSymbol, levels);
            var dto = webMapper.toDTO(marketDepth);

            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid symbol: {}", symbol);
            return ResponseEntity.badRequest().build();
        }
    }

    // ===== BEST PRICES =====

    @GetMapping("/ticker/{symbol}")
    @Operation(summary = "Get best bid/ask prices",
            description = "Returns current best prices and spread")
    public ResponseEntity<OrderBookStatisticsDTO> getTicker(
            @PathVariable
            @NotBlank(message = "Symbol cannot be blank")
            @Pattern(regexp = "^[A-Z0-9]+$", message = "Invalid symbol format")
            String symbol) {

        logger.debug("Ticker request - Symbol: {}", symbol);

        try {
            Symbol domainSymbol = Symbol.createFromCode(symbol);
            var stats = orderBookService.getOrderBookStatistics(domainSymbol);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid symbol: {}", symbol);
            return ResponseEntity.badRequest().build();
        }
    }

    // ===== MARKET OVERVIEW =====

    @GetMapping("/overview")
    @Operation(summary = "Get market overview",
            description = "Returns overview of all active markets")
    public ResponseEntity<MarketOverviewDTO> getMarketOverview() {
        logger.debug("Market overview request");

        var overview = orderBookService.getMarketOverview();
        var dto = webMapper.toDTO(overview);

        return ResponseEntity.ok(dto);
    }

    // ===== SYMBOL LIST =====

    @GetMapping("/symbols")
    @Operation(summary = "Get active symbols",
            description = "Returns list of all tradeable symbols")
    public ResponseEntity<String[]> getActiveSymbols() {
        logger.debug("Active symbols request");

        var symbols = orderBookService.getActiveSymbols();
        String[] symbolCodes = symbols.stream()
                .map(Symbol::getCode)
                .toArray(String[]::new);

        return ResponseEntity.ok(symbolCodes);
    }

    // ===== ORDER BOOK SUMMARY =====

    @GetMapping("/summary/{symbol}")
    @Operation(summary = "Get order book summary",
            description = "Returns condensed order book information")
    public ResponseEntity<OrderBookSummaryDTO> getOrderBookSummary(
            @PathVariable
            @NotBlank(message = "Symbol cannot be blank")
            @Pattern(regexp = "^[A-Z0-9]+$", message = "Invalid symbol format")
            String symbol) {

        logger.debug("Order book summary request - Symbol: {}", symbol);

        try {
            Symbol domainSymbol = Symbol.createFromCode(symbol);
            var summary = orderBookService.getOrderBookSummary(domainSymbol);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid symbol: {}", symbol);
            return ResponseEntity.badRequest().build();
        }
    }
}