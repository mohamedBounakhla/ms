package core.ms.order_book.domain.events.subscribe;

import core.ms.shared.events.DomainEvent;

import java.time.LocalDateTime;

public class AddOrderToBookEvent implements DomainEvent {
    private final String commandId;
    private final String orderId;
    private final String symbolCode;
    private final LocalDateTime occurredAt;

    public AddOrderToBookEvent(String commandId, String orderId, String symbolCode) {
        this.commandId = commandId;
        this.orderId = orderId;
        this.symbolCode = symbolCode;
        this.occurredAt = LocalDateTime.now();
    }

    // Getters
    public String getCommandId() { return commandId; }
    public String getOrderId() { return orderId; }
    public String getSymbolCode() { return symbolCode; }
    @Override
    public LocalDateTime getOccurredAt() { return occurredAt; }
}