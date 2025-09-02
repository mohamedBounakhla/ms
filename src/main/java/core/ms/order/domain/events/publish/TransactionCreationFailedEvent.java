package core.ms.order.domain.events.publish;

import core.ms.shared.events.BaseEvent;

public class TransactionCreationFailedEvent extends BaseEvent {
    private final String buyOrderId;
    private final String sellOrderId;
    private final String reason;
    private final String errorDetails;

    public TransactionCreationFailedEvent(String correlationId, String buyOrderId,
                                          String sellOrderId, String reason, String errorDetails) {
        super(correlationId, "ORDER_BC");
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.reason = reason;
        this.errorDetails = errorDetails;
    }

    // Getters
    public String getBuyOrderId() { return buyOrderId; }
    public String getSellOrderId() { return sellOrderId; }
    public String getReason() { return reason; }
    public String getErrorDetails() { return errorDetails; }
}