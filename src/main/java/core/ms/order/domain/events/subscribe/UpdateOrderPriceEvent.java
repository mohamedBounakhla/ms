package core.ms.order.domain.events.subscribe;

import core.ms.shared.events.DomainEvent;
import core.ms.shared.money.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UpdateOrderPriceEvent implements DomainEvent {
    private final String commandId;
    private final String orderId;
    private final BigDecimal newPrice;
    private final Currency currency;
    private final  LocalDateTime occurredAt;

    public UpdateOrderPriceEvent(String commandId, String orderId,
                                 BigDecimal newPrice, Currency currency) {
        this.commandId = commandId;
        this.orderId = orderId;
        this.newPrice = newPrice;
        this.currency = currency;
        this.occurredAt = LocalDateTime.now();
    }

    // Getters
    public String getCommandId() { return commandId; }
    public String getOrderId() { return orderId; }
    public BigDecimal getNewPrice() { return newPrice; }
    public Currency getCurrency() { return currency; }
    @Override
    public LocalDateTime getOccurredAt() { return occurredAt; }
}