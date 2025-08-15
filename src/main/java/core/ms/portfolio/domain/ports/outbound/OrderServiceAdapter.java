package core.ms.portfolio.domain.ports.outbound;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;

import java.util.Optional;

public interface OrderServiceAdapter {
    Optional<IBuyOrder> findBuyOrderById(String orderId);
    Optional<ISellOrder> findSellOrderById(String orderId);
    boolean orderExists(String orderId);
    boolean isOrderActive(String orderId);
}