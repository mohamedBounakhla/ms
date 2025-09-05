package core.ms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = {
        "core.ms.order.infrastructure.persistence.dao",
        "core.ms.order_book.infrastructure.persistence.DAO",
        "core.ms.portfolio.infrastructure.persistence.dao",
        "core.ms.symbol.dao",
        "core.ms.security.dao"
})
@EnableTransactionManagement
public class JpaConfig {
    // JPA configuration is handled by Spring Boot auto-configuration
    // This class provides explicit control over JPA settings if needed
}