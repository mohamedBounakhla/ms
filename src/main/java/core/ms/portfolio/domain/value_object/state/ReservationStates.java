package core.ms.portfolio.domain.value_object.state;

public final class ReservationStates {

    // Singleton instances with clear names
    public static final ReservationState PENDING = new PendingState();
    public static final ReservationState EXECUTED = new ExecutedState();
    public static final ReservationState CANCELLED = new CancelledState();
    public static final ReservationState EXPIRED = new ExpiredState();

    private ReservationStates() {
        // Private constructor to prevent instantiation
    }

    public static ReservationState fromType(ReservationState.StateType type) {
        return switch (type) {
            case PENDING -> PENDING;
            case EXECUTED -> EXECUTED;
            case CANCELLED -> CANCELLED;
            case EXPIRED -> EXPIRED;
        };
    }
}