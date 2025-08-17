package core.ms.order.domain.ports.inbound;

import core.ms.order.application.dto.query.OrderOperationResultDTO;
import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.shared.money.Symbol;
import core.ms.shared.money.Money;
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
    OrderOperationResultDTO createBuyOrder(String portfolioId, String reservationId,
                                           Symbol symbol, Money price, BigDecimal quantity);

    /**
     * Creates a new sell order
     */
    OrderOperationResultDTO createSellOrder(String portfolioId, String reservationId,
                                            Symbol symbol, Money price, BigDecimal quantity);

    // ===== ORDER MANAGEMENT =====
    /**
     * Cancels an existing order
     */
    OrderOperationResultDTO cancelOrder(String orderId);

    /**
     * Updates the price of an existing order
     */
    OrderOperationResultDTO updateOrderPrice(String orderId, Money newPrice);

    /**
     * Cancels partial quantity of an order
     */
    OrderOperationResultDTO cancelPartialOrder(String orderId, BigDecimal quantityToCancel);

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
}