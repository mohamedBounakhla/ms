package core.ms.order_book.application.dto.command;

import jakarta.validation.constraints.NotBlank;

public class AddOrderToBookCommand {
    @NotBlank(message = "Order ID cannot be blank")
    private String orderId;

    public AddOrderToBookCommand() {}

    public AddOrderToBookCommand(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
}