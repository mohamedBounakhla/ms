package core.ms.order.domain.events.subscribe;

import core.ms.shared.events.DomainEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CancelPartialOrderEvent implements DomainEvent {
    private final String commandId;
    private final String orderId;
    private final BigDecimal quantityToCancel;
    private final String reason;
    private final LocalDateTime occurredAt;

    public CancelPartialOrderEvent(String commandId, String orderId,
                                   BigDecimal quantityToCancel, String reason) {
        this.commandId = commandId;
        this.orderId = orderId;
        this.quantityToCancel = quantityToCancel;
        this.reason = reason;
        this.occurredAt = LocalDateTime.now();
    }

    // Getters
    public String getCommandId() { return commandId; }
    public String getOrderId() { return orderId; }
    public BigDecimal getQuantityToCancel() { return quantityToCancel; }
    public String getReason() { return reason; }
    @Override
    public LocalDateTime getOccurredAt() { return occurredAt; }
}