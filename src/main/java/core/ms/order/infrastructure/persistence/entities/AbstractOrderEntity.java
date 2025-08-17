package core.ms.order.infrastructure.persistence.entities;

import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.shared.money.Currency;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@MappedSuperclass
public abstract class AbstractOrderEntity {

    @Id
    @Column(name = "id", length = 50)
    protected String id;

    @Column(name = "portfolio_id", length = 50)
    protected String portfolioId;

    @Column(name = "reservation_id", length = 50)
    protected String reservationId;

    @Column(name = "symbol_code", nullable = false, length = 20)
    protected String symbolCode;

    @Column(name = "symbol_name", nullable = false, length = 100)
    protected String symbolName;

    @Column(name = "price", nullable = false, precision = 19, scale = 8)
    protected BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    protected Currency currency;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 8)
    protected BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    protected OrderStatusEnum status;

    @Column(name = "executed_quantity", nullable = false, precision = 19, scale = 8)
    protected BigDecimal executedQuantity;

    @Column(name = "created_at", nullable = false)
    protected LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    protected LocalDateTime updatedAt;

    // Constructors
    protected AbstractOrderEntity() {}

    protected AbstractOrderEntity(String id, String portfolioId, String reservationId,
                                  String symbolCode, String symbolName,
                                  BigDecimal price, Currency currency, BigDecimal quantity,
                                  OrderStatusEnum status, BigDecimal executedQuantity,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.portfolioId = portfolioId;
        this.reservationId = reservationId;
        this.symbolCode = symbolCode;
        this.symbolName = symbolName;
        this.price = price;
        this.currency = currency;
        this.quantity = quantity;
        this.status = status;
        this.executedQuantity = executedQuantity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public String getSymbolCode() { return symbolCode; }
    public void setSymbolCode(String symbolCode) { this.symbolCode = symbolCode; }
    public String getSymbolName() { return symbolName; }
    public void setSymbolName(String symbolName) { this.symbolName = symbolName; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public OrderStatusEnum getStatus() { return status; }
    public void setStatus(OrderStatusEnum status) { this.status = status; }
    public BigDecimal getExecutedQuantity() { return executedQuantity; }
    public void setExecutedQuantity(BigDecimal executedQuantity) { this.executedQuantity = executedQuantity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AbstractOrderEntity that = (AbstractOrderEntity) obj;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}