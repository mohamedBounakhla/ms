package core.ms.portfolio.infrastructure.persistence.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "portfolios")
public class PortfolioEntity {

    @Id
    @Column(name = "portfolio_id", length = 50)
    private String portfolioId;

    @Column(name = "owner_id", nullable = false, unique = true, length = 50)
    private String ownerId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<CashBalanceEntity> cashBalances = new HashSet<>();

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<PositionEntity> positions = new HashSet<>();

    // Constructors
    public PortfolioEntity() {}

    public PortfolioEntity(String portfolioId, String ownerId) {
        this.portfolioId = portfolioId;
        this.ownerId = ownerId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public void addCashBalance(CashBalanceEntity cashBalance) {
        cashBalances.add(cashBalance);
        cashBalance.setPortfolio(this);
    }

    public void addPosition(PositionEntity position) {
        positions.add(position);
        position.setPortfolio(this);
    }

    // Getters and Setters
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Set<CashBalanceEntity> getCashBalances() { return cashBalances; }
    public void setCashBalances(Set<CashBalanceEntity> cashBalances) { this.cashBalances = cashBalances; }
    public Set<PositionEntity> getPositions() { return positions; }
    public void setPositions(Set<PositionEntity> positions) { this.positions = positions; }
}
