package core.ms.portfolio.domain.reservations;

import core.ms.order.domain.entities.IOrder;



import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import java.util.Objects;

public abstract class Reservation<T extends IOrder> {
    protected final String reservationId;
    protected final T order;
    protected final Instant createdAt;
    protected final Instant expirationTime;

    private static final Duration DEFAULT_EXPIRATION = Duration.ofMinutes(5);



    protected Reservation(String reservationId, T order) {
        this.reservationId = Objects.requireNonNull(reservationId);
        this.order = Objects.requireNonNull(order);
        this.createdAt = Instant.now();
        this.expirationTime = createdAt.plus(DEFAULT_EXPIRATION);
    }

    public abstract BigDecimal getReservedAmount();

    public boolean isExpired() {
        return Instant.now().isAfter(expirationTime);
    }

    // Simple getters
    public String getReservationId() { return reservationId; }
    public T getOrder() { return order; }
    public Instant getExpirationTime() { return expirationTime; }
    public Instant getCreatedAt() {
        return createdAt;
    }
}