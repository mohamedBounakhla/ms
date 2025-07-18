package core.ms.order.domain.value_objects;

public interface StateTransition {
    void transitionTo(IOrderState newState, OrderStatusEnum status);
}