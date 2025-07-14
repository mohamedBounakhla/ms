package core.ms.utils;

public class IdGenerator {
    private static final String TRANSACTION_PREFIX = "TX-";
    private static final String EVENT_PREFIX = "EVT-";

    /**
     * Generates unique transaction ID.
     */
    public String generateTransactionId() {
        return TRANSACTION_PREFIX + createUniqueId();
    }

    /**
     * Generates unique event ID.
     */
    public String generateEventId() {
        return EVENT_PREFIX + createUniqueId();
    }

    /**
     * Creates a unique identifier.
     */
    private String createUniqueId() {
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 10000);
        return timestamp + "-" + String.format("%04d", random);
    }
}