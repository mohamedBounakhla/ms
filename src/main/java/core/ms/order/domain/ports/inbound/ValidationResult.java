package core.ms.order.domain.ports.inbound;

import core.ms.order.domain.validators.ValidationErrorMessage;
import java.util.List;

/**
 * Result type for validation operations
 */
public class ValidationResult {
    private final boolean valid;
    private final List<ValidationErrorMessage> errors;

    public ValidationResult(boolean valid, List<ValidationErrorMessage> errors) {
        this.valid = valid;
        this.errors = errors != null ? errors : List.of();
    }

    public static ValidationResult valid() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult invalid(List<ValidationErrorMessage> errors) {
        return new ValidationResult(false, errors);
    }

    // Getters
    public boolean isValid() { return valid; }
    public List<ValidationErrorMessage> getErrors() { return errors; }
}
