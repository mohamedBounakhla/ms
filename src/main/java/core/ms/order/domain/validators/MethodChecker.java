package core.ms.order.domain.validators;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

public interface MethodChecker {
    Optional<ValidationErrorMessage> checkMethod(Annotation a, Method m, Object o);

}
