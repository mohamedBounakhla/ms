package core.ms.order.domain.value_objects;

public interface IOrderState {
    void cancelOrder();
    void cancelPartialOrder();
    void fillPartialOrder();
    void completeOrder();
}