package core.ms.utils.idgenerator;

import jakarta.annotation.PostConstruct;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IdGen {
    private static final Map<String, BaseIdGenerator> generators = new ConcurrentHashMap<>();
    private static ApplicationContext applicationContext;

    public IdGen(ApplicationContext context) {
        applicationContext = context;
    }

    @PostConstruct
    public void init() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(IdGeneratorBean.class);

        for (Object bean : beans.values()) {
            if (bean instanceof BaseIdGenerator) {
                BaseIdGenerator generator = (BaseIdGenerator) bean;
                IdGeneratorBean annotation = bean.getClass().getAnnotation(IdGeneratorBean.class);

                String name = annotation.value().isEmpty()
                        ? bean.getClass().getSimpleName().replace("IdGenerator", "").toLowerCase()
                        : annotation.value().toLowerCase();

                generators.put(name, generator);
            }
        }
    }

    /**
     * Generate ID by type name
     */
    public static String generate(String type) {
        BaseIdGenerator generator = generators.get(type.toLowerCase());
        if (generator == null) {
            throw new IllegalArgumentException("No ID generator found for type: " + type);
        }
        return generator.generate();
    }
}