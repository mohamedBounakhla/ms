package core.ms.order_book.web.controllers;

import core.ms.order_book.application.services.OrderSynchronizationService;
import core.ms.order_book.web.dto.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/order-books/cache")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Cache Management", description = "Order cache management endpoints")
public class OrderCacheController {

    private static final Logger logger = LoggerFactory.getLogger(OrderCacheController.class);

    @Autowired
    private OrderSynchronizationService syncService;

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<OrderSynchronizationService.CacheStatistics>> getCacheStatistics() {
        var stats = syncService.getCacheStatistics();
        return ResponseEntity.ok(ApiResponse.success("Cache statistics retrieved", stats));
    }

    @PostMapping("/clear")
    public ResponseEntity<ApiResponse<String>> clearCache() {
        logger.warn("Admin clearing order cache");
        syncService.clearCache();
        return ResponseEntity.ok(ApiResponse.success("Cache cleared successfully",null));
    }

    @PostMapping("/cleanup")
    public ResponseEntity<ApiResponse<String>> cleanupExpired() {
        syncService.cleanupExpiredEntries();
        return ResponseEntity.ok(ApiResponse.success("Expired entries cleaned up",null));
    }

    @PostMapping("/refresh/{symbol}")
    public ResponseEntity<ApiResponse<String>> refreshSymbol(@PathVariable String symbol) {
        logger.info("Admin refreshing cache for symbol: {}", symbol);

        try {
            var domainSymbol = createSymbol(symbol);
            syncService.refreshOrdersForSymbol(domainSymbol);
            return ResponseEntity.ok(ApiResponse.success("Symbol cache refreshed", symbol));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to refresh: " + e.getMessage()));
        }
    }

    private core.ms.shared.money.Symbol createSymbol(String symbolCode) {
        return switch (symbolCode.toUpperCase()) {
            case "BTC" -> core.ms.shared.money.Symbol.btcUsd();
            case "ETH" -> core.ms.shared.money.Symbol.ethUsd();
            case "EURUSD" -> core.ms.shared.money.Symbol.eurUsd();
            case "GBPUSD" -> core.ms.shared.money.Symbol.gbpUsd();
            default -> throw new IllegalArgumentException("Unsupported symbol: " + symbolCode);
        };
    }
}