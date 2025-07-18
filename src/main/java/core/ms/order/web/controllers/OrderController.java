package core.ms.order.web.controllers;

import core.ms.order.application.dto.query.OrderDTO;
import core.ms.order.application.services.OrderApplicationService;
import core.ms.order.web.dto.request.CancelPartialOrderRequest;
import core.ms.order.web.dto.request.CreateBuyOrderRequest;
import core.ms.order.web.dto.request.CreateSellOrderRequest;
import core.ms.order.web.dto.request.UpdateOrderPriceRequest;
import core.ms.order.web.dto.response.ApiResponse;
import core.ms.order.web.mappers.OrderWebMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/orders")
@Validated
public class OrderController {

    @Autowired
    private OrderApplicationService orderApplicationService;

    @Autowired
    private OrderWebMapper orderWebMapper;

    // ===== ORDER CREATION =====

    @PostMapping("/buy")
    public ResponseEntity<ApiResponse<String>> createBuyOrder(@Valid @RequestBody CreateBuyOrderRequest request) {
        try {
            var command = orderWebMapper.toCommand(request);
            var result = orderApplicationService.createBuyOrder(command);
            var response = orderWebMapper.toApiResponse(result);

            HttpStatus status = response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/sell")
    public ResponseEntity<ApiResponse<String>> createSellOrder(@Valid @RequestBody CreateSellOrderRequest request) {
        try {
            var command = orderWebMapper.toCommand(request);
            var result = orderApplicationService.createSellOrder(command);
            var response = orderWebMapper.toApiResponse(result);

            HttpStatus status = response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    // ===== ORDER MANAGEMENT =====

    @PutMapping("/{orderId}/price")
    public ResponseEntity<ApiResponse<String>> updateOrderPrice(
            @PathVariable @NotBlank(message = "Order ID cannot be blank") String orderId,
            @Valid @RequestBody UpdateOrderPriceRequest request) {
        try {
            var command = orderWebMapper.toCommand(orderId, request);
            var result = orderApplicationService.updateOrderPrice(command);
            var response = orderWebMapper.toApiResponse(result);

            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<String>> cancelOrder(
            @PathVariable @NotBlank(message = "Order ID cannot be blank") String orderId) {
        try {
            var command = orderWebMapper.toCommand(orderId);
            var result = orderApplicationService.cancelOrder(command);
            var response = orderWebMapper.toApiResponse(result);

            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    @PutMapping("/{orderId}/cancel-partial")
    public ResponseEntity<ApiResponse<String>> cancelPartialOrder(
            @PathVariable @NotBlank(message = "Order ID cannot be blank") String orderId,
            @Valid @RequestBody CancelPartialOrderRequest request) {
        try {
            var command = orderWebMapper.toCommand(orderId, request);
            var result = orderApplicationService.cancelPartialOrder(command);
            var response = orderWebMapper.toApiResponse(result);

            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    // ===== ORDER QUERIES =====

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderById(
            @PathVariable @NotBlank(message = "Order ID cannot be blank") String orderId) {
        try {
            Optional<OrderDTO> orderOpt = orderApplicationService.findOrderByIdAsDTO(orderId);

            return orderOpt.map(orderDTO -> ResponseEntity.ok(ApiResponse.success("Order found", orderDTO))).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Order not found")));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/symbol/{symbolCode}")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getOrdersBySymbol(
            @PathVariable @NotBlank(message = "Symbol code cannot be blank")
            @Pattern(regexp = "^[A-Z0-9]+$", message = "Symbol code must contain only uppercase letters and numbers")
            String symbolCode) {
        try {
            List<OrderDTO> orders = orderApplicationService.findOrdersBySymbolAsDTO(symbolCode);
            return ResponseEntity.ok(ApiResponse.success("Orders found", orders));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getOrdersByStatus(
            @PathVariable @NotBlank(message = "Status cannot be blank")
            @Pattern(regexp = "^(PENDING|PARTIAL|FILLED|CANCELLED)$", message = "Invalid order status")
            String status) {
        try {
            List<OrderDTO> orders = orderApplicationService.findOrdersByStatusAsDTO(status);
            return ResponseEntity.ok(ApiResponse.success("Orders found", orders));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/buy/symbol/{symbolCode}")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getBuyOrdersBySymbol(
            @PathVariable @NotBlank(message = "Symbol code cannot be blank")
            @Pattern(regexp = "^[A-Z0-9]+$", message = "Symbol code must contain only uppercase letters and numbers")
            String symbolCode) {
        try {
            List<OrderDTO> orders = orderApplicationService.findBuyOrdersBySymbolAsDTO(symbolCode);
            return ResponseEntity.ok(ApiResponse.success("Buy orders found", orders));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/sell/symbol/{symbolCode}")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getSellOrdersBySymbol(
            @PathVariable @NotBlank(message = "Symbol code cannot be blank")
            @Pattern(regexp = "^[A-Z0-9]+$", message = "Symbol code must contain only uppercase letters and numbers")
            String symbolCode) {
        try {
            List<OrderDTO> orders = orderApplicationService.findSellOrdersBySymbolAsDTO(symbolCode);
            return ResponseEntity.ok(ApiResponse.success("Sell orders found", orders));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }
}
