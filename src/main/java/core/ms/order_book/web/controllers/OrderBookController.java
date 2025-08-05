package core.ms.order_book.web.controllers;

import core.ms.order.web.dto.response.ApiResponse;
import core.ms.order_book.application.dto.command.AddOrderToBookCommand;
import core.ms.order_book.application.dto.command.CreateOrderBookCommand;
import core.ms.order_book.application.dto.query.MarketDepthDTO;
import core.ms.order_book.application.dto.query.MarketOverviewDTO;
import core.ms.order_book.application.dto.query.OrderBookOperationResultDTO;
import core.ms.order_book.application.services.OrderBookApplicationService;
import core.ms.order_book.web.mappers.OrderBookWebMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/order-books")
@Validated
public class OrderBookController {

    @Autowired
    private OrderBookApplicationService orderBookService;

    @Autowired
    private OrderBookWebMapper webMapper;

    // ===== ORDER BOOK MANAGEMENT =====

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createOrderBook(@Valid @RequestBody CreateOrderBookCommand command) {
        try {
            var symbol = webMapper.createSymbol(command.getSymbolCode());
            var result = orderBookService.createOrderBook(symbol);
            var response = webMapper.toApiResponse(result);

            HttpStatus status = response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{symbolCode}")
    public ResponseEntity<ApiResponse<String>> removeOrderBook(
            @PathVariable @NotBlank(message = "Symbol code cannot be blank")
            @Pattern(regexp = "^[A-Z0-9]+$", message = "Symbol code must contain only uppercase letters and numbers")
            String symbolCode) {
        try {
            var symbol = webMapper.createSymbol(symbolCode);
            var result = orderBookService.removeOrderBook(symbol);
            var response = webMapper.toApiResponse(result);

            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.NOT_FOUND;
            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    // ===== ORDER PROCESSING =====

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<OrderBookOperationResultDTO>> addOrderToBook(
            @Valid @RequestBody AddOrderToBookCommand command) {
        try {
            var order = webMapper.fetchOrder(command.getOrderId());
            var result = orderBookService.addOrderToBook(order);
            var dto = webMapper.toDTO(result);

            HttpStatus status = result.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(ApiResponse.success("Order processed", dto));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/process-matches/{symbolCode}")
    public ResponseEntity<ApiResponse<Integer>> processMatches(
            @PathVariable @NotBlank(message = "Symbol code cannot be blank")
            @Pattern(regexp = "^[A-Z0-9]+$", message = "Symbol code must contain only uppercase letters and numbers")
            String symbolCode) {
        try {
            var symbol = webMapper.createSymbol(symbolCode);
            var matchEvents = orderBookService.processPendingMatches(symbol);

            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Processed %d matches", matchEvents.size()),
                    matchEvents.size()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/process-all-matches")
    public ResponseEntity<ApiResponse<Integer>> processAllMatches() {
        try {
            var matchEvents = orderBookService.processAllPendingMatches();

            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Processed %d total matches", matchEvents.size()),
                    matchEvents.size()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    // ===== MARKET DATA QUERIES =====

    @GetMapping("/{symbolCode}/depth")
    public ResponseEntity<ApiResponse<MarketDepthDTO>> getMarketDepth(
            @PathVariable @NotBlank(message = "Symbol code cannot be blank")
            @Pattern(regexp = "^[A-Z0-9]+$", message = "Symbol code must contain only uppercase letters and numbers")
            String symbolCode,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int levels) {
        try {
            var symbol = webMapper.createSymbol(symbolCode);
            var marketDepth = orderBookService.getMarketDepth(symbol, levels);
            var dto = webMapper.toDTO(marketDepth);

            return ResponseEntity.ok(ApiResponse.success("Market depth retrieved", dto));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<MarketOverviewDTO>> getMarketOverview() {
        try {
            var overview = orderBookService.getMarketOverview();
            var dto = webMapper.toDTO(overview);

            return ResponseEntity.ok(ApiResponse.success("Market overview retrieved", dto));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    // ===== MAINTENANCE =====

    @PostMapping("/cleanup-inactive")
    public ResponseEntity<ApiResponse<Integer>> cleanupInactiveOrders() {
        try {
            int removedCount = orderBookService.cleanupInactiveOrders();

            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Removed %d inactive orders", removedCount),
                    removedCount));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }
}