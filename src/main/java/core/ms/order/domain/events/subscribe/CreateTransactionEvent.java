package core.ms.order.domain.events.subscribe;

import core.ms.shared.events.DomainEvent;
import core.ms.shared.money.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateTransactionEvent implements DomainEvent {
    private final String commandId;
    private final String buyOrderId;
    private final String sellOrderId;
    private final BigDecimal executionPrice;
    private final Currency currency;
    private final BigDecimal quantity;
    private final LocalDateTime occurredAt;

    public CreateTransactionEvent(String commandId, String buyOrderId, String sellOrderId,
                                  BigDecimal executionPrice, Currency currency, BigDecimal quantity) {
        this.commandId = commandId;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.executionPrice = executionPrice;
        this.currency = currency;
        this.quantity = quantity;
        this.occurredAt = LocalDateTime.now();
    }

    // Getters
    public String getCommandId() { return commandId; }
    public String getBuyOrderId() { return buyOrderId; }
    public String getSellOrderId() { return sellOrderId; }
    public BigDecimal getExecutionPrice() { return executionPrice; }
    public Currency getCurrency() { return currency; }
    public BigDecimal getQuantity() { return quantity; }
    @Override
    public LocalDateTime getOccurredAt() { return occurredAt; }
}