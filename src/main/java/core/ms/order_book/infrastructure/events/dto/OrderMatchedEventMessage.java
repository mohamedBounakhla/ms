package core.ms.order_book.infrastructure.events.dto;

import core.ms.shared.events.DomainEvent;
import core.ms.shared.money.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderMatchedEventMessage implements DomainEvent {
    private final String buyOrderId;
    private final String sellOrderId;
    private final String symbolCode;
    private final BigDecimal quantity;
    private final BigDecimal executionPrice;
    private final Currency currency;
    private final LocalDateTime occurredAt;

    public OrderMatchedEventMessage(String buyOrderId, String sellOrderId, String symbolCode,
                                    BigDecimal quantity, BigDecimal executionPrice,
                                    Currency currency, LocalDateTime occurredAt) {
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.symbolCode = symbolCode;
        this.quantity = quantity;
        this.executionPrice = executionPrice;
        this.currency = currency;
        this.occurredAt = occurredAt;
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    // Getters
    public String getBuyOrderId() { return buyOrderId; }
    public String getSellOrderId() { return sellOrderId; }
    public String getSymbolCode() { return symbolCode; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getExecutionPrice() { return executionPrice; }
    public Currency getCurrency() { return currency; }
}