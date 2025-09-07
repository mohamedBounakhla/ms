package core.ms.portfolio.infrastructure.persistence.entities;

import core.ms.shared.OrderType;
import core.ms.shared.money.Currency;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_reservations",
        indexes = {
                @Index(name = "idx_portfolio_id", columnList = "portfolio_id"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_created_at", columnList = "created_at")
        })
public class ReservationEntity {

    public enum ReservationStatus {
        PENDING, CONFIRMED, EXECUTED, RELEASED, FAILED
    }

    @Id
    @Column(name = "reservation_id", length = 50)
    private String reservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private PortfolioEntity portfolio;

    @Column(name = "order_id", length = 50)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 10)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "symbol_code", length = 20)
    private String symbolCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 10)
    private Currency currency;

    @Column(name = "amount", precision = 19, scale = 8)
    private BigDecimal amount;

    @Column(name = "quantity", precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    // Constructors
    public ReservationEntity() {}

    public ReservationEntity(String reservationId, PortfolioEntity portfolio,
                             OrderType orderType, String symbolCode,
                             Currency currency, BigDecimal amount,
                             BigDecimal quantity, String correlationId) {
        this.reservationId = reservationId;
        this.portfolio = portfolio;
        this.orderType = orderType;
        this.status = ReservationStatus.PENDING;
        this.symbolCode = symbolCode;
        this.currency = currency;
        this.amount = amount;
        this.quantity = quantity;
        this.correlationId = correlationId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Business methods
    public void confirm(String orderId) {
        this.orderId = orderId;
        this.status = ReservationStatus.CONFIRMED;
        this.updatedAt = LocalDateTime.now();
    }

    public void execute() {
        this.status = ReservationStatus.EXECUTED;
        this.executedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void release() {
        this.status = ReservationStatus.RELEASED;
        this.updatedAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = ReservationStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED;
    }

    // Getters and Setters
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }

    public PortfolioEntity getPortfolio() { return portfolio; }
    public void setPortfolio(PortfolioEntity portfolio) { this.portfolio = portfolio; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public OrderType getOrderType() { return orderType; }
    public void setOrderType(OrderType orderType) { this.orderType = orderType; }

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }

    public String getSymbolCode() { return symbolCode; }
    public void setSymbolCode(String symbolCode) { this.symbolCode = symbolCode; }

    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
}