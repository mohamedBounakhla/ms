package core.ms.order.domain.value;

import lombok.Getter;

public class OrderStatus implements IOrderState, StateTransition {
    private IOrderState orderState;
    // ===== GETTERS =====
    @Getter
    private OrderStatusEnum status;

    public OrderStatus() {
        orderState = new PendingOrder(this);
        status = OrderStatusEnum.PENDING;
    }

    // ===== STATE TRANSITION CALLBACK =====
    @Override
    public void transitionTo(IOrderState newState, OrderStatusEnum status) {
        this.orderState = newState;
        this.status = status;
    }

    // ===== DELEGATION TO CURRENT STATE =====
    @Override
    public void cancelOrder() {
        this.orderState.cancelOrder();
    }

    @Override
    public void cancelPartialOrder() {
        this.orderState.cancelPartialOrder();
    }

    @Override
    public void fillPartialOrder() {
        this.orderState.fillPartialOrder();
    }

    @Override
    public void completeOrder() {
        this.orderState.completeOrder();
    }

    public boolean isTerminal() {
        return status == OrderStatusEnum.FILLED || status == OrderStatusEnum.CANCELLED;
    }

    @Override
    public String toString() {
        return status.name();
    }
}