package core.ms.order;
import core.ms.order.application.dto.command.*;
import core.ms.order.application.dto.query.OrderDTO;
import core.ms.order.application.dto.query.OrderOperationResultDTO;
import core.ms.order.application.services.OrderApplicationService;
import core.ms.order.domain.entities.*;
import core.ms.order.domain.ports.outbound.OrderRepository;
import core.ms.order.domain.validators.ValidationErrorMessage;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderApplicationService Infrastructure Tests")
class OrderApplicationServiceInfrastructureTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderValidationService orderValidationService;

    @InjectMocks
    private OrderApplicationService orderApplicationService;

    @Nested
    @DisplayName("🏗️ Order Creation Infrastructure Tests")
    class OrderCreationInfrastructureTests {

        @Test
        @DisplayName("Should create buy order with repository interaction")
        void shouldCreateBuyOrderWithRepositoryInteraction() {
            // Given
            CreateBuyOrderCommand command = new CreateBuyOrderCommand(
                    "testUser",
                    "BTC",
                    new BigDecimal("45000.00"),
                    Currency.USD,
                    new BigDecimal("0.1")
            );

            // Mock validation success
            when(orderValidationService.validateOrderCreation(any(), any(), any(), any()))
                    .thenReturn(new ArrayList<>());

            // Mock repository save
            BuyOrder savedOrder = new BuyOrder("ORDER_123", Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), new BigDecimal("0.1"));
            when(orderRepository.save(any(BuyOrder.class))).thenReturn(savedOrder);

            // When
            OrderOperationResultDTO result = orderApplicationService.createBuyOrder(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getOrderId()).isEqualTo("ORDER_123");
            assertThat(result.getMessage()).contains("Buy order created successfully");
            assertThat(result.getErrors()).isEmpty();

            // Verify interactions
            verify(orderValidationService).validateOrderCreation(
                    eq("testUser"), any(Symbol.class), any(Money.class), any(BigDecimal.class));
            verify(orderRepository).save(any(BuyOrder.class));
        }

        @Test
        @DisplayName("Should create sell order with repository interaction")
        void shouldCreateSellOrderWithRepositoryInteraction() {
            // Given
            CreateSellOrderCommand command = new CreateSellOrderCommand(
                    "testUser",
                    "ETH",
                    new BigDecimal("3000.00"),
                    Currency.USD,
                    new BigDecimal("1.5")
            );

            // Mock validation success
            when(orderValidationService.validateOrderCreation(any(), any(), any(), any()))
                    .thenReturn(new ArrayList<>());

            // Mock repository save
            SellOrder savedOrder = new SellOrder("ORDER_456", Symbol.ethUsd(),
                    Money.of("3000.00", Currency.USD), new BigDecimal("1.5"));
            when(orderRepository.save(any(SellOrder.class))).thenReturn(savedOrder);

            // When
            OrderOperationResultDTO result = orderApplicationService.createSellOrder(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getOrderId()).isEqualTo("ORDER_456");
            assertThat(result.getMessage()).contains("Sell order created successfully");
            assertThat(result.getErrors()).isEmpty();

            // Verify interactions
            verify(orderValidationService).validateOrderCreation(
                    eq("testUser"), any(Symbol.class), any(Money.class), any(BigDecimal.class));
            verify(orderRepository).save(any(SellOrder.class));
        }

        @Test
        @DisplayName("Should handle validation failure during order creation")
        void shouldHandleValidationFailureDuringOrderCreation() {
            // Given
            CreateBuyOrderCommand command = new CreateBuyOrderCommand(
                    "testUser",
                    "INVALID",
                    new BigDecimal("100.00"),
                    Currency.USD,
                    new BigDecimal("1.0")
            );

            // Mock validation failure
            List<ValidationErrorMessage> errors = List.of(
                    new ValidationErrorMessage("Unsupported symbol: INVALID")
            );
            when(orderValidationService.validateOrderCreation(any(), any(), any(), any()))
                    .thenReturn(errors);

            // When
            OrderOperationResultDTO result = orderApplicationService.createBuyOrder(command);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getOrderId()).isNull();
            assertThat(result.getMessage()).contains("Order validation failed");
            assertThat(result.getErrors()).containsExactly("Unsupported symbol: INVALID");

            // Verify no repository save
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle repository exception during order creation")
        void shouldHandleRepositoryExceptionDuringOrderCreation() {
            // Given
            CreateBuyOrderCommand command = new CreateBuyOrderCommand(
                    "testUser",
                    "BTC",
                    new BigDecimal("45000.00"),
                    Currency.USD,
                    new BigDecimal("0.1")
            );

            // Mock validation success
            when(orderValidationService.validateOrderCreation(any(), any(), any(), any()))
                    .thenReturn(new ArrayList<>());

            // Mock repository exception
            when(orderRepository.save(any(BuyOrder.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // When
            OrderOperationResultDTO result = orderApplicationService.createBuyOrder(command);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getOrderId()).isNull();
            assertThat(result.getMessage()).contains("Failed to create buy order");
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0)).contains("Database connection failed");
        }
    }

    @Nested
    @DisplayName("🔄 Order Management Infrastructure Tests")
    class OrderManagementInfrastructureTests {

        @Test
        @DisplayName("Should cancel order with repository interaction")
        void shouldCancelOrderWithRepositoryInteraction() {
            // Given
            String orderId = "ORDER_123";
            CancelOrderCommand command = new CancelOrderCommand(orderId);

            BuyOrder existingOrder = new BuyOrder("ORDER_123", Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), new BigDecimal("0.1"));

            // Mock repository find
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

            // Mock validation success
            when(orderValidationService.validateOrderCancellation(any()))
                    .thenReturn(new ArrayList<>());

            // Mock repository save
            when(orderRepository.save(any(IOrder.class))).thenReturn(existingOrder);

            // When
            OrderOperationResultDTO result = orderApplicationService.cancelOrder(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getOrderId()).isEqualTo(orderId);
            assertThat(result.getMessage()).contains("Order cancelled successfully");

            // Verify interactions
            verify(orderRepository).findById(orderId);
            verify(orderValidationService).validateOrderCancellation(existingOrder);
            verify(orderRepository).save(existingOrder);

            // Verify order state change
            assertThat(existingOrder.getStatus().getStatus()).isEqualTo(OrderStatusEnum.CANCELLED);
        }

        @Test
        @DisplayName("Should handle order not found during cancellation")
        void shouldHandleOrderNotFoundDuringCancellation() {
            // Given
            String orderId = "NONEXISTENT_ORDER";
            CancelOrderCommand command = new CancelOrderCommand(orderId);

            // Mock repository - order not found
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            // When
            OrderOperationResultDTO result = orderApplicationService.cancelOrder(command);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getOrderId()).isEqualTo(orderId);
            assertThat(result.getMessage()).contains("Order not found");
            assertThat(result.getErrors()).containsExactly("Order not found");

            // Verify no validation or save calls
            verify(orderValidationService, never()).validateOrderCancellation(any());
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should update order price with repository interaction")
        void shouldUpdateOrderPriceWithRepositoryInteraction() {
            // Given
            String orderId = "ORDER_123";
            UpdateOrderPriceCommand command = new UpdateOrderPriceCommand(
                    orderId, new BigDecimal("46000.00"), Currency.USD);

            BuyOrder existingOrder = new BuyOrder("ORDER_123", Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), new BigDecimal("0.1"));

            // Mock repository find
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

            // Mock validation success
            when(orderValidationService.validateOrderModification(any(), any()))
                    .thenReturn(new ArrayList<>());

            // Mock repository save
            when(orderRepository.save(any(IOrder.class))).thenReturn(existingOrder);

            // When
            OrderOperationResultDTO result = orderApplicationService.updateOrderPrice(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getOrderId()).isEqualTo(orderId);
            assertThat(result.getMessage()).contains("Order price updated successfully");

            // Verify interactions
            verify(orderRepository).findById(orderId);
            verify(orderValidationService).validateOrderModification(eq(existingOrder), any(Money.class));
            verify(orderRepository).save(existingOrder);

            // Verify price update
            assertThat(existingOrder.getPrice()).isEqualTo(Money.of("46000.00", Currency.USD));
        }

        @Test
        @DisplayName("Should handle partial order cancellation")
        void shouldHandlePartialOrderCancellation() {
            // Given
            String orderId = "ORDER_123";
            CancelPartialOrderCommand command = new CancelPartialOrderCommand(
                    orderId, new BigDecimal("0.05"));

            BuyOrder existingOrder = new BuyOrder("ORDER_123", Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), new BigDecimal("0.1"));

            // Mock repository find
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

            // Mock repository save
            when(orderRepository.save(any(IOrder.class))).thenReturn(existingOrder);

            // When
            OrderOperationResultDTO result = orderApplicationService.cancelPartialOrder(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getOrderId()).isEqualTo(orderId);
            assertThat(result.getMessage()).contains("Order partially cancelled successfully");

            // Verify interactions
            verify(orderRepository).findById(orderId);
            verify(orderRepository).save(existingOrder);
        }
    }

    @Nested
    @DisplayName("🔍 Order Query Infrastructure Tests")
    class OrderQueryInfrastructureTests {

        @Test
        @DisplayName("Should find order by ID with DTO mapping")
        void shouldFindOrderByIdWithDtoMapping() {
            // Given
            String orderId = "ORDER_123";
            BuyOrder order = new BuyOrder("ORDER_123", Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), new BigDecimal("0.1"));

            // Mock repository find
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // When
            Optional<OrderDTO> result = orderApplicationService.findOrderByIdAsDTO(orderId);

            // Then
            assertThat(result).isPresent();
            OrderDTO orderDTO = result.get();
            assertThat(orderDTO.getId()).isEqualTo("ORDER_123");
            assertThat(orderDTO.getSymbolCode()).isEqualTo("BTC");
            assertThat(orderDTO.getPrice()).isEqualTo(new BigDecimal("45000.00"));
            assertThat(orderDTO.getQuantity()).isEqualTo(new BigDecimal("0.1"));
            assertThat(orderDTO.getOrderType()).isEqualTo("BUY");
            assertThat(orderDTO.getStatus()).isEqualTo("PENDING");

            // Verify repository interaction
            verify(orderRepository).findById(orderId);
        }

        @Test
        @DisplayName("Should find orders by symbol with DTO mapping")
        void shouldFindOrdersBySymbolWithDtoMapping() {
            // Given
            String symbolCode = "BTC";
            List<IOrder> orders = List.of(
                    new BuyOrder("ORDER_123", Symbol.btcUsd(), Money.of("45000.00", Currency.USD), new BigDecimal("0.1")),
                    new SellOrder("ORDER_456", Symbol.btcUsd(), Money.of("44000.00", Currency.USD), new BigDecimal("0.2"))
            );

            // Mock repository find
            when(orderRepository.findBySymbol(any(Symbol.class))).thenReturn(orders);

            // When
            List<OrderDTO> result = orderApplicationService.findOrdersBySymbolAsDTO(symbolCode);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getSymbolCode()).isEqualTo("BTC");
            assertThat(result.get(0).getOrderType()).isEqualTo("BUY");
            assertThat(result.get(1).getSymbolCode()).isEqualTo("BTC");
            assertThat(result.get(1).getOrderType()).isEqualTo("SELL");

            // Verify repository interaction
            verify(orderRepository).findBySymbol(any(Symbol.class));
        }

        @Test
        @DisplayName("Should find orders by status with DTO mapping")
        void shouldFindOrdersByStatusWithDtoMapping() {
            // Given
            String status = "PENDING";
            List<IOrder> orders = List.of(
                    new BuyOrder("ORDER_123", Symbol.btcUsd(), Money.of("45000.00", Currency.USD), new BigDecimal("0.1"))
            );

            // Mock repository find
            when(orderRepository.findByStatus(OrderStatusEnum.PENDING)).thenReturn(orders);

            // When
            List<OrderDTO> result = orderApplicationService.findOrdersByStatusAsDTO(status);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("PENDING");

            // Verify repository interaction
            verify(orderRepository).findByStatus(OrderStatusEnum.PENDING);
        }

        @Test
        @DisplayName("Should find buy orders by symbol")
        void shouldFindBuyOrdersBySymbol() {
            // Given
            String symbolCode = "ETH";
            List<IBuyOrder> buyOrders = List.of(
                    new BuyOrder("ORDER_123", Symbol.ethUsd(), Money.of("3000.00", Currency.USD), new BigDecimal("1.0"))
            );

            // Mock repository find
            when(orderRepository.findBuyOrdersBySymbol(any(Symbol.class))).thenReturn(buyOrders);

            // When
            List<OrderDTO> result = orderApplicationService.findBuyOrdersBySymbolAsDTO(symbolCode);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSymbolCode()).isEqualTo("ETH");
            assertThat(result.get(0).getOrderType()).isEqualTo("BUY");

            // Verify repository interaction
            verify(orderRepository).findBuyOrdersBySymbol(any(Symbol.class));
        }

        @Test
        @DisplayName("Should find sell orders by symbol")
        void shouldFindSellOrdersBySymbol() {
            // Given
            String symbolCode = "ETH";
            List<ISellOrder> sellOrders = List.of(
                    new SellOrder("ORDER_456", Symbol.ethUsd(), Money.of("3000.00", Currency.USD), new BigDecimal("1.5"))
            );

            // Mock repository find
            when(orderRepository.findSellOrdersBySymbol(any(Symbol.class))).thenReturn(sellOrders);

            // When
            List<OrderDTO> result = orderApplicationService.findSellOrdersBySymbolAsDTO(symbolCode);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSymbolCode()).isEqualTo("ETH");
            assertThat(result.get(0).getOrderType()).isEqualTo("SELL");

            // Verify repository interaction
            verify(orderRepository).findSellOrdersBySymbol(any(Symbol.class));
        }
    }

    @Nested
    @DisplayName("🛡️ Validation Infrastructure Tests")
    class ValidationInfrastructureTests {

        @Test
        @DisplayName("Should handle validation service integration")
        void shouldHandleValidationServiceIntegration() {
            // Given
            String userId = "testUser";
            Symbol symbol = Symbol.btcUsd();
            Money price = Money.of("45000.00", Currency.USD);
            BigDecimal quantity = new BigDecimal("0.1");

            // Mock validation success
            when(orderValidationService.validateOrderCreation(userId, symbol, price, quantity))
                    .thenReturn(new ArrayList<>());

            // When
            ValidationResult result = orderApplicationService.validateOrderCreation(userId, symbol, price, quantity);

            // Then
            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrors()).isEmpty();

            // Verify validation service interaction
            verify(orderValidationService).validateOrderCreation(userId, symbol, price, quantity);
        }

        @Test
        @DisplayName("Should handle validation errors from validation service")
        void shouldHandleValidationErrorsFromValidationService() {
            // Given
            String userId = null; // Invalid
            Symbol symbol = Symbol.btcUsd();
            Money price = Money.of("45000.00", Currency.USD);
            BigDecimal quantity = new BigDecimal("0.1");

            List<ValidationErrorMessage> errors = List.of(
                    new ValidationErrorMessage("User ID cannot be null or empty")
            );

            // Mock validation failure
            when(orderValidationService.validateOrderCreation(userId, symbol, price, quantity))
                    .thenReturn(errors);

            // When
            ValidationResult result = orderApplicationService.validateOrderCreation(userId, symbol, price, quantity);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0).getMessage()).contains("User ID cannot be null or empty");

            // Verify validation service interaction
            verify(orderValidationService).validateOrderCreation(userId, symbol, price, quantity);
        }

        @Test
        @DisplayName("Should validate order cancellation with repository lookup")
        void shouldValidateOrderCancellationWithRepositoryLookup() {
            // Given
            String orderId = "ORDER_123";
            BuyOrder order = new BuyOrder("ORDER_123", Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), new BigDecimal("0.1"));

            // Mock repository find
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // Mock validation success
            when(orderValidationService.validateOrderCancellation(order))
                    .thenReturn(new ArrayList<>());

            // When
            ValidationResult result = orderApplicationService.validateOrderCancellation(orderId);

            // Then
            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrors()).isEmpty();

            // Verify interactions
            verify(orderRepository).findById(orderId);
            verify(orderValidationService).validateOrderCancellation(order);
        }

        @Test
        @DisplayName("Should handle order not found during validation")
        void shouldHandleOrderNotFoundDuringValidation() {
            // Given
            String orderId = "NONEXISTENT_ORDER";

            // Mock repository - order not found
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            // When
            ValidationResult result = orderApplicationService.validateOrderCancellation(orderId);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0).getMessage()).contains("Order not found");

            // Verify no validation service call
            verify(orderValidationService, never()).validateOrderCancellation(any());
        }
    }

    @Nested
    @DisplayName("🎯 DTO Mapping Infrastructure Tests")
    class DtoMappingInfrastructureTests {

        @Test
        @DisplayName("Should map OrderOperationResult to DTO correctly")
        void shouldMapOrderOperationResultToDtoCorrectly() {
            // Given
            CreateBuyOrderCommand command = new CreateBuyOrderCommand(
                    "testUser", "BTC", new BigDecimal("45000.00"), Currency.USD, new BigDecimal("0.1"));

            // Mock successful creation
            when(orderValidationService.validateOrderCreation(any(), any(), any(), any()))
                    .thenReturn(new ArrayList<>());

            BuyOrder savedOrder = new BuyOrder("ORDER_123", Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), new BigDecimal("0.1"));
            when(orderRepository.save(any(BuyOrder.class))).thenReturn(savedOrder);

            // When
            OrderOperationResultDTO result = orderApplicationService.createBuyOrder(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getOrderId()).isEqualTo("ORDER_123");
            assertThat(result.getMessage()).isNotNull();
            assertThat(result.getTimestamp()).isNotNull();
            assertThat(result.getErrors()).isEmpty();
        }

        @Test
        @DisplayName("Should handle symbol mapping correctly")
        void shouldHandleSymbolMappingCorrectly() {
            // Given - Test different symbols
            String[] symbolCodes = {"BTC", "ETH", "EURUSD", "GBPUSD"};

            for (String symbolCode : symbolCodes) {
                // Mock repository to return empty list
                when(orderRepository.findBySymbol(any(Symbol.class))).thenReturn(new ArrayList<>());

                // When
                List<OrderDTO> result = orderApplicationService.findOrdersBySymbolAsDTO(symbolCode);

                // Then
                assertThat(result).isNotNull();

                // Verify correct symbol was created and used
                verify(orderRepository).findBySymbol(argThat(symbol ->
                        symbol.getCode().equals(symbolCode)));
            }
        }

        @Test
        @DisplayName("Should handle invalid symbol mapping")
        void shouldHandleInvalidSymbolMapping() {
            // Given
            String invalidSymbolCode = "INVALID";

            // When & Then
            assertThatThrownBy(() -> orderApplicationService.findOrdersBySymbolAsDTO(invalidSymbolCode))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported symbol: INVALID");

            // Verify no repository call
            verify(orderRepository, never()).findBySymbol(any());
        }
    }
}
