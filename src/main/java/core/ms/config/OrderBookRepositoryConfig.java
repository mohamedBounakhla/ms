package core.ms.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderBookRepositoryConfig {

    // Use in-memory implementation by default
    @ConditionalOnProperty(
            name = "orderbook.persistence.type",
            havingValue = "in-memory",
            matchIfMissing = true
    )
    public static class InMemoryConfig {
        // OrderBookRepositoryService will be used
    }

    // Use JPA implementation when configured
    @ConditionalOnProperty(
            name = "orderbook.persistence.type",
            havingValue = "jpa"
    )
    public static class JpaConfig {
        // OrderBookRepositoryJpaImpl will be used
    }
}