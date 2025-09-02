package core.ms.shared.events;

public abstract class CorrelationAwareEventListener {

    protected void handleEvent(DomainEvent event, Runnable handler) {
        // Propagate correlation ID to current thread
        EventContext.setCorrelationId(event.getCorrelationId());
        try {
            handler.run();
        } finally {
            EventContext.clear();
        }
    }
}