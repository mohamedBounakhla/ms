package core.ms.order.infrastructure.persistence.dao;

import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.order.infrastructure.persistence.entities.BuyOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuyOrderDAO extends JpaRepository<BuyOrderEntity, String> {
    List<BuyOrderEntity> findBySymbolCode(String symbolCode);
    List<BuyOrderEntity> findByStatus(OrderStatusEnum status);
}
