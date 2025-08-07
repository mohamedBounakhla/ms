package core.ms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class ConfigurationVerifier implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationVerifier.class);

    private final Environment env;
    private final ApplicationContext context;

    @Value("${orderbook.persistence.type:NOT_FOUND}")
    private String orderBookPersistenceType;

    @Value("${spring.datasource.url:NOT_FOUND}")
    private String datasourceUrl;

    @Value("${spring.application.name:NOT_FOUND}")
    private String applicationName;

    public ConfigurationVerifier(Environment env, ApplicationContext context) {
        this.env = env;
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("=== Configuration Verification Starting ===");

        // 1. Check which profiles are active
        String[] activeProfiles = env.getActiveProfiles();
        log.info("Active Profiles: {}",
                activeProfiles.length > 0 ? Arrays.toString(activeProfiles) : "default");

        // 2. Verify key configurations are loaded
        log.info("Application Name: {}", applicationName);
        log.info("Datasource URL: {}", datasourceUrl);
        log.info("OrderBook Persistence Type: {}", orderBookPersistenceType);

        // 3. Check if configurations are loaded
        verifyConfiguration("jwt.secret", true);  // Don't log sensitive data
        verifyConfiguration("spring.flyway.enabled", false);
        verifyConfiguration("orderbook.snapshot.enabled", false);

        // 4. List all @Configuration beans
        String[] configBeans = context.getBeanNamesForAnnotation(
                org.springframework.context.annotation.Configuration.class);
        log.info("Loaded @Configuration classes: {}", Arrays.toString(configBeans));

        // 5. Check if OrderBookRepositoryConfig is loaded
        boolean orderBookConfigLoaded = Arrays.stream(configBeans)
                .anyMatch(bean -> bean.toLowerCase().contains("orderbook"));
        log.info("OrderBook Configuration loaded: {}", orderBookConfigLoaded);

        log.info("=== Configuration Verification Complete ===");
    }

    private void verifyConfiguration(String key, boolean sensitive) {
        String value = env.getProperty(key);
        if (value != null) {
            if (sensitive) {
                log.info("{}: [PRESENT - NOT DISPLAYED]", key);
            } else {
                log.info("{}: {}", key, value);
            }
        } else {
            log.warn("{}: NOT FOUND", key);
        }
    }
}