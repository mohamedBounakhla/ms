package core.ms.order_book.domain.events.subscribe;

import core.ms.shared.events.DomainEvent;

import java.time.LocalDateTime;

public class UpdateOrderInBookEvent implements DomainEvent {
    private final String commandId;
    private final String orderId;
    private final String symbolCode;
    private final String updateType; // "QUANTITY_CHANGE", "PRICE_CHANGE", "STATUS_CHANGE"
    private final LocalDateTime occurredAt;

    public UpdateOrderInBookEvent(String commandId, String orderId,
                                  String symbolCode, String updateType) {
        this.commandId = commandId;
        this.orderId = orderId;
        this.symbolCode = symbolCode;
        this.updateType = updateType;
        this.occurredAt = LocalDateTime.now();
    }

    // Getters
    public String getCommandId() { return commandId; }
    public String getOrderId() { return orderId; }
    public String getSymbolCode() { return symbolCode; }
    public String getUpdateType() { return updateType; }
    @Override
    public LocalDateTime getOccurredAt() { return occurredAt; }
}