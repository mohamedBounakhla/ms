package core.ms.robot.controller;

import core.ms.robot.dto.BotStatusDTO;
import core.ms.robot.service.BotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

@Controller
public class BotWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(BotWebSocketController.class);

    @Autowired
    private BotService botService;

    /**
     * Subscribe to all bot status updates
     */
    @SubscribeMapping("/topic/bots/updates")
    public List<BotStatusDTO> subscribeToAllBots() {
        logger.info("Client subscribed to all bot updates");
        return botService.getAllBotStatuses();
    }

    /**
     * Subscribe to specific bot updates
     */
    @SubscribeMapping("/topic/bots/{botId}")
    public BotStatusDTO subscribeToBotStatus(@DestinationVariable String botId) {
        logger.info("Client subscribed to bot: {}", botId);

        Optional<BotStatusDTO> status = botService.getBotStatus(botId);
        return status.orElse(null);
    }

    /**
     * Request current status of all bots (client-initiated)
     */
    @MessageMapping("/bots/status")
    @SendTo("/topic/bots/bulk-updates")
    public List<BotStatusDTO> getAllBotsStatus() {
        logger.debug("Client requested all bot statuses");
        return botService.getAllBotStatuses();
    }

    /**
     * Request status of specific bot (client-initiated)
     */
    @MessageMapping("/bots/{botId}/status")
    @SendTo("/topic/bots/{botId}")
    public BotStatusDTO getBotStatus(@DestinationVariable String botId) {
        logger.debug("Client requested status for bot: {}", botId);

        Optional<BotStatusDTO> status = botService.getBotStatus(botId);
        return status.orElse(null);
    }
}