package core.ms.portfolio.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "asset_reservations")
public class AssetReservationEntity {

    @Id
    @Column(name = "reservation_id", length = 50)
    private String reservationId;

    @Column(name = "portfolio_id", nullable = false, length = 50)
    private String portfolioId;

    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;

    @Column(name = "symbol_code", nullable = false, length = 20)
    private String symbolCode;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expiration_time", nullable = false)
    private Instant expirationTime;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // ACTIVE, EXECUTED, RELEASED, EXPIRED

    // Constructors
    public AssetReservationEntity() {}

    // Getters and Setters
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getSymbolCode() { return symbolCode; }
    public void setSymbolCode(String symbolCode) { this.symbolCode = symbolCode; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpirationTime() { return expirationTime; }
    public void setExpirationTime(Instant expirationTime) { this.expirationTime = expirationTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}