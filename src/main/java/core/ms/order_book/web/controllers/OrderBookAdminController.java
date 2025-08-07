package core.ms.order_book.web.controllers;

import core.ms.order.web.dto.response.ApiResponse;
import core.ms.order_book.application.dto.command.CreateSnapshotCommand;
import core.ms.order_book.application.dto.query.OrderBookSnapshotDTO;
import core.ms.order_book.application.services.OrderBookSnapshotService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin/order-books")
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class OrderBookAdminController {

    @Autowired
    private OrderBookSnapshotService snapshotService;

    // ===== SNAPSHOT OPERATIONS =====

    @PostMapping("/snapshot")
    public ResponseEntity<ApiResponse<String>> createSnapshotAll() {
        try {
            snapshotService.performSnapshot();
            // Use success(message, data) to return ApiResponse<String>
            return ResponseEntity.ok(
                    ApiResponse.success("OrderBook snapshots created successfully", "SUCCESS")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Snapshot failed: " + e.getMessage()));
        }
    }

    @PostMapping("/snapshot/{symbolCode}")
    public ResponseEntity<ApiResponse<String>> createSnapshotForSymbol(
            @PathVariable @NotBlank(message = "Symbol code cannot be blank")
            @Pattern(regexp = "^[A-Z0-9]+$", message = "Symbol code must contain only uppercase letters and numbers")
            String symbolCode) {
        try {
            snapshotService.createSnapshot(symbolCode);
            return ResponseEntity.ok(
                    ApiResponse.success("Snapshot created for " + symbolCode, symbolCode)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Snapshot failed: " + e.getMessage()));
        }
    }

    @PostMapping("/snapshot/create")
    public ResponseEntity<ApiResponse<String>> createSnapshot(
            @Valid @RequestBody CreateSnapshotCommand command) {
        try {
            snapshotService.createSnapshot(command);
            return ResponseEntity.ok(
                    ApiResponse.success("Snapshot created for " + command.getSymbolCode(), command.getSymbolCode())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Snapshot failed: " + e.getMessage()));
        }
    }

    // ===== SNAPSHOT QUERIES =====

    @GetMapping("/snapshot/{symbolCode}/latest")
    public ResponseEntity<ApiResponse<OrderBookSnapshotDTO>> getLatestSnapshot(
            @PathVariable @NotBlank(message = "Symbol code cannot be blank")
            @Pattern(regexp = "^[A-Z0-9]+$", message = "Symbol code must contain only uppercase letters and numbers")
            String symbolCode) {
        try {
            Optional<OrderBookSnapshotDTO> snapshot = snapshotService.getLatestSnapshot(symbolCode);

            return snapshot.map(dto -> ResponseEntity.ok(
                    ApiResponse.success("Latest snapshot retrieved", dto)
            )).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("No snapshot found for " + symbolCode)));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve snapshot: " + e.getMessage()));
        }
    }

    @GetMapping("/snapshot/{symbolCode}/history")
    public ResponseEntity<ApiResponse<List<OrderBookSnapshotDTO>>> getSnapshotHistory(
            @PathVariable @NotBlank(message = "Symbol code cannot be blank")
            @Pattern(regexp = "^[A-Z0-9]+$", message = "Symbol code must contain only uppercase letters and numbers")
            String symbolCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        try {
            List<OrderBookSnapshotDTO> snapshots =
                    snapshotService.getSnapshotHistory(symbolCode, from, to);

            return ResponseEntity.ok(
                    ApiResponse.success("Snapshot history retrieved", snapshots)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve history: " + e.getMessage()));
        }
    }

    // ===== RESTORE OPERATIONS =====

    @PostMapping("/restore/{symbolCode}")
    public ResponseEntity<ApiResponse<String>> restoreFromSnapshot(
            @PathVariable @NotBlank(message = "Symbol code cannot be blank")
            @Pattern(regexp = "^[A-Z0-9]+$", message = "Symbol code must contain only uppercase letters and numbers")
            String symbolCode) {
        try {
            snapshotService.restoreFromLatestSnapshot(symbolCode);
            return ResponseEntity.ok(
                    ApiResponse.success("OrderBook restored from snapshot for " + symbolCode, symbolCode)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Restore failed: " + e.getMessage()));
        }
    }
}