package core.ms.shared.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class EventDebugListener {
    private static final Logger logger = LoggerFactory.getLogger(EventDebugListener.class);

    @EventListener
    public void handleAnyEvent(DomainEvent event) {
        logger.debug("🔍 EVENT: {} from {} with correlation ID: {}",
                event.getClass().getSimpleName(),
                event.getSourceBC(),
                event.getCorrelationId()
        );
    }
}