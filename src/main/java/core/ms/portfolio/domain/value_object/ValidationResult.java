package core.ms.portfolio.domain.value_object;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {

    private final boolean valid;
    private final List<ValidationError> errors;

    private ValidationResult(boolean valid, List<ValidationError> errors) {
        this.valid = valid;
        this.errors = new ArrayList<>(errors);
    }

    public static ValidationResult valid() {
        return new ValidationResult(true, new ArrayList<>());
    }

    public static ValidationResult invalid(String reason) {
        List<ValidationError> errors = new ArrayList<>();
        errors.add(new ValidationError(reason));
        return new ValidationResult(false, errors);
    }

    public static ValidationResult invalid(List<ValidationError> errors) {
        return new ValidationResult(false, errors);
    }

    public boolean isValid() {
        return valid;
    }

    public List<ValidationError> getErrors() {
        return new ArrayList<>(errors);
    }

    public String getErrorMessage() {
        return errors.stream()
                .map(ValidationError::getMessage)
                .reduce((a, b) -> a + "; " + b)
                .orElse("");
    }

    public static class ValidationError {
        private final String message;
        private final String field;

        public ValidationError(String message) {
            this(message, null);
        }

        public ValidationError(String message, String field) {
            this.message = message;
            this.field = field;
        }

        public String getMessage() {
            return field != null ? field + ": " + message : message;
        }
    }
}