package core.ms.order.domain.events.subscribe;

import core.ms.shared.events.DomainEvent;

import java.time.LocalDateTime;

public class CancelOrderEvent implements DomainEvent {
    private final String commandId;
    private final String orderId;
    private final String reason;
    private final LocalDateTime occurredAt;

    public CancelOrderEvent(String commandId, String orderId, String reason) {
        this.commandId = commandId;
        this.orderId = orderId;
        this.reason = reason;
        this.occurredAt = LocalDateTime.now();
    }

    // Getters
    public String getCommandId() { return commandId; }
    public String getOrderId() { return orderId; }
    public String getReason() { return reason; }
    @Override
    public LocalDateTime getOccurredAt() { return occurredAt; }
}