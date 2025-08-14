package core.ms.utils.idgenerator;


import org.springframework.stereotype.Component;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface IdGeneratorBean {
    String value() default "";
}
