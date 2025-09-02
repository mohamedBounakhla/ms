package core.ms.order.application.services;

import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.ports.inbound.OrderService;
import core.ms.order.domain.ports.outbound.OrderRepository;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.shared.money.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OrderQueryService implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderQueryService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public Optional<IOrder> findOrderById(String orderId) {
        logger.debug("Finding order by ID: {}", orderId);
        return orderRepository.findById(orderId);
    }

    @Override
    public List<IOrder> findOrdersByPortfolioId(String portfolioId) {
        logger.debug("Finding orders for portfolio: {}", portfolioId);
        return orderRepository.findByPortfolioId(portfolioId);
    }

    @Override
    public Optional<IOrder> findOrderByReservationId(String reservationId) {
        logger.debug("Finding order by reservation ID: {}", reservationId);
        List<IOrder> allOrders = orderRepository.findAll();
        return allOrders.stream()
                .filter(order -> reservationId.equals(order.getReservationId()))
                .findFirst();
    }


    @Override
    public List<IOrder> findActiveOrdersBySymbol(Symbol symbol) {
        logger.debug("Finding active orders for symbol: {}", symbol.getCode());
        List<IOrder> allOrders = orderRepository.findBySymbol(symbol);
        return allOrders.stream()
                .filter(IOrder::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public List<IOrder> findOrdersByStatus(OrderStatusEnum status) {
        logger.debug("Finding orders by status: {}", status);
        return orderRepository.findByStatus(status);
    }

    @Override
    public long getTotalOrderCount() {
        logger.debug("Getting total order count");
        return orderRepository.count();
    }

    @Override
    public long getActiveOrderCount() {
        logger.debug("Getting active order count");
        List<IOrder> allOrders = orderRepository.findAll();
        return allOrders.stream()
                .filter(IOrder::isActive)
                .count();
    }
}