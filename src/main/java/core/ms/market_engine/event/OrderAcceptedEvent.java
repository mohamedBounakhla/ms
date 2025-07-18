package core.ms.market_engine.event;

import core.ms.order.domain.entities.IOrder;

import java.util.Objects;

public class OrderAcceptedEvent extends DomainEvent {
    private final IOrder order;

    public OrderAcceptedEvent(IOrder order, String engineId) {
        super(engineId);
        this.order = Objects.requireNonNull(order, "Order cannot be null");
    }

    public IOrder getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return "OrderAcceptedEvent{" +
                "orderId='" + order.getId() + '\'' +
                ", symbol=" + order.getSymbol().getCode() +
                ", eventId='" + eventId + '\'' +
                '}';
    }
}
