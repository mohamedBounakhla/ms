package core.ms.order.domain.value_objects;

public class CancelledOrder extends AbstractOrderState {

    public CancelledOrder(StateTransition transition) {
        super(transition);
    }

    @Override
    public void cancelOrder() {
        throw new IllegalStateException("Order is already cancelled");
    }

    @Override
    public void cancelPartialOrder() {
        throw new IllegalStateException("Order is already cancelled");
    }

    @Override
    public void fillPartialOrder() {
        throw new IllegalStateException("Cannot fill a cancelled order");
    }

    @Override
    public void completeOrder() {
        throw new IllegalStateException("Cannot complete a cancelled order");
    }
}