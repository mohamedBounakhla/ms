package core.ms.order.domain.value_objects;

public class PartialOrder extends AbstractOrderState {

    public PartialOrder(StateTransition transition) {
        super(transition);
    }

    @Override
    public void cancelPartialOrder() {
        // Stay in PARTIAL state, just reduce remaining quantity
        // No state transition needed
    }
}