package core.ms.portfolio.domain.value_object;

public final class ReservationStates {

    // Singleton instances of each state
    public static final ReservationState PENDING = new PendingState();
    public static final ReservationState CONFIRMED = new ConfirmedState();
    public static final ReservationState RELEASED = new ReleasedState();
    public static final ReservationState EXPIRED = new ExpiredState();

    private ReservationStates() {
        // Private constructor to prevent instantiation
    }

    public static ReservationState fromType(ReservationState.StateType type) {
        return switch (type) {
            case PENDING -> PENDING;
            case CONFIRMED -> CONFIRMED;
            case RELEASED -> RELEASED;
            case EXPIRED -> EXPIRED;
        };
    }
}