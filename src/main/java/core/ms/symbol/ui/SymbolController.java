package core.ms.symbol.ui;

import core.ms.shared.money.AssetType;
import core.ms.shared.web.ApiResponse;
import core.ms.symbol.dto.CreateSymbolRequest;
import core.ms.symbol.dto.SymbolDTO;
import core.ms.symbol.dto.UpdateSymbolRequest;
import core.ms.symbol.service.SymbolService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/symbols")
public class SymbolController {

    @Autowired
    private SymbolService symbolService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SymbolDTO>>> getAllSymbols(
            @RequestParam(required = false) AssetType type) {

        List<SymbolDTO> symbols;
        if (type != null) {
            symbols = symbolService.getSymbolsByType(type)
                    .stream()
                    .map(symbol -> {
                        SymbolDTO dto = new SymbolDTO();
                        dto.setCode(symbol.getCode());
                        dto.setName(symbol.getName());
                        dto.setAssetType(symbol.getType());
                        dto.setBaseCurrency(symbol.getBaseCurrency());
                        dto.setQuoteCurrency(symbol.getQuoteCurrency());
                        dto.setActive(true);
                        return dto;
                    })
                    .toList();
        } else {
            symbols = symbolService.getAllSymbolDTOs();
        }

        return ResponseEntity.ok(
                ApiResponse.success("Symbols retrieved successfully", symbols)
        );
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<SymbolDTO>> getSymbol(@PathVariable String code) {
        return symbolService.getSymbolDTO(code)
                .map(dto -> ResponseEntity.ok(
                        ApiResponse.success("Symbol retrieved successfully", dto)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Symbol not found: " + code)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SymbolDTO>> createSymbol(
            @Valid @RequestBody CreateSymbolRequest request) {
        try {
            SymbolDTO created = symbolService.createSymbol(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Symbol created successfully", created));
        } catch (SymbolService.SymbolAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to create symbol: " + e.getMessage()));
        }
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SymbolDTO>> updateSymbol(
            @PathVariable String code,
            @Valid @RequestBody UpdateSymbolRequest request) {
        try {
            SymbolDTO updated = symbolService.updateSymbol(code, request);
            return ResponseEntity.ok(
                    ApiResponse.success("Symbol updated successfully", updated));
        } catch (SymbolService.SymbolNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to update symbol: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateSymbol(@PathVariable String code) {
        try {
            symbolService.deactivateSymbol(code);
            return ResponseEntity.ok(
                    ApiResponse.success("Symbol deactivated successfully"));
        } catch (SymbolService.SymbolNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{code}/active")
    public ResponseEntity<ApiResponse<Boolean>> isSymbolActive(@PathVariable String code) {
        boolean active = symbolService.isSymbolActive(code);
        return ResponseEntity.ok(
                ApiResponse.success("Symbol status retrieved", active));
    }
}