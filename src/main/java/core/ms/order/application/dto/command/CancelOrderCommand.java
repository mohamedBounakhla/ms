package core.ms.order.application.dto.command;

public class CancelOrderCommand {
    private String orderId;

    public CancelOrderCommand() {}

    public CancelOrderCommand(String orderId) {
        this.orderId = orderId;
    }

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
}