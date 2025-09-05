package core.ms.robot.service;

import core.ms.robot.dto.BotStatusDTO;
import core.ms.robot.dto.BotTradeEventDTO;
import core.ms.robot.dto.BotListUpdateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BotWebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(BotWebSocketService.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast individual bot status update
     */
    public void broadcastBotStatus(BotStatusDTO status) {
        try {
            // Send to bot-specific channel
            messagingTemplate.convertAndSend(
                    "/topic/bots/" + status.getBotId(),
                    status
            );

            // Also send to general updates channel
            messagingTemplate.convertAndSend(
                    "/topic/bots/updates",
                    status
            );

            logger.debug("Broadcasted status update for bot: {}", status.getBotId());

        } catch (Exception e) {
            logger.error("Failed to broadcast bot status", e);
        }
    }

    /**
     * Broadcast trade execution event
     */
    public void broadcastTradeEvent(BotTradeEventDTO tradeEvent) {
        try {
            // Send to bot-specific trade channel
            messagingTemplate.convertAndSend(
                    "/topic/bots/" + tradeEvent.getBotId() + "/trades",
                    tradeEvent
            );

            // Send to general trades channel
            messagingTemplate.convertAndSend(
                    "/topic/bots/trades",
                    tradeEvent
            );

            logger.info("Broadcasted trade event: Bot {} executed {} {} @ {}",
                    tradeEvent.getBotId(),
                    tradeEvent.getAction(),
                    tradeEvent.getQuantity(),
                    tradeEvent.getPrice()
            );

        } catch (Exception e) {
            logger.error("Failed to broadcast trade event", e);
        }
    }

    /**
     * Broadcast bot list changes (created/removed)
     */
    public void broadcastBotListUpdate(BotListUpdateDTO update) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/bots/list",
                    update
            );

            logger.info("Broadcasted bot list update: {} bot {}",
                    update.getAction(), update.getBotId());

        } catch (Exception e) {
            logger.error("Failed to broadcast bot list update", e);
        }
    }

    /**
     * Broadcast bulk status updates (for efficiency)
     */
    public void broadcastBulkStatusUpdate(List<BotStatusDTO> statuses) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/bots/bulk-updates",
                    statuses
            );

            logger.debug("Broadcasted bulk update for {} bots", statuses.size());

        } catch (Exception e) {
            logger.error("Failed to broadcast bulk status update", e);
        }
    }

    /**
     * Send error notification for a specific bot
     */
    public void broadcastBotError(String botId, String error) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/bots/" + botId + "/errors",
                    new BotErrorDTO(botId, error)
            );

            logger.error("Broadcasted error for bot {}: {}", botId, error);

        } catch (Exception e) {
            logger.error("Failed to broadcast bot error", e);
        }
    }

    // Inner class for error messages
    public static class BotErrorDTO {
        private final String botId;
        private final String error;
        private final long timestamp;

        public BotErrorDTO(String botId, String error) {
            this.botId = botId;
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }

        public String getBotId() { return botId; }
        public String getError() { return error; }
        public long getTimestamp() { return timestamp; }
    }
}