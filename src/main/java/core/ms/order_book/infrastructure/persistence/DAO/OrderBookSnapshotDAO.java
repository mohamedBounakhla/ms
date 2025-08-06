package core.ms.order_book.infrastructure.persistence.DAO;

import core.ms.order_book.infrastructure.persistence.entities.OrderBookSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderBookSnapshotDAO extends JpaRepository<OrderBookSnapshotEntity, String> {

    @Query("SELECT s FROM OrderBookSnapshotEntity s WHERE s.symbolCode = :symbol " +
            "ORDER BY s.snapshotTime DESC LIMIT 1")
    Optional<OrderBookSnapshotEntity> findLatestBySymbol(@Param("symbol") String symbol);

    List<OrderBookSnapshotEntity> findBySymbolCodeAndSnapshotTimeBetween(
            String symbolCode, Instant start, Instant end);

    void deleteBySnapshotTimeBefore(Instant cutoff);

    long countBySymbolCode(String symbolCode);

    @Query("SELECT s FROM OrderBookSnapshotEntity s " +
            "LEFT JOIN FETCH s.orders " +
            "WHERE s.id = :id")
    Optional<OrderBookSnapshotEntity> findByIdWithOrders(@Param("id") String id);
}