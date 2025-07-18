package core.ms.order.application.services;

import core.ms.order.application.dto.command.*;
import core.ms.order.application.dto.query.OrderDTO;
import core.ms.order.application.dto.query.OrderOperationResultDTO;
import core.ms.order.domain.entities.*;
import core.ms.order.domain.ports.inbound.*;
import core.ms.order.domain.ports.outbound.OrderRepository;
import core.ms.order.domain.validators.ValidationErrorMessage;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderApplicationService implements OrderService {

    // Inject infrastructure services (outbound port implementations)
    @Autowired
    private OrderRepository orderRepository; // This will be OrderRepositoryService

    @Autowired
    private OrderValidationService orderValidationService; // This will be OrderValidationApplicationService

    // ===== ORDER CREATION =====

    @Override
    public OrderOperationResult createBuyOrder(String userId, Symbol symbol, Money price, BigDecimal quantity) {
        try {
            // Validate order creation
            ValidationResult validationResult = validateOrderCreation(userId, symbol, price, quantity);
            if (!validationResult.isValid()) {
                List<String> errorMessages = validationResult.getErrors().stream()
                        .map(ValidationErrorMessage::getMessage)
                        .toList();
                return OrderOperationResult.failure(null, "Order validation failed", errorMessages);
            }

            // Generate order ID
            String orderId = generateOrderId();

            // Create buy order (Domain Entity)
            BuyOrder buyOrder = new BuyOrder(orderId, symbol, price, quantity);

            // Save order (Using Infrastructure Service)
            IOrder savedOrder = orderRepository.save(buyOrder);

            return OrderOperationResult.success(savedOrder.getId(), "Buy order created successfully");

        } catch (Exception e) {
            return OrderOperationResult.failure(null, "Failed to create buy order: " + e.getMessage(),
                    List.of(e.getMessage()));
        }
    }

    @Override
    public OrderOperationResult createSellOrder(String userId, Symbol symbol, Money price, BigDecimal quantity) {
        try {
            // Validate order creation
            ValidationResult validationResult = validateOrderCreation(userId, symbol, price, quantity);
            if (!validationResult.isValid()) {
                List<String> errorMessages = validationResult.getErrors().stream()
                        .map(ValidationErrorMessage::getMessage)
                        .toList();
                return OrderOperationResult.failure(null, "Order validation failed", errorMessages);
            }

            // Generate order ID
            String orderId = generateOrderId();

            // Create sell order (Domain Entity)
            SellOrder sellOrder = new SellOrder(orderId, symbol, price, quantity);

            // Save order (Using Infrastructure Service)
            IOrder savedOrder = orderRepository.save(sellOrder);

            return OrderOperationResult.success(savedOrder.getId(), "Sell order created successfully");

        } catch (Exception e) {
            return OrderOperationResult.failure(null, "Failed to create sell order: " + e.getMessage(),
                    List.of(e.getMessage()));
        }
    }

    // ===== ORDER MANAGEMENT =====

    @Override
    public OrderOperationResult cancelOrder(String orderId) {
        try {
            Optional<IOrder> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return OrderOperationResult.failure(orderId, "Order not found", List.of("Order not found"));
            }

            IOrder order = orderOpt.get();

            // Validate cancellation
            ValidationResult validationResult = validateOrderCancellation(orderId);
            if (!validationResult.isValid()) {
                List<String> errorMessages = validationResult.getErrors().stream()
                        .map(ValidationErrorMessage::getMessage)
                        .toList();
                return OrderOperationResult.failure(orderId, "Order cancellation validation failed", errorMessages);
            }

            // Cancel the order (Domain Logic)
            order.cancel();

            // Save updated order (Using Infrastructure Service)
            orderRepository.save(order);

            return OrderOperationResult.success(orderId, "Order cancelled successfully");

        } catch (Exception e) {
            return OrderOperationResult.failure(orderId, "Failed to cancel order: " + e.getMessage(),
                    List.of(e.getMessage()));
        }
    }

    @Override
    public OrderOperationResult updateOrderPrice(String orderId, Money newPrice) {
        try {
            Optional<IOrder> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return OrderOperationResult.failure(orderId, "Order not found", List.of("Order not found"));
            }

            IOrder order = orderOpt.get();

            // Validate price update
            List<ValidationErrorMessage> errors = orderValidationService.validateOrderModification(order, newPrice);
            if (!errors.isEmpty()) {
                List<String> errorMessages = errors.stream()
                        .map(ValidationErrorMessage::getMessage)
                        .toList();
                return OrderOperationResult.failure(orderId, "Order price update validation failed", errorMessages);
            }

            // Update price (Domain Logic)
            order.updatePrice(newPrice);

            // Save updated order (Using Infrastructure Service)
            orderRepository.save(order);

            return OrderOperationResult.success(orderId, "Order price updated successfully");

        } catch (Exception e) {
            return OrderOperationResult.failure(orderId, "Failed to update order price: " + e.getMessage(),
                    List.of(e.getMessage()));
        }
    }

    @Override
    public OrderOperationResult cancelPartialOrder(String orderId, BigDecimal quantityToCancel) {
        try {
            Optional<IOrder> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return OrderOperationResult.failure(orderId, "Order not found", List.of("Order not found"));
            }

            IOrder order = orderOpt.get();

            // Validate partial cancellation
            if (quantityToCancel.compareTo(order.getRemainingQuantity()) > 0) {
                return OrderOperationResult.failure(orderId, "Cannot cancel more than remaining quantity",
                        List.of("Quantity to cancel exceeds remaining quantity"));
            }

            // For partial cancellation (Domain Logic)
            order.cancelPartial();

            // Save updated order (Using Infrastructure Service)
            orderRepository.save(order);

            return OrderOperationResult.success(orderId, "Order partially cancelled successfully");

        } catch (Exception e) {
            return OrderOperationResult.failure(orderId, "Failed to cancel partial order: " + e.getMessage(),
                    List.of(e.getMessage()));
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

    // ===== ORDER VALIDATION =====

    @Override
    public ValidationResult validateOrderCreation(String userId, Symbol symbol, Money price, BigDecimal quantity) {
        List<ValidationErrorMessage> errors = orderValidationService.validateOrderCreation(userId, symbol, price, quantity);

        if (errors.isEmpty()) {
            return ValidationResult.valid();
        } else {
            return ValidationResult.invalid(errors);
        }
    }

    @Override
    public ValidationResult validateOrderCancellation(String orderId) {
        Optional<IOrder> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ValidationResult.invalid(List.of(new ValidationErrorMessage("Order not found")));
        }

        List<ValidationErrorMessage> errors = orderValidationService.validateOrderCancellation(orderOpt.get());

        if (errors.isEmpty()) {
            return ValidationResult.valid();
        } else {
            return ValidationResult.invalid(errors);
        }
    }

    // ===== DTO ORCHESTRATION METHODS =====

    public OrderOperationResultDTO createBuyOrder(CreateBuyOrderCommand command) {
        Symbol symbol = createSymbol(command.getSymbolCode());
        Money price = Money.of(command.getPrice(), command.getCurrency());

        OrderOperationResult result = createBuyOrder(
                command.getUserId(),
                symbol,
                price,
                command.getQuantity()
        );

        return mapToOrderOperationResultDTO(result);
    }

    public OrderOperationResultDTO createSellOrder(CreateSellOrderCommand command) {
        Symbol symbol = createSymbol(command.getSymbolCode());
        Money price = Money.of(command.getPrice(), command.getCurrency());

        OrderOperationResult result = createSellOrder(
                command.getUserId(),
                symbol,
                price,
                command.getQuantity()
        );

        return mapToOrderOperationResultDTO(result);
    }

    public OrderOperationResultDTO updateOrderPrice(UpdateOrderPriceCommand command) {
        Money newPrice = Money.of(command.getNewPrice(), command.getCurrency());

        OrderOperationResult result = updateOrderPrice(
                command.getOrderId(),
                newPrice
        );

        return mapToOrderOperationResultDTO(result);
    }

    public OrderOperationResultDTO cancelOrder(CancelOrderCommand command) {
        OrderOperationResult result = cancelOrder(command.getOrderId());
        return mapToOrderOperationResultDTO(result);
    }

    public OrderOperationResultDTO cancelPartialOrder(CancelPartialOrderCommand command) {
        OrderOperationResult result = cancelPartialOrder(
                command.getOrderId(),
                command.getQuantityToCancel()
        );

        return mapToOrderOperationResultDTO(result);
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

    private String generateOrderId() {
        return "ORDER_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private OrderOperationResultDTO mapToOrderOperationResultDTO(OrderOperationResult result) {
        return new OrderOperationResultDTO(
                result.isSuccess(),
                result.getOrderId(),
                result.getMessage(),
                result.getTimestamp(),
                result.getErrors()
        );
    }

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
