package core.ms.order.domain.ports.inbound;

import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.shared.money.Symbol;

import java.util.List;
import java.util.Optional;

/**
 * Primary service interface for Order domain operations.
 * Defines all use cases that the Order domain can handle.
 */
public interface OrderService {

    // ===== QUERY OPERATIONS (For Internal Use/Monitoring) =====

    /**
     * Finds an order by its ID
     */
    Optional<IOrder> findOrderById(String orderId);

    /**
     * Finds all orders for a portfolio
     */
    List<IOrder> findOrdersByPortfolioId(String portfolioId);

    /**
     * Finds all orders by reservation ID
     */
    Optional<IOrder> findOrderByReservationId(String reservationId);

    /**
     * Finds all active orders for a specific symbol
     */
    List<IOrder> findActiveOrdersBySymbol(Symbol symbol);

    /**
     * Finds orders by status
     */
    List<IOrder> findOrdersByStatus(OrderStatusEnum status);

    /**
     * Get total count of orders
     */
    long getTotalOrderCount();

    /**
     * Get count of active orders
     */
    long getActiveOrderCount();
}