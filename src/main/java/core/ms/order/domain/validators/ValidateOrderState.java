package core.ms.order.domain.validators;

import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.validators.annotation.OrderNotFinal;
import core.ms.order.domain.value_objects.OrderStatusEnum;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.lang.annotation.Annotation;

public class ValidateOrderState {
    private Map<Class<? extends Annotation>, MethodChecker> annotationMap = new HashMap<>();

    public ValidateOrderState() {
        annotationMap.put(OrderNotFinal.class, this::checkOrderNotFinal);
    }

    /**
     * Validates all annotated fields in the given object
     * @param target The object to validate
     * @return List of validation errors (empty if valid)
     */
    public List<ValidationErrorMessage> validate(Object target) {
        List<ValidationErrorMessage> errors = new ArrayList<>();

        // Get all fields from the class hierarchy
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            Field[] fields = clazz.getDeclaredFields();

            for (Field field : fields) {
                // Check each annotation on the field
                for (Annotation annotation : field.getAnnotations()) {
                    MethodChecker checker = annotationMap.get(annotation.annotationType());
                    if (checker != null) {
                        try {
                            // Make field accessible to read its value
                            field.setAccessible(true);
                            Object fieldValue = field.get(target);

                            // Find the corresponding getter method
                            Method getterMethod = findGetterMethod(clazz, field);
                            if (getterMethod != null) {
                                Optional<ValidationErrorMessage> error = checker.checkMethod(annotation, getterMethod, fieldValue);
                                error.ifPresent(errors::add);
                            } else {
                                // If no getter found, pass null method but use fieldValue as object
                                Optional<ValidationErrorMessage> error = checker.checkMethod(annotation, null, fieldValue);
                                error.ifPresent(errors::add);
                            }

                        } catch (IllegalAccessException e) {
                            errors.add(new ValidationErrorMessage("Cannot access field: " + field.getName()));
                        }
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }

        return errors;
    }

    /**
     * Validates a single object and throws exception if invalid
     * @param target The object to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateAndThrow(Object target) {
        List<ValidationErrorMessage> errors = validate(target);
        if (!errors.isEmpty()) {
            StringBuilder message = new StringBuilder("Validation failed: ");
            for (ValidationErrorMessage error : errors) {
                message.append(error.getMessage()).append("; ");
            }
            throw new IllegalArgumentException(message.toString());
        }
    }

    /**
     * Checks if an order is not in a final state
     */
    private Optional<ValidationErrorMessage> checkOrderNotFinal(Annotation annotation, Method method, Object o) {
        OrderNotFinal orderNotFinal = (OrderNotFinal) annotation;

        // The object 'o' is the field value (IOrder instance)
        if (o instanceof IOrder) {
            IOrder order = (IOrder) o;
            OrderStatusEnum status = order.getStatus().getStatus();

            // Check if order is in a final state
            if (status == OrderStatusEnum.FILLED || status == OrderStatusEnum.CANCELLED) {
                return Optional.of(new ValidationErrorMessage(orderNotFinal.message()));
            }
        }

        return Optional.empty();
    }

    /**
     * Finds the getter method for a given field
     */
    private Method findGetterMethod(Class<?> clazz, Field field) {
        String fieldName = field.getName();
        String getterName = "get" + capitalize(fieldName);

        try {
            return clazz.getMethod(getterName);
        } catch (NoSuchMethodException e) {
            // Try boolean getter pattern
            if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                String booleanGetterName = "is" + capitalize(fieldName);
                try {
                    return clazz.getMethod(booleanGetterName);
                } catch (NoSuchMethodException ex) {
                    // No getter found
                    return null;
                }
            }
            return null;
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}