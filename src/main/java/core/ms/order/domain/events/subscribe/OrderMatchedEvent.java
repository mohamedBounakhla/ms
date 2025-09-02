package core.ms.order.domain.events.subscribe;

import core.ms.shared.events.BaseEvent;
import core.ms.shared.money.Currency;

import java.math.BigDecimal;

public class OrderMatchedEvent extends BaseEvent {
    private final String buyOrderId;
    private final String sellOrderId;
    private final BigDecimal matchedQuantity;
    private final BigDecimal executionPrice;
    private final Currency currency;

    public OrderMatchedEvent(String correlationId, String sourceBC,
                             String buyOrderId, String sellOrderId,
                             BigDecimal matchedQuantity, BigDecimal executionPrice,
                             Currency currency) {
        super(correlationId, sourceBC);
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.matchedQuantity = matchedQuantity;
        this.executionPrice = executionPrice;
        this.currency = currency;
    }

    // Getters
    public String getBuyOrderId() { return buyOrderId; }
    public String getSellOrderId() { return sellOrderId; }
    public BigDecimal getMatchedQuantity() { return matchedQuantity; }
    public BigDecimal getExecutionPrice() { return executionPrice; }
    public Currency getCurrency() { return currency; }
}