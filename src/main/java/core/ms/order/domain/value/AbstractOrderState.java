package core.ms.order.domain.value;

public abstract class AbstractOrderState implements IOrderState {
    protected StateTransition transition;

    public AbstractOrderState(StateTransition transition) {
        this.transition = transition;
    }

    @Override
    public void cancelOrder() {
        transition.transitionTo(new CancelledOrder(transition), OrderStatusEnum.CANCELLED);
    }

    @Override
    public void fillPartialOrder() {
        transition.transitionTo(new PartialOrder(transition), OrderStatusEnum.PARTIAL);
    }

    @Override
    public void completeOrder() {
        transition.transitionTo(new FilledOrder(transition), OrderStatusEnum.FILLED);
    }

    // Default implementation for cancelPartialOrder - states override if needed
    @Override
    public void cancelPartialOrder() {
        // Default: stay in same state (no transition)
        // Override in specific states where it makes sense
    }
}