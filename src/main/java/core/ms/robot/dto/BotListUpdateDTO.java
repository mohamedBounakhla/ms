package core.ms.robot.dto;

import java.time.LocalDateTime;

public class BotListUpdateDTO {
    private String action; // "CREATED", "REMOVED", "STARTED", "STOPPED"
    private String botId;
    private String botName;
    private BotStatusDTO botStatus; // Full status for CREATED action
    private LocalDateTime timestamp;

    // Constructor for bot creation
    public static BotListUpdateDTO botCreated(String botId, String botName, BotStatusDTO status) {
        BotListUpdateDTO update = new BotListUpdateDTO();
        update.action = "CREATED";
        update.botId = botId;
        update.botName = botName;
        update.botStatus = status;
        update.timestamp = LocalDateTime.now();
        return update;
    }

    // Constructor for bot removal
    public static BotListUpdateDTO botRemoved(String botId, String botName) {
        BotListUpdateDTO update = new BotListUpdateDTO();
        update.action = "REMOVED";
        update.botId = botId;
        update.botName = botName;
        update.timestamp = LocalDateTime.now();
        return update;
    }

    // Constructor for status changes
    public static BotListUpdateDTO botStatusChanged(String botId, String botName, String newStatus) {
        BotListUpdateDTO update = new BotListUpdateDTO();
        update.action = newStatus; // "STARTED" or "STOPPED"
        update.botId = botId;
        update.botName = botName;
        update.timestamp = LocalDateTime.now();
        return update;
    }

    // Getters and Setters
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getBotId() { return botId; }
    public void setBotId(String botId) { this.botId = botId; }

    public String getBotName() { return botName; }
    public void setBotName(String botName) { this.botName = botName; }

    public BotStatusDTO getBotStatus() { return botStatus; }
    public void setBotStatus(BotStatusDTO botStatus) { this.botStatus = botStatus; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}