package core.ms.order.application.services;

import core.ms.order.application.dto.command.*;
import core.ms.order.application.dto.query.OrderDTO;
import core.ms.order.application.dto.query.OrderOperationResultDTO;
import core.ms.order.domain.entities.*;
import core.ms.order.domain.ports.inbound.OrderService;
import core.ms.order.domain.factories.OrderFactory;
import core.ms.order.domain.ports.outbound.OrderRepository;
import core.ms.order.domain.ports.outbound.TransactionRepository;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderApplicationService implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private OrderEventPublisher eventPublisher;

    // ===== ORDER CREATION =====

    @Override
    public OrderOperationResultDTO createBuyOrder(String userId, Symbol symbol, Money price, BigDecimal quantity) {
        try {
            // Create buy order using factory (includes validation)
            BuyOrder buyOrder = OrderFactory.createBuyOrder(symbol, price, quantity);

            // Save order (Using Infrastructure Service)
            IOrder savedOrder = orderRepository.save(buyOrder);

            return new OrderOperationResultDTO(true, savedOrder.getId(), "Buy order created successfully",
                    LocalDateTime.now(), null);

        } catch (OrderFactory.OrderCreationException e) {
            return new OrderOperationResultDTO(false, null, "Order creation failed: " + e.getMessage(),
                    LocalDateTime.now(), List.of(e.getMessage()));
        } catch (Exception e) {
            return new OrderOperationResultDTO(false, null, "Failed to create buy order: " + e.getMessage(),
                    LocalDateTime.now(), List.of(e.getMessage()));
        }
    }

    @Override
    public OrderOperationResultDTO createSellOrder(String userId, Symbol symbol, Money price, BigDecimal quantity) {
        try {
            // Create sell order using factory (includes validation)
            SellOrder sellOrder = OrderFactory.createSellOrder(symbol, price, quantity);

            // Save order (Using Infrastructure Service)
            IOrder savedOrder = orderRepository.save(sellOrder);

            return new OrderOperationResultDTO(true, savedOrder.getId(), "Sell order created successfully",
                    LocalDateTime.now(), null);

        } catch (OrderFactory.OrderCreationException e) {
            return new OrderOperationResultDTO(false, null, "Order creation failed: " + e.getMessage(),
                    LocalDateTime.now(), List.of(e.getMessage()));
        } catch (Exception e) {
            return new OrderOperationResultDTO(false, null, "Failed to create sell order: " + e.getMessage(),
                    LocalDateTime.now(), List.of(e.getMessage()));
        }
    }

    // ===== ORDER MANAGEMENT =====

    @Override
    public OrderOperationResultDTO cancelOrder(String orderId) {
        try {
            Optional<IOrder> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return new OrderOperationResultDTO(false, orderId, "Order not found",
                        LocalDateTime.now(), List.of("Order not found"));
            }

            IOrder order = orderOpt.get();

            // Check if order can be cancelled (simple domain validation)
            if (order.getStatus().isTerminal()) {
                return new OrderOperationResultDTO(false, orderId, "Cannot cancel order in terminal state",
                        LocalDateTime.now(), List.of("Order is in terminal state"));
            }

            // Cancel the order (Domain Logic)
            order.cancel();

            // Save updated order (Using Infrastructure Service)
            orderRepository.save(order);

            return new OrderOperationResultDTO(true, orderId, "Order cancelled successfully",
                    LocalDateTime.now(), null);

        } catch (Exception e) {
            return new OrderOperationResultDTO(false, orderId, "Failed to cancel order: " + e.getMessage(),
                    LocalDateTime.now(), List.of(e.getMessage()));
        }
    }

    @Override
    public OrderOperationResultDTO updateOrderPrice(String orderId, Money newPrice) {
        try {
            Optional<IOrder> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return new OrderOperationResultDTO(false, orderId, "Order not found",
                        LocalDateTime.now(), List.of("Order not found"));
            }

            IOrder order = orderOpt.get();

            // Simple validation
            if (order.getStatus().isTerminal()) {
                return new OrderOperationResultDTO(false, orderId, "Cannot modify order in terminal state",
                        LocalDateTime.now(), List.of("Order is in terminal state"));
            }

            if (newPrice.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return new OrderOperationResultDTO(false, orderId, "Price must be positive",
                        LocalDateTime.now(), List.of("Price must be positive"));
            }

            if (!newPrice.getCurrency().equals(order.getPrice().getCurrency())) {
                return new OrderOperationResultDTO(false, orderId, "Currency mismatch",
                        LocalDateTime.now(), List.of("New price currency must match order currency"));
            }

            // Update price (Domain Logic)
            order.updatePrice(newPrice);

            // Save updated order (Using Infrastructure Service)
            orderRepository.save(order);

            return new OrderOperationResultDTO(true, orderId, "Order price updated successfully",
                    LocalDateTime.now(), null);

        } catch (Exception e) {
            return new OrderOperationResultDTO(false, orderId, "Failed to update order price: " + e.getMessage(),
                    LocalDateTime.now(), List.of(e.getMessage()));
        }
    }

    @Override
    public OrderOperationResultDTO cancelPartialOrder(String orderId, BigDecimal quantityToCancel) {
        try {
            Optional<IOrder> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return new OrderOperationResultDTO(false, orderId, "Order not found",
                        LocalDateTime.now(), List.of("Order not found"));
            }

            IOrder order = orderOpt.get();

            // Validate partial cancellation
            if (quantityToCancel.compareTo(order.getRemainingQuantity()) > 0) {
                return new OrderOperationResultDTO(false, orderId, "Cannot cancel more than remaining quantity",
                        LocalDateTime.now(), List.of("Quantity to cancel exceeds remaining quantity"));
            }

            // For partial cancellation (Domain Logic)
            order.cancelPartial();

            // Save updated order (Using Infrastructure Service)
            orderRepository.save(order);

            return new OrderOperationResultDTO(true, orderId, "Order partially cancelled successfully",
                    LocalDateTime.now(), null);

        } catch (Exception e) {
            return new OrderOperationResultDTO(false, orderId, "Failed to cancel partial order: " + e.getMessage(),
                    LocalDateTime.now(), List.of(e.getMessage()));
        }
    }

    // ===== ORDER QUERIES =====

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

    // ===== ORDER VALIDATION (Simplified) =====

    public boolean validateOrderCreation(String userId, Symbol symbol, Money price, BigDecimal quantity) {
        // Basic validation - most validation is handled by the factory
        return userId != null && !userId.trim().isEmpty() &&
                symbol != null && price != null && quantity != null;
    }

    public boolean validateOrderCancellation(String orderId) {
        Optional<IOrder> orderOpt = orderRepository.findById(orderId);
        return orderOpt.isPresent() && !orderOpt.get().getStatus().isTerminal();
    }

    // ===== DTO ORCHESTRATION METHODS =====

    public OrderOperationResultDTO createBuyOrder(CreateBuyOrderCommand command) {
        Symbol symbol = createSymbol(command.getSymbolCode());
        Money price = Money.of(command.getPrice(), command.getCurrency());

        return createBuyOrder(
                command.getUserId(),
                symbol,
                price,
                command.getQuantity()
        );
    }

    public OrderOperationResultDTO createSellOrder(CreateSellOrderCommand command) {
        Symbol symbol = createSymbol(command.getSymbolCode());
        Money price = Money.of(command.getPrice(), command.getCurrency());

        return createSellOrder(
                command.getUserId(),
                symbol,
                price,
                command.getQuantity()
        );
    }

    public OrderOperationResultDTO updateOrderPrice(UpdateOrderPriceCommand command) {
        Money newPrice = Money.of(command.getNewPrice(), command.getCurrency());

        return updateOrderPrice(
                command.getOrderId(),
                newPrice
        );
    }

    public OrderOperationResultDTO cancelOrder(CancelOrderCommand command) {
        return cancelOrder(command.getOrderId());
    }

    public OrderOperationResultDTO cancelPartialOrder(CancelPartialOrderCommand command) {
        return cancelPartialOrder(
                command.getOrderId(),
                command.getQuantityToCancel()
        );
    }

    // DTO Query Methods
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

    public List<OrderDTO> findBuyOrdersBySymbolAsDTO(String symbolCode) {
        Symbol symbol = createSymbol(symbolCode);
        List<IBuyOrder> buyOrders = findBuyOrdersBySymbol(symbol);
        return buyOrders.stream()
                .map(this::mapToOrderDTO)
                .collect(Collectors.toList());
    }

    public List<OrderDTO> findSellOrdersBySymbolAsDTO(String symbolCode) {
        Symbol symbol = createSymbol(symbolCode);
        List<ISellOrder> sellOrders = findSellOrdersBySymbol(symbol);
        return sellOrders.stream()
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