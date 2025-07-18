package core.ms.order.web.controllers;

import core.ms.order.application.dto.query.TransactionDTO;
import core.ms.order.application.dto.query.TransactionStatisticsDTO;
import core.ms.order.application.services.TransactionApplicationService;
import core.ms.order.web.dto.request.CreateTransactionRequest;
import core.ms.order.web.dto.response.ApiResponse;
import core.ms.order.web.mappers.TransactionWebMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/transactions")
@Validated
public class TransactionController {

    @Autowired
    private TransactionApplicationService transactionApplicationService;

    @Autowired
    private TransactionWebMapper transactionWebMapper;

    // ===== TRANSACTION CREATION =====

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        try {
            var command = transactionWebMapper.toCommand(request);
            var result = transactionApplicationService.createTransaction(command);
            var response = transactionWebMapper.toApiResponse(result);

            HttpStatus status = response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    // ===== TRANSACTION QUERIES =====

    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionDTO>> getTransactionById(
            @PathVariable @NotBlank(message = "Transaction ID cannot be blank") String transactionId) {
        try {
            Optional<TransactionDTO> transactionOpt = transactionApplicationService.findTransactionByIdAsDTO(transactionId);

            return transactionOpt.map(transactionDTO -> ResponseEntity.ok(ApiResponse.success("Transaction found", transactionDTO))).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Transaction not found")));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getTransactionsByOrderId(
            @PathVariable @NotBlank(message = "Order ID cannot be blank") String orderId) {
        try {
            List<TransactionDTO> transactions = transactionApplicationService.findTransactionsByOrderIdAsDTO(orderId);
            return ResponseEntity.ok(ApiResponse.success("Transactions found", transactions));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/symbol/{symbolCode}")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getTransactionsBySymbol(
            @PathVariable @NotBlank(message = "Symbol code cannot be blank")
            @Pattern(regexp = "^[A-Z0-9]+$", message = "Symbol code must contain only uppercase letters and numbers")
            String symbolCode) {
        try {
            List<TransactionDTO> transactions = transactionApplicationService.findTransactionsBySymbolAsDTO(symbolCode);
            return ResponseEntity.ok(ApiResponse.success("Transactions found", transactions));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getTransactionsByDateRange(
            @RequestParam @NotNull(message = "Start date cannot be null")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @NotNull(message = "End date cannot be null")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            if (startDate.isAfter(endDate)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Start date must be before end date"));
            }

            List<TransactionDTO> transactions = transactionApplicationService.findTransactionsByDateRangeAsDTO(startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success("Transactions found", transactions));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    // ===== TRANSACTION ANALYTICS =====

    @GetMapping("/statistics/symbol/{symbolCode}")
    public ResponseEntity<ApiResponse<TransactionStatisticsDTO>> getTransactionStatistics(
            @PathVariable @NotBlank(message = "Symbol code cannot be blank")
            @Pattern(regexp = "^[A-Z0-9]+$", message = "Symbol code must contain only uppercase letters and numbers")
            String symbolCode) {
        try {
            TransactionStatisticsDTO statistics = transactionApplicationService.getTransactionStatisticsAsDTO(symbolCode);
            return ResponseEntity.ok(ApiResponse.success("Transaction statistics found", statistics));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }
}
