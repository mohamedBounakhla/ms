package core.ms.order_book.infrastructure.persistence.entities;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;

@Entity
@Table(name = "order_book_snapshots",
        indexes = {
                @Index(name = "idx_symbol_timestamp", columnList = "symbol_code,snapshot_time")
        })
public class OrderBookSnapshotEntity {
    @Id
    @Column(name = "id", length = 100)
    private String id;

    @Column(name = "symbol_code", nullable = false, length = 20)
    private String symbolCode;

    @Column(name = "snapshot_time", nullable = false)
    private Instant snapshotTime;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    private List<OrderSnapshotEntity> orders = new ArrayList<>();

    @Embedded
    private OrderBookStatisticsEntity statistics;

    // Constructors
    public OrderBookSnapshotEntity() {}

    public OrderBookSnapshotEntity(String id, String symbolCode, Instant snapshotTime) {
        this.id = id;
        this.symbolCode = symbolCode;
        this.snapshotTime = snapshotTime;
    }

    // Helper methods
    public void addOrder(OrderSnapshotEntity order) {
        orders.add(order);
        order.setSnapshot(this);
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSymbolCode() { return symbolCode; }
    public void setSymbolCode(String symbolCode) { this.symbolCode = symbolCode; }
    public Instant getSnapshotTime() { return snapshotTime; }
    public void setSnapshotTime(Instant snapshotTime) { this.snapshotTime = snapshotTime; }
    public List<OrderSnapshotEntity> getOrders() { return orders; }
    public void setOrders(List<OrderSnapshotEntity> orders) { this.orders = orders; }
    public OrderBookStatisticsEntity getStatistics() { return statistics; }
    public void setStatistics(OrderBookStatisticsEntity statistics) { this.statistics = statistics; }
}