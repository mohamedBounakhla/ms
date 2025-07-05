package core.ms.order.domain.value;

public interface StateTransition {
    void transitionTo(IOrderState newState, OrderStatusEnum status);
}