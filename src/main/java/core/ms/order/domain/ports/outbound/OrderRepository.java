package core.ms.order.domain.ports.outbound;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.shared.money.Symbol;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    IOrder save(IOrder order);
    void flush();
    Optional<IOrder> findById(String orderId);
    Optional<IOrder> findByIdWithLock(String orderId, LockModeType lockMode);
    void deleteById(String orderId);
    boolean existsById(String orderId);

    List<IOrder> findBySymbol(Symbol symbol);
    List<IOrder> findByStatus(OrderStatusEnum status);
    List<IOrder> findByPortfolioId(String portfolioId);
    List<IBuyOrder> findBuyOrdersBySymbol(Symbol symbol);
    List<ISellOrder> findSellOrdersBySymbol(Symbol symbol);

    List<IOrder> findAll();
    long count();
}