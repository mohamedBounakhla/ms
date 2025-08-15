package core.ms.portfolio.infrastructure.adapters;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order.domain.ports.outbound.OrderRepository;
import core.ms.portfolio.domain.ports.outbound.OrderServiceAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OrderServiceAdapterImpl implements OrderServiceAdapter {

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public Optional<IBuyOrder> findBuyOrderById(String orderId) {
        Optional<IOrder> order = orderRepository.findById(orderId);
        if (order.isPresent() && order.get() instanceof IBuyOrder) {
            return Optional.of((IBuyOrder) order.get());
        }
        return Optional.empty();
    }

    @Override
    public Optional<ISellOrder> findSellOrderById(String orderId) {
        Optional<IOrder> order = orderRepository.findById(orderId);
        if (order.isPresent() && order.get() instanceof ISellOrder) {
            return Optional.of((ISellOrder) order.get());
        }
        return Optional.empty();
    }

    @Override
    public boolean orderExists(String orderId) {
        return orderRepository.existsById(orderId);
    }

    @Override
    public boolean isOrderActive(String orderId) {
        Optional<IOrder> order = orderRepository.findById(orderId);
        return order.map(IOrder::isActive).orElse(false);
    }
}