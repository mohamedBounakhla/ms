package core.ms.order_book.infrastructure.persistence.DAO;

import core.ms.order_book.infrastructure.persistence.entities.OrderBookEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderBookDAO extends JpaRepository<OrderBookEntity, String> {

    Optional<OrderBookEntity> findBySymbolCode(String symbolCode);

    List<OrderBookEntity> findByActiveTrue();

    @Query("SELECT o FROM OrderBookEntity o WHERE o.active = true ORDER BY o.totalVolume DESC")
    List<OrderBookEntity> findActiveOrderBooksByVolume();

    boolean existsBySymbolCode(String symbolCode);

    void deleteBySymbolCode(String symbolCode);

    @Query("SELECT COUNT(o) FROM OrderBookEntity o WHERE o.active = true")
    long countActive();
}