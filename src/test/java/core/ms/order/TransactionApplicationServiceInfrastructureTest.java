package core.ms.order;

import core.ms.order.application.dto.command.CreateTransactionCommand;
import core.ms.order.application.dto.query.TransactionDTO;
import core.ms.order.application.dto.query.TransactionResultDTO;
import core.ms.order.application.dto.query.TransactionStatisticsDTO;
import core.ms.order.application.services.TransactionApplicationService;
import core.ms.order.domain.entities.*;
import core.ms.order.domain.ports.inbound.OrderValidationService;
import core.ms.order.domain.ports.inbound.TransactionVolume;
import core.ms.order.domain.ports.outbound.OrderRepository;
import core.ms.order.domain.ports.outbound.TransactionRepository;
import core.ms.order.domain.validators.ValidationErrorMessage;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionApplicationService Infrastructure Tests")
class TransactionApplicationServiceInfrastructureTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderValidationService orderValidationService;

    @InjectMocks
    private TransactionApplicationService transactionApplicationService;

    @Nested
    @DisplayName("🏗️ Transaction Creation Infrastructure Tests")
    class TransactionCreationInfrastructureTests {

        @Test
        @DisplayName("Should create transaction with direct order references")
        void shouldCreateTransactionWithDirectOrderReferences() {
            // Given
            IBuyOrder buyOrder = new BuyOrder("BUY_ORDER_123", Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), new BigDecimal("1.0"));

            ISellOrder sellOrder = new SellOrder("SELL_ORDER_456", Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD), new BigDecimal("0.8"));

            Money executionPrice = Money.of("44500.00", Currency.USD);
            BigDecimal quantity = new BigDecimal("0.8");

            // Mock validation success
            when(orderValidationService.validateTransactionCreation(any(), any(), any(), any()))
                    .thenReturn(new ArrayList<>());

            // Mock transaction save
            Transaction savedTransaction = new Transaction("TXN_789", Symbol.btcUsd(),
                    buyOrder, sellOrder, executionPrice, quantity);
            when(transactionRepository.save(any(ITransaction.class))).thenReturn(savedTransaction);

            // Mock order saves
            when(orderRepository.save(any(IBuyOrder.class))).thenReturn(buyOrder);
            when(orderRepository.save(any(ISellOrder.class))).thenReturn(sellOrder);

            // When
            var result = transactionApplicationService.createTransaction(
                    buyOrder, sellOrder, executionPrice, quantity);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getTransactionId()).isEqualTo("TXN_789");
            assertThat(result.getMessage()).contains("Transaction created successfully");

            // Verify interactions
            verify(orderValidationService).validateTransactionCreation(buyOrder, sellOrder, executionPrice, quantity);
            verify(transactionRepository).save(any(ITransaction.class));
            verify(orderRepository).save(buyOrder);
            verify(orderRepository).save(sellOrder);
        }

        @Test
        @DisplayName("Should create transaction by order IDs with repository lookups")
        void shouldCreateTransactionByOrderIdsWithRepositoryLookups() {
            // Given
            CreateTransactionCommand command = new CreateTransactionCommand(
                    "BUY_ORDER_123",
                    "SELL_ORDER_456",
                    new BigDecimal("44500.00"),
                    Currency.USD,
                    new BigDecimal("0.8")
            );

            // Mock order repository lookups
            IBuyOrder buyOrder = new BuyOrder("BUY_ORDER_123", Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), new BigDecimal("1.0"));
            ISellOrder sellOrder = new SellOrder("SELL_ORDER_456", Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD), new BigDecimal("0.8"));

            when(orderRepository.findById("BUY_ORDER_123")).thenReturn(Optional.of(buyOrder));
            when(orderRepository.findById("SELL_ORDER_456")).thenReturn(Optional.of(sellOrder));

            // Mock validation success
            when(orderValidationService.validateTransactionCreation(any(), any(), any(), any()))
                    .thenReturn(new ArrayList<>());

            // Mock transaction save
            Transaction savedTransaction = new Transaction("TXN_789", Symbol.btcUsd(),
                    buyOrder, sellOrder, Money.of("44500.00", Currency.USD), new BigDecimal("0.8"));
            when(transactionRepository.save(any(ITransaction.class))).thenReturn(savedTransaction);

            // Mock order saves
            when(orderRepository.save(any(IBuyOrder.class))).thenReturn(buyOrder);
            when(orderRepository.save(any(ISellOrder.class))).thenReturn(sellOrder);

            // When
            TransactionResultDTO result = transactionApplicationService.createTransaction(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getTransactionId()).isEqualTo("TXN_789");
            assertThat(result.getMessage()).contains("Transaction created successfully");

            // Verify repository interactions
            verify(orderRepository).findById("BUY_ORDER_123");
            verify(orderRepository).findById("SELL_ORDER_456");
            verify(transactionRepository).save(any(ITransaction.class));
        }

        @Test
        @DisplayName("Should handle buy order not found")
        void shouldHandleBuyOrderNotFound() {
            // Given
            CreateTransactionCommand command = new CreateTransactionCommand(
                    "NONEXISTENT_BUY",
                    "SELL_ORDER_456",
                    new BigDecimal("44500.00"),
                    Currency.USD,
                    new BigDecimal("0.8")
            );

            // Mock order repository - buy order not found
            when(orderRepository.findById("NONEXISTENT_BUY")).thenReturn(Optional.empty());

            // When
            TransactionResultDTO result = transactionApplicationService.createTransaction(command);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getTransactionId()).isNull();
            assertThat(result.getMessage()).contains("Buy order not found");
            assertThat(result.getErrors()).containsExactly("Buy order not found");

            // Verify no transaction save
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle sell order not found")
        void shouldHandleSellOrderNotFound() {
            // Given
            CreateTransactionCommand command = new CreateTransactionCommand(
                    "BUY_ORDER_123",
                    "NONEXISTENT_SELL",
                    new BigDecimal("44500.00"),
                    Currency.USD,
                    new BigDecimal("0.8")
            );

            // Mock order repository - buy order found, sell order not found
            IBuyOrder buyOrder = new BuyOrder("BUY_ORDER_123", Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), new BigDecimal("1.0"));
            when(orderRepository.findById("BUY_ORDER_123")).thenReturn(Optional.of(buyOrder));
            when(orderRepository.findById("NONEXISTENT_SELL")).thenReturn(Optional.empty());

            // When
            TransactionResultDTO result = transactionApplicationService.createTransaction(command);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getTransactionId()).isNull();
            assertThat(result.getMessage()).contains("Sell order not found");
            assertThat(result.getErrors()).containsExactly("Sell order not found");

            // Verify no transaction save
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle validation failure during transaction creation")
        void shouldHandleValidationFailureDuringTransactionCreation() {
            // Given
            CreateTransactionCommand command = new CreateTransactionCommand(
                    "BUY_ORDER_123",
                    "SELL_ORDER_456",
                    new BigDecimal("46000.00"), // Invalid price above buy price
                    Currency.USD,
                    new BigDecimal("0.8")
            );

            // Mock order repository lookups
            IBuyOrder buyOrder = new BuyOrder("BUY_ORDER_123", Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), new BigDecimal("1.0"));
            ISellOrder sellOrder = new SellOrder("SELL_ORDER_456", Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD), new BigDecimal("0.8"));

            when(orderRepository.findById("BUY_ORDER_123")).thenReturn(Optional.of(buyOrder));
            when(orderRepository.findById("SELL_ORDER_456")).thenReturn(Optional.of(sellOrder));

            // Mock validation failure
            List<ValidationErrorMessage> errors = List.of(
                    new ValidationErrorMessage("Execution price must be between sell price and buy price")
            );
            when(orderValidationService.validateTransactionCreation(any(), any(), any(), any()))
                    .thenReturn(errors);

            // When
            TransactionResultDTO result = transactionApplicationService.createTransaction(command);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getTransactionId()).isNull();
            assertThat(result.getMessage()).contains("Transaction validation failed");
            assertThat(result.getErrors()).containsExactly("Execution price must be between sell price and buy price");

            // Verify no transaction save
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle repository exception during transaction creation")
        void shouldHandleRepositoryExceptionDuringTransactionCreation() {
            // Given
            IBuyOrder buyOrder = new BuyOrder("BUY_ORDER_123", Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), new BigDecimal("1.0"));
            ISellOrder sellOrder = new SellOrder("SELL_ORDER_456", Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD), new BigDecimal("0.8"));

            // Mock validation success
            when(orderValidationService.validateTransactionCreation(any(), any(), any(), any()))
                    .thenReturn(new ArrayList<>());

            // Mock transaction repository exception
            when(transactionRepository.save(any(ITransaction.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // When
            var result = transactionApplicationService.createTransaction(
                    buyOrder, sellOrder, Money.of("44500.00", Currency.USD), new BigDecimal("0.8"));

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getTransactionId()).isNull();
            assertThat(result.getMessage()).contains("Failed to create transaction");
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0)).contains("Database connection failed");
        }
    }

    @Nested
    @DisplayName("🔍 Transaction Query Infrastructure Tests")
    class TransactionQueryInfrastructureTests {

        @Test
        @DisplayName("Should find transaction by ID with DTO mapping")
        void shouldFindTransactionByIdWithDtoMapping() {
            // Given
            String transactionId = "TXN_123";

            IBuyOrder buyOrder = new BuyOrder("BUY_ORDER_123", Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), new BigDecimal("1.0"));
            ISellOrder sellOrder = new SellOrder("SELL_ORDER_456", Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD), new BigDecimal("0.8"));

            ITransaction transaction = new Transaction("TXN_123", Symbol.btcUsd(),
                    buyOrder, sellOrder, Money.of("44500.00", Currency.USD), new BigDecimal("0.8"));

            // Mock repository find
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

            // When
            Optional<TransactionDTO> result = transactionApplicationService.findTransactionByIdAsDTO(transactionId);

            // Then
            assertThat(result).isPresent();
            TransactionDTO transactionDTO = result.get();
            assertThat(transactionDTO.getId()).isEqualTo("TXN_123");
            assertThat(transactionDTO.getSymbolCode()).isEqualTo("BTC");
            assertThat(transactionDTO.getBuyOrderId()).isEqualTo("BUY_ORDER_123");
            assertThat(transactionDTO.getSellOrderId()).isEqualTo("SELL_ORDER_456");
            assertThat(transactionDTO.getPrice()).isEqualTo(new BigDecimal("44500.00"));
            assertThat(transactionDTO.getQuantity()).isEqualTo(new BigDecimal("0.8"));
            assertThat(transactionDTO.getTotalValue()).isEqualTo(new BigDecimal("35600.00"));

            // Verify repository interaction
            verify(transactionRepository).findById(transactionId);
        }

        @Test
        @DisplayName("Should find transactions by order ID")
        void shouldFindTransactionsByOrderId() {
            // Given
            String orderId = "BUY_ORDER_123";

            IBuyOrder buyOrder = new BuyOrder("BUY_ORDER_123", Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), new BigDecimal("1.0"));
            ISellOrder sellOrder = new SellOrder("SELL_ORDER_456", Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD), new BigDecimal("0.8"));

            List<ITransaction> transactions = List.of(
                    new Transaction("TXN_123", Symbol.btcUsd(), buyOrder, sellOrder,
                            Money.of("44500.00", Currency.USD), new BigDecimal("0.5")),
                    new Transaction("TXN_124", Symbol.btcUsd(), buyOrder, sellOrder,
                            Money.of("44500.00", Currency.USD), new BigDecimal("0.3"))
            );

            // Mock repository find
            when(transactionRepository.findByOrderId(orderId)).thenReturn(transactions);

            // When
            List<TransactionDTO> result = transactionApplicationService.findTransactionsByOrderIdAsDTO(orderId);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getBuyOrderId()).isEqualTo("BUY_ORDER_123");
            assertThat(result.get(1).getBuyOrderId()).isEqualTo("BUY_ORDER_123");

            // Verify repository interaction
            verify(transactionRepository).findByOrderId(orderId);
        }

        @Test
        @DisplayName("Should find transactions by symbol with DTO mapping")
        void shouldFindTransactionsBySymbolWithDtoMapping() {
            // Given
            String symbolCode = "ETH";

            IBuyOrder buyOrder = new BuyOrder("BUY_ORDER_123", Symbol.ethUsd(),
                    Money.of("3000.00", Currency.USD), new BigDecimal("2.0"));
            ISellOrder sellOrder = new SellOrder("SELL_ORDER_456", Symbol.ethUsd(),
                    Money.of("2950.00", Currency.USD), new BigDecimal("1.5"));

            List<ITransaction> transactions = List.of(
                    new Transaction("TXN_123", Symbol.ethUsd(), buyOrder, sellOrder,
                            Money.of("2975.00", Currency.USD), new BigDecimal("1.5"))
            );

            // Mock repository find
            when(transactionRepository.findBySymbol(any(Symbol.class))).thenReturn(transactions);

            // When
            List<TransactionDTO> result = transactionApplicationService.findTransactionsBySymbolAsDTO(symbolCode);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSymbolCode()).isEqualTo("ETH");

            // Verify repository interaction
            verify(transactionRepository).findBySymbol(any(Symbol.class));
        }

        @Test
        @DisplayName("Should find transactions by date range")
        void shouldFindTransactionsByDateRange() {
            // Given
            LocalDateTime startDate = LocalDateTime.now().minusDays(1);
            LocalDateTime endDate = LocalDateTime.now();

            IBuyOrder buyOrder = new BuyOrder("BUY_ORDER_123", Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), new BigDecimal("1.0"));
            ISellOrder sellOrder = new SellOrder("SELL_ORDER_456", Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD), new BigDecimal("0.8"));

            List<ITransaction> transactions = List.of(
                    new Transaction("TXN_123", Symbol.btcUsd(), buyOrder, sellOrder,
                            Money.of("44500.00", Currency.USD), new BigDecimal("0.8"))
            );

            // Mock repository find
            when(transactionRepository.findByDateRange(startDate, endDate)).thenReturn(transactions);

            // When
            List<TransactionDTO> result = transactionApplicationService.findTransactionsByDateRangeAsDTO(startDate, endDate);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo("TXN_123");

            // Verify repository interaction
            verify(transactionRepository).findByDateRange(startDate, endDate);
        }
    }

    @Nested
    @DisplayName("📊 Transaction Analytics Infrastructure Tests")
    class TransactionAnalyticsInfrastructureTests {

        @Test
        @DisplayName("Should get transaction statistics with DTO mapping")
        void shouldGetTransactionStatisticsWithDtoMapping() {
            // Given
            String symbolCode = "BTC";

            IBuyOrder buyOrder = new BuyOrder("BUY_ORDER_123", Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), new BigDecimal("1.0"));
            ISellOrder sellOrder = new SellOrder("SELL_ORDER_456", Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD), new BigDecimal("0.8"));

            List<ITransaction> transactions = List.of(
                    new Transaction("TXN_123", Symbol.btcUsd(), buyOrder, sellOrder,
                            Money.of("44500.00", Currency.USD), new BigDecimal("0.8")),
                    new Transaction("TXN_124", Symbol.btcUsd(), buyOrder, sellOrder,
                            Money.of("44800.00", Currency.USD), new BigDecimal("0.5"))
            );

            // Mock repository find
            when(transactionRepository.findBySymbol(any(Symbol.class))).thenReturn(transactions);

            // When
            TransactionStatisticsDTO result = transactionApplicationService.getTransactionStatisticsAsDTO(symbolCode);

            // Then
            assertThat(result.getSymbolCode()).isEqualTo("BTC");
            assertThat(result.getTotalTransactions()).isEqualTo(2);
            assertThat(result.getTotalVolume()).isEqualTo(new BigDecimal("1.3")); // 0.8 + 0.5
            assertThat(result.getCurrency()).isEqualTo(Currency.USD);

            // Verify repository interaction
            verify(transactionRepository).findBySymbol(any(Symbol.class));
        }

        @Test
        @DisplayName("Should handle empty transaction statistics")
        void shouldHandleEmptyTransactionStatistics() {
            // Given
            String symbolCode = "BTC";

            // Mock repository - no transactions
            when(transactionRepository.findBySymbol(any(Symbol.class))).thenReturn(new ArrayList<>());

            // When
            TransactionStatisticsDTO result = transactionApplicationService.getTransactionStatisticsAsDTO(symbolCode);

            // Then
            assertThat(result.getSymbolCode()).isEqualTo("BTC");
            assertThat(result.getTotalTransactions()).isEqualTo(0);
            assertThat(result.getTotalVolume()).isEqualTo(BigDecimal.ZERO);
            assertThat(result.getAveragePrice()).isEqualTo(BigDecimal.ZERO);

            // Verify repository interaction
            verify(transactionRepository).findBySymbol(any(Symbol.class));
        }

        @Test
        @DisplayName("Should calculate transaction volume statistics")
        void shouldCalculateTransactionVolumeStatistics() {
            // Given
            String symbolCode = "ETH";
            LocalDateTime startDate = LocalDateTime.now().minusDays(1);
            LocalDateTime endDate = LocalDateTime.now();

            IBuyOrder buyOrder = new BuyOrder("BUY_ORDER_123", Symbol.ethUsd(),
                    Money.of("3000.00", Currency.USD), new BigDecimal("2.0"));
            ISellOrder sellOrder = new SellOrder("SELL_ORDER_456", Symbol.ethUsd(),
                    Money.of("2950.00", Currency.USD), new BigDecimal("1.5"));

            List<ITransaction> transactions = List.of(
                    new Transaction("TXN_123", Symbol.ethUsd(), buyOrder, sellOrder,
                            Money.of("2975.00", Currency.USD), new BigDecimal("1.5"))
            );

            // Mock repository find
            when(transactionRepository.findByDateRange(startDate, endDate)).thenReturn(transactions);

            // When
            TransactionVolume result = transactionApplicationService.getTransactionVolume(
                    Symbol.ethUsd(), startDate, endDate);

            // Then
            assertThat(result.getSymbol()).isEqualTo(Symbol.ethUsd());
            assertThat(result.getVolume()).isEqualTo(new BigDecimal("1.5"));
            assertThat(result.getPeriodStart()).isEqualTo(startDate);
            assertThat(result.getPeriodEnd()).isEqualTo(endDate);

            // Verify repository interaction
            verify(transactionRepository).findByDateRange(startDate, endDate);
        }
    }

    @Nested
    @DisplayName("🎯 DTO Mapping and Symbol Handling Tests")
    class DtoMappingAndSymbolTests {

        @Test
        @DisplayName("Should handle different symbol mappings correctly")
        void shouldHandleDifferentSymbolMappingsCorrectly() {
            // Given - Test different symbols
            String[] symbolCodes = {"BTC", "ETH", "EURUSD", "GBPUSD"};

            for (String symbolCode : symbolCodes) {
                // Mock repository to return empty list
                when(transactionRepository.findBySymbol(any(Symbol.class))).thenReturn(new ArrayList<>());

                // When
                List<TransactionDTO> result = transactionApplicationService.findTransactionsBySymbolAsDTO(symbolCode);

                // Then
                assertThat(result).isNotNull();

                // Verify correct symbol was created and used
                verify(transactionRepository).findBySymbol(argThat(symbol ->
                        symbol.getCode().equals(symbolCode)));
            }
        }

        @Test
        @DisplayName("Should handle invalid symbol in transaction queries")
        void shouldHandleInvalidSymbolInTransactionQueries() {
            // Given
            String invalidSymbolCode = "INVALID";

            // When & Then
            assertThatThrownBy(() -> transactionApplicationService.findTransactionsBySymbolAsDTO(invalidSymbolCode))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported symbol: INVALID");

            // Verify no repository call
            verify(transactionRepository, never()).findBySymbol(any());
        }

        @Test
        @DisplayName("Should map TransactionResult to DTO correctly")
        void shouldMapTransactionResultToDtoCorrectly() {
            // Given
            CreateTransactionCommand command = new CreateTransactionCommand(
                    "BUY_ORDER_123", "SELL_ORDER_456", new BigDecimal("44500.00"), Currency.USD, new BigDecimal("0.8"));

            // Mock successful creation
            IBuyOrder buyOrder = new BuyOrder("BUY_ORDER_123", Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), new BigDecimal("1.0"));
            ISellOrder sellOrder = new SellOrder("SELL_ORDER_456", Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD), new BigDecimal("0.8"));

            when(orderRepository.findById("BUY_ORDER_123")).thenReturn(Optional.of(buyOrder));
            when(orderRepository.findById("SELL_ORDER_456")).thenReturn(Optional.of(sellOrder));
            when(orderValidationService.validateTransactionCreation(any(), any(), any(), any()))
                    .thenReturn(new ArrayList<>());

            Transaction savedTransaction = new Transaction("TXN_789", Symbol.btcUsd(),
                    buyOrder, sellOrder, Money.of("44500.00", Currency.USD), new BigDecimal("0.8"));
            when(transactionRepository.save(any(ITransaction.class))).thenReturn(savedTransaction);
            when(orderRepository.save(any(IBuyOrder.class))).thenReturn(buyOrder);
            when(orderRepository.save(any(ISellOrder.class))).thenReturn(sellOrder);

            // When
            TransactionResultDTO result = transactionApplicationService.createTransaction(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getTransactionId()).isEqualTo("TXN_789");
            assertThat(result.getMessage()).isNotNull();
            assertThat(result.getTimestamp()).isNotNull();
            assertThat(result.getErrors()).isEmpty();
        }

        @Test
        @DisplayName("Should handle order type validation in transaction creation")
        void shouldHandleOrderTypeValidationInTransactionCreation() {
            // Given - Create sell order but pass it as buy order ID
            CreateTransactionCommand command = new CreateTransactionCommand(
                    "SELL_ORDER_456", // This is actually a sell order
                    "SELL_ORDER_789",
                    new BigDecimal("44500.00"),
                    Currency.USD,
                    new BigDecimal("0.8")
            );

            // Mock repository lookups - both are sell orders
            ISellOrder sellOrder1 = new SellOrder("SELL_ORDER_456", Symbol.btcUsd(),
                    Money.of("44000.00", Currency.USD), new BigDecimal("0.8"));
            ISellOrder sellOrder2 = new SellOrder("SELL_ORDER_789", Symbol.btcUsd(),
                    Money.of("43000.00", Currency.USD), new BigDecimal("0.5"));

            when(orderRepository.findById("SELL_ORDER_456")).thenReturn(Optional.of(sellOrder1));
            when(orderRepository.findById("SELL_ORDER_789")).thenReturn(Optional.of(sellOrder2));

            // When
            TransactionResultDTO result = transactionApplicationService.createTransaction(command);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("Invalid buy order type");
            assertThat(result.getErrors()).containsExactly("Invalid buy order type");
        }
    }
}