package core.ms.portfolio.domain.ports.inbound;

public class OrderReservationResult {
    private final boolean success;
    private final String reservationId;
    private final String orderId;
    private final String message;
    private final ReservationType type;

    public enum ReservationType {
        CASH, ASSET
    }

    public OrderReservationResult(boolean success, String reservationId,
                                  String orderId, String message, ReservationType type) {
        this.success = success;
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.message = message;
        this.type = type;
    }

    // Static factory methods
    public static OrderReservationResult successfulCashReservation(String reservationId, String orderId) {
        return new OrderReservationResult(true, reservationId, orderId,
                "Cash reserved successfully", ReservationType.CASH);
    }

    public static OrderReservationResult successfulAssetReservation(String reservationId, String orderId) {
        return new OrderReservationResult(true, reservationId, orderId,
                "Assets reserved successfully", ReservationType.ASSET);
    }

    public static OrderReservationResult failed(String message) {
        return new OrderReservationResult(false, null, null, message, null);
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getReservationId() { return reservationId; }
    public String getOrderId() { return orderId; }
    public String getMessage() { return message; }
    public ReservationType getType() { return type; }
}