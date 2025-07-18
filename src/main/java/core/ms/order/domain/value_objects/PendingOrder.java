package core.ms.order.domain.value_objects;

public class PendingOrder extends AbstractOrderState {

    public PendingOrder(StateTransition transition) {
        super(transition);
    }

    @Override
    public void cancelPartialOrder() {
        // Stay in PENDING state, just reduce quantity
        // No state transition needed
    }
}
