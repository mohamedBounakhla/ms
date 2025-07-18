package core.ms.order.domain.value_objects;

public class FilledOrder extends AbstractOrderState {

    public FilledOrder(StateTransition transition) {
        super(transition);
    }

    @Override
    public void cancelOrder() {
        throw new IllegalStateException("Cannot cancel a filled order");
    }

    @Override
    public void cancelPartialOrder() {
        throw new IllegalStateException("Cannot cancel a filled order");
    }

    @Override
    public void fillPartialOrder() {
        throw new IllegalStateException("Order is already filled");
    }

    @Override
    public void completeOrder() {
        throw new IllegalStateException("Order is already filled");
    }
}
