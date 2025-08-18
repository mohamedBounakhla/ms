package core.ms.order_book.domain.events.subscribe;

import core.ms.shared.events.DomainEvent;

import java.time.LocalDateTime;

public class ProcessMatchesEvent implements DomainEvent {
    private final String commandId;
    private final String symbolCode;
    private final LocalDateTime occurredAt;

    public ProcessMatchesEvent(String commandId, String symbolCode) {
        this.commandId = commandId;
        this.symbolCode = symbolCode;  // ✅ FIXED - removed the orderId line
        this.occurredAt = LocalDateTime.now();
    }

    // Getters
    public String getCommandId() { return commandId; }
    public String getSymbolCode() { return symbolCode; }
    @Override
    public LocalDateTime getOccurredAt() { return occurredAt; }
}