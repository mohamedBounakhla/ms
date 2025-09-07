package core.ms.robot.controller;

import core.ms.robot.dto.BotStatusDTO;
import core.ms.robot.config.BotConfig;
import core.ms.robot.service.BotService;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import core.ms.shared.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/bots")
public class BotController {

    @Autowired
    private BotService botService;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createBot(@Valid @RequestBody BotConfig config) {
        try {
            String botId = botService.createAndStartBot(config);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Bot created successfully", botId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to create bot: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BotStatusDTO>>> getAllBots() {
        List<BotStatusDTO> bots = botService.getAllBotStatuses();
        return ResponseEntity.ok(ApiResponse.success("Bots retrieved", bots));
    }
    @PostMapping("/tick-all")
    public ResponseEntity<String> tickAllBots() {
        botService.tickAllBots();
        return ResponseEntity.ok("Ticked all bots");
    }
    @GetMapping("/{botId}")
    public ResponseEntity<ApiResponse<BotStatusDTO>> getBotStatus(@PathVariable String botId) {
        return botService.getBotStatus(botId)
                .map(status -> ResponseEntity.ok(ApiResponse.success("Bot status retrieved", status)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Bot not found")));
    }

    @PostMapping("/{botId}/start")
    public ResponseEntity<ApiResponse<String>> startBot(@PathVariable String botId) {
        try {
            botService.startBot(botId);
            return ResponseEntity.ok(ApiResponse.success("Bot started", botId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to start bot: " + e.getMessage()));
        }
    }
    @PostMapping("/tick")
    public ResponseEntity<ApiResponse<String>> manualTick() {
        botService.manualTickAllBots();
        return ResponseEntity.ok(ApiResponse.success("Manually ticked all bots", "OK"));
    }
    @GetMapping("/test-price/{symbol}")
    public ResponseEntity<String> testPrice(@PathVariable String symbol) {
        // Call through to the service to test
        return ResponseEntity.ok(botService.testMarketPrice(symbol));
    }
    @PostMapping("/{botId}/stop")
    public ResponseEntity<ApiResponse<String>> stopBot(@PathVariable String botId) {
        try {
            botService.stopBot(botId);
            return ResponseEntity.ok(ApiResponse.success("Bot stopped", botId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to stop bot: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{botId}")
    public ResponseEntity<ApiResponse<String>> removeBot(@PathVariable String botId) {
        try {
            botService.removeBot(botId);
            return ResponseEntity.ok(ApiResponse.success("Bot removed", botId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to remove bot: " + e.getMessage()));
        }
    }

    @PostMapping("/stop-all")
    public ResponseEntity<ApiResponse<String>> stopAllBots() {
        botService.stopAllBots();
        return ResponseEntity.ok(ApiResponse.success("All bots stopped", null));
    }
}