package core.ms.portfolio.infrastructure.persistence.entities;

import core.ms.shared.money.Currency;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cash_reservations")
public class CashReservationEntity {

    @Id
    @Column(name = "reservation_id", length = 50)
    private String reservationId;

    @Column(name = "portfolio_id", nullable = false, length = 50)
    private String portfolioId;

    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private Currency currency;

    @Column(name = "amount", nullable = false, precision = 19, scale = 8)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expiration_time", nullable = false)
    private Instant expirationTime;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // ACTIVE, EXECUTED, RELEASED, EXPIRED

    // Constructors
    public CashReservationEntity() {}

    // Getters and Setters
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpirationTime() { return expirationTime; }
    public void setExpirationTime(Instant expirationTime) { this.expirationTime = expirationTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}