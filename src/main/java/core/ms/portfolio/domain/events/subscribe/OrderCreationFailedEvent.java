package core.ms.portfolio.domain.events.subscribe;

import core.ms.shared.OrderType;
import core.ms.shared.events.BaseEvent;

public class OrderCreationFailedEvent extends BaseEvent {
    private final String reservationId;
    private final String portfolioId;
    private final OrderType orderType;
    private final String reason;

    public OrderCreationFailedEvent(String correlationId, String sourceBC,
                                    String reservationId, String portfolioId,
                                    OrderType orderType, String reason) {
        super(correlationId, sourceBC);
        this.reservationId = reservationId;
        this.portfolioId = portfolioId;
        this.orderType = orderType;
        this.reason = reason;
    }

    // Getters
    public String getReservationId() { return reservationId; }
    public String getPortfolioId() { return portfolioId; }
    public OrderType getOrderType() { return orderType; }
    public String getReason() { return reason; }
}