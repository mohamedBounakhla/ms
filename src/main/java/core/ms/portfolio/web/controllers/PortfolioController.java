package core.ms.portfolio.web.controllers;

import core.ms.portfolio.application.dto.command.*;
import core.ms.portfolio.application.dto.query.CashBalanceDTO;
import core.ms.portfolio.application.dto.query.PortfolioDTO;
import core.ms.portfolio.application.dto.query.PortfolioOperationResultDTO;
import core.ms.portfolio.application.services.PortfolioApplicationService;
import core.ms.portfolio.domain.ports.inbound.PortfolioSnapshot;
import core.ms.portfolio.web.dto.request.CashOperationRequest;
import core.ms.portfolio.web.dto.request.CreatePortfolioRequest;
import core.ms.portfolio.web.dto.request.PlaceOrderRequest;
import core.ms.portfolio.web.mappers.PortfolioWebMapper;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.web.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/portfolios")
@Validated
public class PortfolioController {

    @Autowired
    private PortfolioApplicationService portfolioService;

    @Autowired
    private PortfolioWebMapper webMapper;

    // ===== PORTFOLIO MANAGEMENT =====

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createPortfolio(
            @Valid @RequestBody CreatePortfolioRequest request) {
        CreatePortfolioCommand command = webMapper.toCommand(request);
        PortfolioOperationResultDTO result = portfolioService.createPortfolio(command);

        if (result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Portfolio created successfully", result.getPortfolioId()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(result.getMessage(), result.getErrors()));
    }

    @GetMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<PortfolioDTO>> getPortfolio(
            @PathVariable @NotBlank String portfolioId) {
        Optional<PortfolioDTO> portfolio = portfolioService.findPortfolioByIdAsDTO(portfolioId);

        return portfolio.map(dto -> ResponseEntity.ok(
                ApiResponse.success("Portfolio retrieved", dto)
        )).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Portfolio not found")));
    }

    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<String>> deletePortfolio(
            @PathVariable @NotBlank String portfolioId) {
        portfolioService.deletePortfolio(portfolioId);
        return ResponseEntity.ok(ApiResponse.success("Portfolio deleted successfully", portfolioId));
    }

    // ===== ORDER PLACEMENT (SAGA INITIATION) =====

    @PostMapping("/{portfolioId}/orders/buy")
    public ResponseEntity<ApiResponse<String>> placeBuyOrder(
            @PathVariable @NotBlank String portfolioId,
            @Valid @RequestBody PlaceOrderRequest request) {

        PlaceBuyOrderCommand command = new PlaceBuyOrderCommand();
        command.setPortfolioId(portfolioId);
        command.setSymbolCode(request.getSymbolCode());
        command.setPrice(request.getPrice());
        command.setCurrency(request.getCurrency());
        command.setQuantity(request.getQuantity());

        PortfolioOperationResultDTO result = portfolioService.placeBuyOrder(command);

        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(result.getMessage(), portfolioId));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(result.getMessage()));
    }

    @PostMapping("/{portfolioId}/orders/sell")
    public ResponseEntity<ApiResponse<String>> placeSellOrder(
            @PathVariable @NotBlank String portfolioId,
            @Valid @RequestBody PlaceOrderRequest request) {

        PlaceSellOrderCommand command = new PlaceSellOrderCommand();
        command.setPortfolioId(portfolioId);
        command.setSymbolCode(request.getSymbolCode());
        command.setPrice(request.getPrice());
        command.setCurrency(request.getCurrency());
        command.setQuantity(request.getQuantity());

        PortfolioOperationResultDTO result = portfolioService.placeSellOrder(command);

        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(result.getMessage(), portfolioId));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(result.getMessage()));
    }

    // ===== CASH OPERATIONS =====

    @PostMapping("/{portfolioId}/cash/deposit")
    public ResponseEntity<ApiResponse<String>> depositCash(
            @PathVariable @NotBlank String portfolioId,
            @Valid @RequestBody CashOperationRequest request) {

        DepositCashCommand command = new DepositCashCommand(
                portfolioId, request.getAmount(), request.getCurrency()
        );
        PortfolioOperationResultDTO result = portfolioService.depositCash(command);

        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(result.getMessage(),
                    request.getAmount().toString() + " " + request.getCurrency()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(result.getMessage()));
    }

    @PostMapping("/{portfolioId}/cash/withdraw")
    public ResponseEntity<ApiResponse<String>> withdrawCash(
            @PathVariable @NotBlank String portfolioId,
            @Valid @RequestBody CashOperationRequest request) {

        WithdrawCashCommand command = new WithdrawCashCommand(
                portfolioId, request.getAmount(), request.getCurrency()
        );
        PortfolioOperationResultDTO result = portfolioService.withdrawCash(command);

        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(result.getMessage(),
                    request.getAmount().toString() + " " + request.getCurrency()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(result.getMessage()));
    }

    @GetMapping("/{portfolioId}/cash/{currency}")
    public ResponseEntity<ApiResponse<CashBalanceDTO>> getCashBalance(
            @PathVariable @NotBlank String portfolioId,
            @PathVariable @NotNull Currency currency) {

        Money available = portfolioService.getAvailableCash(portfolioId, currency);
        Money total = portfolioService.getTotalCash(portfolioId, currency);
        Money reserved = total.subtract(available);

        CashBalanceDTO balance = new CashBalanceDTO(
                currency,
                available.getAmount(),
                reserved.getAmount(),
                total.getAmount()
        );

        return ResponseEntity.ok(ApiResponse.success("Cash balance retrieved", balance));
    }

    // ===== PORTFOLIO SNAPSHOT =====

    @GetMapping("/{portfolioId}/snapshot")
    public ResponseEntity<ApiResponse<PortfolioSnapshot>> getPortfolioSnapshot(
            @PathVariable @NotBlank String portfolioId) {
        PortfolioSnapshot snapshot = portfolioService.getPortfolioSnapshot(portfolioId);
        return ResponseEntity.ok(ApiResponse.success("Portfolio snapshot retrieved", snapshot));
    }

    // ===== MAINTENANCE =====

    @PostMapping("/cleanup-reservations")
    public ResponseEntity<ApiResponse<Void>> cleanupExpiredReservations() {
        portfolioService.cleanupExpiredReservations();
        return ResponseEntity.ok(ApiResponse.success("Expired reservations cleaned up", null));
    }

    @GetMapping("/{portfolioId}/reservations/count")
    public ResponseEntity<ApiResponse<Integer>> getActiveReservationsCount(
            @PathVariable @NotBlank String portfolioId) {
        int count = portfolioService.getActiveReservationsCount(portfolioId);
        return ResponseEntity.ok(ApiResponse.success("Active reservations count", count));
    }
}