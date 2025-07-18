package core.ms.order.infrastructure.persistence.dao;

import core.ms.order.infrastructure.persistence.entities.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionDAO extends JpaRepository<TransactionEntity, String> {
    List<TransactionEntity> findByBuyOrderId(String buyOrderId);
    List<TransactionEntity> findBySellOrderId(String sellOrderId);
    List<TransactionEntity> findBySymbolCode(String symbolCode);
    List<TransactionEntity> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}