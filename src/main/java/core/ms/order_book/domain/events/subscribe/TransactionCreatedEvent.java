package core.ms.order_book.domain.events.subscribe;

import core.ms.shared.events.BaseEvent;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;

public class TransactionCreatedEvent extends BaseEvent {
    private final String transactionId;
    private final String buyOrderId;
    private final String sellOrderId;
    private final Symbol symbol;
    private final BigDecimal executedQuantity;
    private final Money executionPrice;
    private final BigDecimal buyOrderRemainingQuantity;
    private final BigDecimal sellOrderRemainingQuantity;

    public TransactionCreatedEvent(String correlationId, String transactionId,
                                   String buyOrderId, String sellOrderId, Symbol symbol,
                                   BigDecimal executedQuantity, Money executionPrice,
                                   BigDecimal buyOrderRemainingQuantity,
                                   BigDecimal sellOrderRemainingQuantity) {
        super(correlationId, "ORDER_BC");
        this.transactionId = transactionId;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.symbol = symbol;
        this.executedQuantity = executedQuantity;
        this.executionPrice = executionPrice;
        this.buyOrderRemainingQuantity = buyOrderRemainingQuantity;
        this.sellOrderRemainingQuantity = sellOrderRemainingQuantity;
    }

    // Getters
    public String getTransactionId() { return transactionId; }
    public String getBuyOrderId() { return buyOrderId; }
    public String getSellOrderId() { return sellOrderId; }
    public Symbol getSymbol() { return symbol; }
    public BigDecimal getExecutedQuantity() { return executedQuantity; }
    public Money getExecutionPrice() { return executionPrice; }
    public BigDecimal getBuyOrderRemainingQuantity() { return buyOrderRemainingQuantity; }
    public BigDecimal getSellOrderRemainingQuantity() { return sellOrderRemainingQuantity; }

    public boolean isBuyOrderFullyExecuted() {
        return buyOrderRemainingQuantity.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isSellOrderFullyExecuted() {
        return sellOrderRemainingQuantity.compareTo(BigDecimal.ZERO) == 0;
    }
}