package core.ms.order.application.services;

import core.ms.order.application.dto.query.OrderDTO;
import core.ms.order.application.dto.query.OrderOperationResultDTO;
import core.ms.order.domain.entities.*;
import core.ms.order.domain.ports.inbound.OrderService;
import core.ms.order.domain.factories.OrderFactory;
import core.ms.order.domain.ports.outbound.OrderRepository;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderApplicationService implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderApplicationService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderEventPublisher eventPublisher;

    // ===== CORE ORDER OPERATIONS (Called by Event Handlers) =====

    @Override
    public OrderOperationResultDTO createBuyOrder(String portfolioId, String reservationId,
                                                  Symbol symbol, Money price, BigDecimal quantity) {
        try {
            logger.info("Creating buy order - Portfolio: {}, Symbol: {}, Price: {}, Quantity: {}",
                    portfolioId, symbol.getCode(), price, quantity);

            // Create buy order using factory
            BuyOrder buyOrder = OrderFactory.createBuyOrder(portfolioId, reservationId, symbol, price, quantity);

            // Save order
            IOrder savedOrder = orderRepository.save(buyOrder);

            // Publish event to Market Engine
            eventPublisher.publishOrderCreated(savedOrder, "BUY");

            logger.info("Buy order created successfully - Order ID: {}", savedOrder.getId());
            return new OrderOperationResultDTO(true, savedOrder.getId(),
                    "Buy order created successfully", LocalDateTime.now(), null);

        } catch (OrderFactory.OrderCreationException e) {
            logger.error("Order creation failed: {}", e.getMessage());
            return new OrderOperationResultDTO(false, null,
                    "Order creation failed: " + e.getMessage(), LocalDateTime.now(), List.of(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error creating buy order", e);
            return new OrderOperationResultDTO(false, null,
                    "Failed to create buy order: " + e.getMessage(), LocalDateTime.now(), List.of(e.getMessage()));
        }
    }

    @Override
    public OrderOperationResultDTO createSellOrder(String portfolioId, String reservationId,
                                                   Symbol symbol, Money price, BigDecimal quantity) {
        try {
            logger.info("Creating sell order - Portfolio: {}, Symbol: {}, Price: {}, Quantity: {}",
                    portfolioId, symbol.getCode(), price, quantity);

            // Create sell order using factory
            SellOrder sellOrder = OrderFactory.createSellOrder(portfolioId, reservationId, symbol, price, quantity);

            // Save order
            IOrder savedOrder = orderRepository.save(sellOrder);

            // Publish event to Market Engine
            eventPublisher.publishOrderCreated(savedOrder, "SELL");

            logger.info("Sell order created successfully - Order ID: {}", savedOrder.getId());
            return new OrderOperationResultDTO(true, savedOrder.getId(),
                    "Sell order created successfully", LocalDateTime.now(), null);

        } catch (OrderFactory.OrderCreationException e) {
            logger.error("Order creation failed: {}", e.getMessage());
            return new OrderOperationResultDTO(false, null,
                    "Order creation failed: " + e.getMessage(), LocalDateTime.now(), List.of(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error creating sell order", e);
            return new OrderOperationResultDTO(false, null,
                    "Failed to create sell order: " + e.getMessage(), LocalDateTime.now(), List.of(e.getMessage()));
        }
    }

    @Override
    public OrderOperationResultDTO cancelOrder(String orderId) {
        try {
            logger.info("Cancelling order - Order ID: {}", orderId);

            Optional<IOrder> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                logger.warn("Order not found for cancellation - Order ID: {}", orderId);
                return new OrderOperationResultDTO(false, orderId,
                        "Order not found", LocalDateTime.now(), List.of("Order not found"));
            }

            IOrder order = orderOpt.get();

            // Check if order can be cancelled
            if (order.getStatus().isTerminal()) {
                logger.warn("Cannot cancel order in terminal state - Order ID: {}, Status: {}",
                        orderId, order.getStatus().getStatus());
                return new OrderOperationResultDTO(false, orderId,
                        "Cannot cancel order in terminal state", LocalDateTime.now(),
                        List.of("Order is in terminal state"));
            }

            String orderType = order instanceof IBuyOrder ? "BUY" : "SELL";

            // Cancel the order
            order.cancel();

            // Save updated order
            orderRepository.save(order);

            // Publish event to Market Engine
            eventPublisher.publishOrderCancelled(order, orderType, "Market Engine requested cancellation");

            logger.info("Order cancelled successfully - Order ID: {}", orderId);
            return new OrderOperationResultDTO(true, orderId,
                    "Order cancelled successfully", LocalDateTime.now(), null);

        } catch (Exception e) {
            logger.error("Failed to cancel order - Order ID: " + orderId, e);
            return new OrderOperationResultDTO(false, orderId,
                    "Failed to cancel order: " + e.getMessage(), LocalDateTime.now(), List.of(e.getMessage()));
        }
    }

    @Override
    public OrderOperationResultDTO updateOrderPrice(String orderId, Money newPrice) {
        try {
            logger.info("Updating order price - Order ID: {}, New Price: {}", orderId, newPrice);

            Optional<IOrder> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                logger.warn("Order not found for price update - Order ID: {}", orderId);
                return new OrderOperationResultDTO(false, orderId,
                        "Order not found", LocalDateTime.now(), List.of("Order not found"));
            }

            IOrder order = orderOpt.get();

            // Validation
            if (order.getStatus().isTerminal()) {
                logger.warn("Cannot modify order in terminal state - Order ID: {}, Status: {}",
                        orderId, order.getStatus().getStatus());
                return new OrderOperationResultDTO(false, orderId,
                        "Cannot modify order in terminal state", LocalDateTime.now(),
                        List.of("Order is in terminal state"));
            }

            if (newPrice.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                logger.warn("Invalid price for update - Order ID: {}, Price: {}", orderId, newPrice);
                return new OrderOperationResultDTO(false, orderId,
                        "Price must be positive", LocalDateTime.now(), List.of("Price must be positive"));
            }

            if (!newPrice.getCurrency().equals(order.getPrice().getCurrency())) {
                logger.warn("Currency mismatch - Order ID: {}, Order Currency: {}, New Currency: {}",
                        orderId, order.getPrice().getCurrency(), newPrice.getCurrency());
                return new OrderOperationResultDTO(false, orderId,
                        "Currency mismatch", LocalDateTime.now(),
                        List.of("New price currency must match order currency"));
            }

            // Store old price for event
            Money oldPrice = order.getPrice();

            // Update price
            order.updatePrice(newPrice);

            // Save updated order
            orderRepository.save(order);

            // Publish event to Market Engine
            eventPublisher.publishOrderUpdated(order, oldPrice, newPrice);

            logger.info("Order price updated successfully - Order ID: {}", orderId);
            return new OrderOperationResultDTO(true, orderId,
                    "Order price updated successfully", LocalDateTime.now(), null);

        } catch (Exception e) {
            logger.error("Failed to update order price - Order ID: " + orderId, e);
            return new OrderOperationResultDTO(false, orderId,
                    "Failed to update order price: " + e.getMessage(), LocalDateTime.now(),
                    List.of(e.getMessage()));
        }
    }

    @Override
    public OrderOperationResultDTO cancelPartialOrder(String orderId, BigDecimal quantityToCancel) {
        try {
            logger.info("Partially cancelling order - Order ID: {}, Quantity: {}", orderId, quantityToCancel);

            Optional<IOrder> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                logger.warn("Order not found for partial cancellation - Order ID: {}", orderId);
                return new OrderOperationResultDTO(false, orderId,
                        "Order not found", LocalDateTime.now(), List.of("Order not found"));
            }

            IOrder order = orderOpt.get();

            // Validate partial cancellation
            if (quantityToCancel.compareTo(order.getRemainingQuantity()) > 0) {
                logger.warn("Quantity to cancel exceeds remaining - Order ID: {}, Remaining: {}, To Cancel: {}",
                        orderId, order.getRemainingQuantity(), quantityToCancel);
                return new OrderOperationResultDTO(false, orderId,
                        "Cannot cancel more than remaining quantity", LocalDateTime.now(),
                        List.of("Quantity to cancel exceeds remaining quantity"));
            }

            String orderType = order instanceof IBuyOrder ? "BUY" : "SELL";

            // Partial cancellation
            order.cancelPartial();

            // Save updated order
            orderRepository.save(order);

            // Publish event to Market Engine
            eventPublisher.publishOrderPartialCancelled(order, orderType, quantityToCancel,
                    "Market Engine requested partial cancellation");

            logger.info("Order partially cancelled successfully - Order ID: {}", orderId);
            return new OrderOperationResultDTO(true, orderId,
                    "Order partially cancelled successfully", LocalDateTime.now(), null);

        } catch (Exception e) {
            logger.error("Failed to cancel partial order - Order ID: " + orderId, e);
            return new OrderOperationResultDTO(false, orderId,
                    "Failed to cancel partial order: " + e.getMessage(), LocalDateTime.now(),
                    List.of(e.getMessage()));
        }
    }

    // ===== QUERY OPERATIONS (For Monitoring/Internal Use) =====

    @Override
    public Optional<IOrder> findOrderById(String orderId) {
        return orderRepository.findById(orderId);
    }

    @Override
    public List<IOrder> findActiveOrdersBySymbol(Symbol symbol) {
        List<IOrder> allOrders = orderRepository.findBySymbol(symbol);
        return allOrders.stream()
                .filter(IOrder::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public List<IBuyOrder> findBuyOrdersBySymbol(Symbol symbol) {
        return orderRepository.findBuyOrdersBySymbol(symbol);
    }

    @Override
    public List<ISellOrder> findSellOrdersBySymbol(Symbol symbol) {
        return orderRepository.findSellOrdersBySymbol(symbol);
    }

    @Override
    public List<IOrder> findOrdersByStatus(OrderStatusEnum status) {
        return orderRepository.findByStatus(status);
    }

    // ===== DTO QUERY METHODS (For Monitoring/Internal Use) =====

    public Optional<OrderDTO> findOrderByIdAsDTO(String orderId) {
        Optional<IOrder> order = findOrderById(orderId);
        return order.map(this::mapToOrderDTO);
    }

    public List<OrderDTO> findOrdersBySymbolAsDTO(String symbolCode) {
        Symbol symbol = createSymbol(symbolCode);
        List<IOrder> orders = findActiveOrdersBySymbol(symbol);
        return orders.stream()
                .map(this::mapToOrderDTO)
                .collect(Collectors.toList());
    }

    public List<OrderDTO> findOrdersByStatusAsDTO(String status) {
        OrderStatusEnum statusEnum = OrderStatusEnum.valueOf(status.toUpperCase());
        List<IOrder> orders = findOrdersByStatus(statusEnum);
        return orders.stream()
                .map(this::mapToOrderDTO)
                .collect(Collectors.toList());
    }

    // ===== PRIVATE HELPER METHODS =====

    private OrderDTO mapToOrderDTO(IOrder order) {
        String orderType = order instanceof IBuyOrder ? "BUY" : "SELL";

        return new OrderDTO(
                order.getId(),
                order.getSymbol().getCode(),
                order.getSymbol().getName(),
                order.getPrice().getAmount(),
                order.getPrice().getCurrency(),
                order.getQuantity(),
                order.getStatus().getStatus().name(),
                order.getExecutedQuantity(),
                order.getRemainingQuantity(),
                orderType,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private Symbol createSymbol(String symbolCode) {
        return switch (symbolCode.toUpperCase()) {
            case "BTC" -> Symbol.btcUsd();
            case "ETH" -> Symbol.ethUsd();
            case "EURUSD" -> Symbol.eurUsd();
            case "GBPUSD" -> Symbol.gbpUsd();
            default -> throw new IllegalArgumentException("Unsupported symbol: " + symbolCode);
        };
    }
}