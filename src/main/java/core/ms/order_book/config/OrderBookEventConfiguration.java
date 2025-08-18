package core.ms.order_book.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableAsync
@EnableScheduling
public class OrderBookEventConfiguration {
    // Enables async event processing and scheduled tasks
}