package core.ms.order.domain.events.publish;

import core.ms.shared.events.BaseEvent;

public class OrderCreationFailedEvent extends BaseEvent {
    private final String reservationId;
    private final String portfolioId;
    private final String reason;
    private final String errorDetails;

    public OrderCreationFailedEvent(String correlationId, String reservationId,
                                    String portfolioId, String reason, String errorDetails) {
        super(correlationId, "ORDER_BC");
        this.reservationId = reservationId;
        this.portfolioId = portfolioId;
        this.reason = reason;
        this.errorDetails = errorDetails;
    }

    // Getters
    public String getReservationId() { return reservationId; }
    public String getPortfolioId() { return portfolioId; }
    public String getReason() { return reason; }
    public String getErrorDetails() { return errorDetails; }
}