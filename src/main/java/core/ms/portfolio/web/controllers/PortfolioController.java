package core.ms.portfolio.web.controllers;

import core.ms.order.web.dto.response.ApiResponse;
import core.ms.portfolio.application.dto.command.CreatePortfolioCommand;
import core.ms.portfolio.application.dto.command.DepositCashCommand;
import core.ms.portfolio.application.dto.command.WithdrawCashCommand;
import core.ms.portfolio.application.dto.query.CashBalanceDTO;
import core.ms.portfolio.application.dto.query.PortfolioDTO;
import core.ms.portfolio.application.dto.query.PortfolioOperationResultDTO;
import core.ms.portfolio.application.services.PortfolioApplicationService;
import core.ms.portfolio.domain.ports.inbound.PortfolioSnapshot;
import core.ms.portfolio.web.dto.request.CashOperationRequest;
import core.ms.portfolio.web.dto.request.CreatePortfolioRequest;
import core.ms.portfolio.web.mappers.PortfolioWebMapper;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
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
        try {
            CreatePortfolioCommand command = webMapper.toCommand(request);
            PortfolioOperationResultDTO result = portfolioService.createPortfolio(command);

            if (result.isSuccess()) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("Portfolio created successfully", result.getPortfolioId()));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(result.getMessage(), result.getErrors()));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create portfolio: " + e.getMessage()));
        }
    }

    @GetMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<PortfolioDTO>> getPortfolio(
            @PathVariable @NotBlank String portfolioId) {
        try {
            Optional<PortfolioDTO> portfolio = portfolioService.findPortfolioByIdAsDTO(portfolioId);

            return portfolio.map(dto -> ResponseEntity.ok(
                    ApiResponse.success("Portfolio retrieved", dto)
            )).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Portfolio not found")));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve portfolio: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<String>> deletePortfolio(
            @PathVariable @NotBlank String portfolioId) {
        try {
            portfolioService.deletePortfolio(portfolioId);

            return ResponseEntity.ok(ApiResponse.success("Portfolio deleted successfully", portfolioId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete portfolio: " + e.getMessage()));
        }
    }

    // ===== CASH OPERATIONS =====

    @PostMapping("/{portfolioId}/cash/deposit")
    public ResponseEntity<ApiResponse<String>> depositCash(
            @PathVariable @NotBlank String portfolioId,
            @Valid @RequestBody CashOperationRequest request) {
        try {
            DepositCashCommand command = new DepositCashCommand(
                    portfolioId, request.getAmount(), request.getCurrency()
            );
            PortfolioOperationResultDTO result = portfolioService.depositCash(command);

            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(result.getMessage(),request.getAmount().toString()));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(result.getMessage()));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to deposit cash: " + e.getMessage()));
        }
    }

    @PostMapping("/{portfolioId}/cash/withdraw")
    public ResponseEntity<ApiResponse<String>> withdrawCash(
            @PathVariable @NotBlank String portfolioId,
            @Valid @RequestBody CashOperationRequest request) {
        try {
            WithdrawCashCommand command = new WithdrawCashCommand(
                    portfolioId, request.getAmount(), request.getCurrency()
            );
            PortfolioOperationResultDTO result = portfolioService.withdrawCash(command);

            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(result.getMessage(),request.getAmount().toString()+" "+request.getCurrency()));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(result.getMessage()));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to withdraw cash: " + e.getMessage()));
        }
    }

    @GetMapping("/{portfolioId}/cash/{currency}")
    public ResponseEntity<ApiResponse<CashBalanceDTO>> getCashBalance(
            @PathVariable @NotBlank String portfolioId,
            @PathVariable @NotNull Currency currency) {
        try {
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
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get cash balance: " + e.getMessage()));
        }
    }

    // ===== PORTFOLIO SNAPSHOT =====

    @GetMapping("/{portfolioId}/snapshot")
    public ResponseEntity<ApiResponse<PortfolioSnapshot>> getPortfolioSnapshot(
            @PathVariable @NotBlank String portfolioId) {
        try {
            PortfolioSnapshot snapshot = portfolioService.getPortfolioSnapshot(portfolioId);
            return ResponseEntity.ok(ApiResponse.success("Portfolio snapshot retrieved", snapshot));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get portfolio snapshot: " + e.getMessage()));
        }
    }

    // ===== MAINTENANCE =====

    @PostMapping("/cleanup-reservations")
    public ResponseEntity<ApiResponse<String>> cleanupExpiredReservations() {
        try {
            portfolioService.cleanupExpiredReservations();
            return ResponseEntity.ok(ApiResponse.success("Expired reservations cleaned up", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to cleanup reservations: " + e.getMessage()));
        }
    }
}