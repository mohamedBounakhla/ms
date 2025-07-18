package core.ms.order.infrastructure.persistence.dao;

import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.order.infrastructure.persistence.entities.SellOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface SellOrderDAO extends JpaRepository<SellOrderEntity, String> {
    List<SellOrderEntity> findBySymbolCode(String symbolCode);
    List<SellOrderEntity> findByStatus(OrderStatusEnum status);
}