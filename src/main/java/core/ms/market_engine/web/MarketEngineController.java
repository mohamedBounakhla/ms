package core.ms.market_engine.web;

import core.ms.market_engine.application.dto.MarketStatusDTO;
import core.ms.market_engine.application.dto.OrderFlowDTO;
import core.ms.market_engine.application.services.MarketEngineApplicationService;
import core.ms.order.web.dto.response.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/market-engine")
public class MarketEngineController {

    @Autowired
    private MarketEngineApplicationService marketEngineService;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<MarketStatusDTO>> getMarketStatus() {
        MarketStatusDTO status = marketEngineService.getMarketStatus();
        return ResponseEntity.ok(ApiResponse.success("Market status retrieved", status));
    }

    @GetMapping("/order-flow")
    public ResponseEntity<ApiResponse<OrderFlowDTO>> getOrderFlow() {
        OrderFlowDTO flow = marketEngineService.getOrderFlow();
        return ResponseEntity.ok(ApiResponse.success("Order flow retrieved", flow));
    }

    @PostMapping("/manual-process")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> manualProcessOrder(
            @RequestParam String orderId,
            @RequestParam String portfolioId,
            @RequestParam String symbolCode) {

        try {
            marketEngineService.manualProcessOrder(orderId, portfolioId, symbolCode);
            return ResponseEntity.ok(ApiResponse.success("Order processing initiated", orderId));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to process order: " + e.getMessage()));
        }
    }
}