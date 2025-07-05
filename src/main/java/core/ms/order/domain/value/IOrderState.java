package core.ms.order.domain.value;

public interface IOrderState {
    void cancelOrder();
    void cancelPartialOrder();
    void fillPartialOrder();
    void completeOrder();
}