package core.ms.order_book.web.controllers;


import core.ms.order_book.application.services.OrderBookApplicationService;
import core.ms.order_book.web.mappers.OrderBookWebMapper;
import core.ms.shared.money.Symbol;
import core.ms.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/order-books")
@Validated
@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
@Tag(name = "Order Book Admin", description = "Internal order book management")
public class OrderBookInternalController {

    private static final Logger logger = LoggerFactory.getLogger(OrderBookInternalController.class);

    @Autowired
    private OrderBookApplicationService orderBookService;

    // ===== ORDER BOOK LIFECYCLE (Admin Only) =====

    @PostMapping("/create/{symbol}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> createOrderBook(
            @PathVariable
            @NotBlank(message = "Symbol cannot be blank")
            @Pattern(regexp = "^[A-Z0-9]+$", message = "Invalid symbol format")
            String symbol) {

        logger.info("Admin creating order book for symbol: {}", symbol);

        try {
            var domainSymbol = Symbol.createFromCode(symbol);
            var result = orderBookService.createOrderBook(domainSymbol);

            if (result.isSuccess()) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("Order book created", symbol));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(result.getMessage()));
            }
        } catch (Exception e) {
            logger.error("Failed to create order book", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal error: " + e.getMessage()));
        }
    }

    @DeleteMapping("/remove/{symbol}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> removeOrderBook(
            @PathVariable
            @NotBlank(message = "Symbol cannot be blank")
            @Pattern(regexp = "^[A-Z0-9]+$", message = "Invalid symbol format")
            String symbol) {

        logger.warn("Admin removing order book for symbol: {}", symbol);

        try {
            var domainSymbol = Symbol.createFromCode(symbol);
            var result = orderBookService.removeOrderBook(domainSymbol);

            if (result.isSuccess()) {
                return ResponseEntity.ok(
                        ApiResponse.success("Order book removed", symbol));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(result.getMessage()));
            }
        } catch (Exception e) {
            logger.error("Failed to remove order book", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal error: " + e.getMessage()));
        }
    }

    // ===== MAINTENANCE OPERATIONS =====

    @PostMapping("/cleanup-inactive")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<Integer>> cleanupInactiveOrders() {
        logger.info("System cleanup of inactive orders initiated");

        try {
            int removedCount = orderBookService.cleanupInactiveOrders();
            logger.info("Cleanup complete - Removed {} inactive orders", removedCount);

            return ResponseEntity.ok(
                    ApiResponse.success(
                            String.format("Removed %d inactive orders", removedCount),
                            removedCount));
        } catch (Exception e) {
            logger.error("Cleanup failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Cleanup failed: " + e.getMessage()));
        }
    }

    // ===== TESTING ENDPOINTS (Development Only) =====

    @PostMapping("/force-match/{symbol}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> forceProcessMatches(
            @PathVariable
            @NotBlank(message = "Symbol cannot be blank")
            @Pattern(regexp = "^[A-Z0-9]+$", message = "Invalid symbol format")
            String symbol) {

        logger.warn("Admin forcing match processing for symbol: {}", symbol);

        try {
            var domainSymbol = Symbol.createFromCode(symbol);

            // Black box operation - fire and forget
            orderBookService.processPendingMatches(domainSymbol);

            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Match processing triggered for " + symbol,
                            symbol));
        } catch (Exception e) {
            logger.error("Force match processing failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Processing failed: " + e.getMessage()));
        }
    }
}