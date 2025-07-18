package core.ms.order.domain.ports.inbound;


import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.shared.domain.Symbol;
import core.ms.shared.domain.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Primary service interface for Order domain operations.
 * Defines all use cases that the Order domain can handle.
 */
public interface OrderService {

    // ===== ORDER CREATION =====
    /**
     * Creates a new buy order
     */
    OrderOperationResult createBuyOrder(String userId, Symbol symbol, Money price, BigDecimal quantity);

    /**
     * Creates a new sell order
     */
    OrderOperationResult createSellOrder(String userId, Symbol symbol, Money price, BigDecimal quantity);

    // ===== ORDER MANAGEMENT =====
    /**
     * Cancels an existing order
     */
    OrderOperationResult cancelOrder(String orderId);

    /**
     * Updates the price of an existing order
     */
    OrderOperationResult updateOrderPrice(String orderId, Money newPrice);

    /**
     * Cancels partial quantity of an order
     */
    OrderOperationResult cancelPartialOrder(String orderId, BigDecimal quantityToCancel);

    // ===== ORDER QUERIES =====
    /**
     * Finds an order by its ID
     */
    Optional<IOrder> findOrderById(String orderId);

    /**
     * Finds all active orders for a specific symbol
     */
    List<IOrder> findActiveOrdersBySymbol(Symbol symbol);

    /**
     * Finds all buy orders for a specific symbol
     */
    List<IBuyOrder> findBuyOrdersBySymbol(Symbol symbol);

    /**
     * Finds all sell orders for a specific symbol
     */
    List<ISellOrder> findSellOrdersBySymbol(Symbol symbol);

    /**
     * Finds orders by status
     */
    List<IOrder> findOrdersByStatus(OrderStatusEnum status);

    // ===== ORDER VALIDATION =====
    /**
     * Validates if an order can be created
     */
    ValidationResult validateOrderCreation(String userId, Symbol symbol, Money price, BigDecimal quantity);

    /**
     * Validates if an order can be cancelled
     */
    ValidationResult validateOrderCancellation(String orderId);
}