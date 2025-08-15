package core.ms.market_engine.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class MarketEngineMonitor {
    private static final Logger log = LoggerFactory.getLogger(MarketEngineMonitor.class);

    private final AtomicLong eventsProcessed = new AtomicLong(0);
    private final AtomicLong errors = new AtomicLong(0);

    @Scheduled(fixedDelay = 60000) // Every minute
    public void logMetrics() {
        log.info("Market Engine Metrics - Events: {}, Errors: {}",
                eventsProcessed.get(), errors.get());
    }

    public void recordEventProcessed() {
        eventsProcessed.incrementAndGet();
    }

    public void recordError() {
        errors.incrementAndGet();
    }

    public long getEventsProcessed() {
        return eventsProcessed.get();
    }

    public long getErrors() {
        return errors.get();
    }
}