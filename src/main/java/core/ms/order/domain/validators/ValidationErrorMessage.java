package core.ms.order.domain.validators;

public class ValidationErrorMessage {
    private String message;

    public ValidationErrorMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}