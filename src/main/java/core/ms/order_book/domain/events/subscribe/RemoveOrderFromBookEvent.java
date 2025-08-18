package core.ms.order_book.domain.events.subscribe;

import core.ms.shared.events.DomainEvent;

import java.time.LocalDateTime;

public class RemoveOrderFromBookEvent implements DomainEvent {
    private final String commandId;
    private final String orderId;
    private final String symbolCode;
    private final String reason;
    private final LocalDateTime occurredAt;

    public RemoveOrderFromBookEvent(String commandId, String orderId,
                                    String symbolCode, String reason) {
        this.commandId = commandId;
        this.orderId = orderId;
        this.symbolCode = symbolCode;
        this.reason = reason;
        this.occurredAt = LocalDateTime.now();
    }

    // Getters
    public String getCommandId() { return commandId; }
    public String getOrderId() { return orderId; }
    public String getSymbolCode() { return symbolCode; }
    public String getReason() { return reason; }
    @Override
    public LocalDateTime getOccurredAt() { return occurredAt; }
}