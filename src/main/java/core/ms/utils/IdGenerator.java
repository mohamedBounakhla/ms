package core.ms.utils;

public class IdGenerator {
    private static final String TRANSACTION_PREFIX = "TX-";
    private static final String EVENT_PREFIX = "EVT-";
    private static final String ORDER_PREFIX = "ORDER-";
    private static final String PORTFOLIO_PREFIX = "PF-";        // New
    private static final String RESERVATION_PREFIX = "RES-";     // New
    private static final String POSITION_PREFIX = "POS-";

    /**
     * Generates unique transaction ID.
     */
    public static String generateTransactionId() {
        return TRANSACTION_PREFIX + createUniqueId();
    }

    /**
     * Generates unique event ID.
     */
    public static String generateEventId() {
        return EVENT_PREFIX + createUniqueId();
    }

    /**
     * Generates unique order ID.
     */
    public static String generateOrderId() {
        return ORDER_PREFIX + createUniqueId();
    }

    /**
     * Generates unique portfolio ID.
     */
    public static String generatePortfolioId() {
        return PORTFOLIO_PREFIX + createUniqueId();
    }

    /**
     * Generates unique reservation ID.
     */
    public static String generateReservationId() {
        return RESERVATION_PREFIX + createUniqueId();
    }

    /**
     * Generates unique position ID.
     */
    public static String generatePositionId() {
        return POSITION_PREFIX + createUniqueId();
    }

    /**
     * Creates a unique identifier.
     */
    private static String createUniqueId() {
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 10000);
        return timestamp + "-" + String.format("%04d", random);
    }
}