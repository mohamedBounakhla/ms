package core.ms.order;


import core.ms.order.domain.entities.*;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.order.infrastructure.persistence.dao.BuyOrderDAO;
import core.ms.order.infrastructure.persistence.dao.SellOrderDAO;
import core.ms.order.infrastructure.persistence.entities.BuyOrderEntity;
import core.ms.order.infrastructure.persistence.entities.SellOrderEntity;

import core.ms.order.infrastructure.persistence.mappers.BuyOrderMapper;
import core.ms.order.infrastructure.persistence.mappers.SellOrderMapper;

import core.ms.order.infrastructure.services.OrderRepositoryService;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Repository Infrastructure Tests")
class RepositoryInfrastructureTest {

    @Nested
    @DisplayName("🏪 OrderRepositoryService Tests")
    class OrderRepositoryServiceTests {

        @Mock
        private BuyOrderDAO buyOrderDAO;

        @Mock
        private SellOrderDAO sellOrderDAO;

        @Mock
        private BuyOrderMapper buyOrderMapper;

        @Mock
        private SellOrderMapper sellOrderMapper;

        @InjectMocks
        private OrderRepositoryService orderRepositoryService;

        @Test
        @DisplayName("Should save buy order with entity mapping")
        void shouldSaveBuyOrderWithEntityMapping() {
            // Given
            BuyOrder buyOrder = new BuyOrder("BUY_ORDER_123", Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), new BigDecimal("0.1"));

            BuyOrderEntity buyOrderEntity = new BuyOrderEntity("BUY_ORDER_123", "BTC", "Bitcoin",
                    new BigDecimal("45000.00"), Currency.USD, new BigDecimal("0.1"),
                    OrderStatusEnum.PENDING, BigDecimal.ZERO, LocalDateTime.now(), LocalDateTime.now());

            BuyOrderEntity savedEntity = new BuyOrderEntity("BUY_ORDER_123", "BTC", "Bitcoin",
                    new BigDecimal("45000.00"), Currency.USD, new BigDecimal("0.1"),
                    OrderStatusEnum.PENDING, BigDecimal.ZERO, LocalDateTime.now(), LocalDateTime.now());

            // Mock mapper and DAO
            when(buyOrderMapper.fromDomain(buyOrder)).thenReturn(buyOrderEntity);
            when(buyOrderDAO.save(buyOrderEntity)).thenReturn(savedEntity);
            when(buyOrderMapper.toDomain(savedEntity)).thenReturn(buyOrder);

            // When
            IOrder result = orderRepositoryService.save(buyOrder);

            // Then
            assertThat(result).isEqualTo(buyOrder);

            // Verify interactions
            verify(buyOrderMapper).fromDomain(buyOrder);
            verify(buyOrderDAO).save(buyOrderEntity);
            verify(buyOrderMapper).toDomain(savedEntity);
        }

        @Test
        @DisplayName("Should save sell order with entity mapping")
        void shouldSaveSellOrderWithEntityMapping() {
            // Given
            SellOrder sellOrder = new SellOrder("SELL_ORDER_456", Symbol.ethUsd(),
                    Money.of("3000.00", Currency.USD), new BigDecimal("1.5"));

            SellOrderEntity sellOrderEntity = new SellOrderEntity("SELL_ORDER_456", "ETH", "Ethereum",
                    new BigDecimal("3000.00"), Currency.USD, new BigDecimal("1.5"),
                    OrderStatusEnum.PENDING, BigDecimal.ZERO, LocalDateTime.now(), LocalDateTime.now());

            SellOrderEntity savedEntity = new SellOrderEntity("SELL_ORDER_456", "ETH", "Ethereum",
                    new BigDecimal("3000.00"), Currency.USD, new BigDecimal("1.5"),
                    OrderStatusEnum.PENDING, BigDecimal.ZERO, LocalDateTime.now(), LocalDateTime.now());

            // Mock mapper and DAO
            when(sellOrderMapper.fromDomain(sellOrder)).thenReturn(sellOrderEntity);
            when(sellOrderDAO.save(sellOrderEntity)).thenReturn(savedEntity);
            when(sellOrderMapper.toDomain(savedEntity)).thenReturn(sellOrder);

            // When
            IOrder result = orderRepositoryService.save(sellOrder);

            // Then
            assertThat(result).isEqualTo(sellOrder);

            // Verify interactions
            verify(sellOrderMapper).fromDomain(sellOrder);
            verify(sellOrderDAO).save(sellOrderEntity);
            verify(sellOrderMapper).toDomain(savedEntity);
        }

        @Test
        @DisplayName("Should find order by ID checking both DAOs")
        void shouldFindOrderByIdCheckingBothDaos() {
            // Given
            String orderId = "ORDER_123";
            BuyOrder buyOrder = new BuyOrder("ORDER_123", Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), new BigDecimal("0.1"));

            BuyOrderEntity buyOrderEntity = new BuyOrderEntity("ORDER_123", "BTC", "Bitcoin",
                    new BigDecimal("45000.00"), Currency.USD, new BigDecimal("0.1"),
                    OrderStatusEnum.PENDING, BigDecimal.ZERO, LocalDateTime.now(), LocalDateTime.now());

            // Mock buy order found
            when(buyOrderDAO.findById(orderId)).thenReturn(Optional.of(buyOrderEntity));
            when(buyOrderMapper.toDomain(buyOrderEntity)).thenReturn(buyOrder);

            // When
            Optional<IOrder> result = orderRepositoryService.findById(orderId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(buyOrder);

            // Verify interactions
            verify(buyOrderDAO).findById(orderId);
            verify(buyOrderMapper).toDomain(buyOrderEntity);
            // Sell order DAO should not be called since buy order was found
            verify(sellOrderDAO, never()).findById(orderId);
        }

        @Test
        @DisplayName("Should find sell order when buy order not found")
        void shouldFindSellOrderWhenBuyOrderNotFound() {
            // Given
            String orderId = "ORDER_456";
            SellOrder sellOrder = new SellOrder("ORDER_456", Symbol.ethUsd(),
                    Money.of("3000.00", Currency.USD), new BigDecimal("1.5"));

            SellOrderEntity sellOrderEntity = new SellOrderEntity("ORDER_456", "ETH", "Ethereum",
                    new BigDecimal("3000.00"), Currency.USD, new BigDecimal("1.5"),
                    OrderStatusEnum.PENDING, BigDecimal.ZERO, LocalDateTime.now(), LocalDateTime.now());

            // Mock buy order not found, sell order found
            when(buyOrderDAO.findById(orderId)).thenReturn(Optional.empty());
            when(sellOrderDAO.findById(orderId)).thenReturn(Optional.of(sellOrderEntity));
            when(sellOrderMapper.toDomain(sellOrderEntity)).thenReturn(sellOrder);

            // When
            Optional<IOrder> result = orderRepositoryService.findById(orderId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(sellOrder);

            // Verify interactions
            verify(buyOrderDAO).findById(orderId);
            verify(sellOrderDAO).findById(orderId);
            verify(sellOrderMapper).toDomain(sellOrderEntity);
        }

        @Test
        @DisplayName("Should return empty when order not found in either DAO")
        void shouldReturnEmptyWhenOrderNotFoundInEitherDao() {
            // Given
            String orderId = "NONEXISTENT_ORDER";

            when(buyOrderDAO.findById(orderId)).thenReturn(Optional.empty());
            when(sellOrderDAO.findById(orderId)).thenReturn(Optional.empty());

            // When
            Optional<IOrder> result = orderRepositoryService.findById(orderId);

            // Then
            assertThat(result).isEmpty();

            // Verify interactions
            verify(buyOrderDAO).findById(orderId);
            verify(sellOrderDAO).findById(orderId);
            verifyNoInteractions(buyOrderMapper, sellOrderMapper);
        }

        @Test
        @DisplayName("Should throw exception for unknown order type")
        void shouldThrowExceptionForUnknownOrderType() {
            // Given
            IOrder unknownOrder = mock(IOrder.class);

            // When & Then
            assertThatThrownBy(() -> orderRepositoryService.save(unknownOrder))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown order type");
        }

        @Test
        @DisplayName("Should delete from both DAOs")
        void shouldDeleteFromBothDaos() {
            // Given
            String orderId = "ORDER_TO_DELETE";

            // When
            orderRepositoryService.deleteById(orderId);

            // Then
            verify(buyOrderDAO).deleteById(orderId);
            verify(sellOrderDAO).deleteById(orderId);
        }

        @Test
        @DisplayName("Should check existence in both DAOs")
        void shouldCheckExistenceInBothDaos() {
            // Given
            String orderId = "ORDER_EXISTS";

            when(buyOrderDAO.existsById(orderId)).thenReturn(true);
            when(sellOrderDAO.existsById(orderId)).thenReturn(false);

            // When
            boolean exists = orderRepositoryService.existsById(orderId);

            // Then
            assertThat(exists).isTrue();
            verify(buyOrderDAO).existsById(orderId);
            verify(sellOrderDAO).existsById(orderId);
        }

        @Test
        @DisplayName("Should find orders by symbol from both DAOs")
        void shouldFindOrdersBySymbolFromBothDaos() {
            // Given
            Symbol symbol = Symbol.btcUsd();

            BuyOrder buyOrder = new BuyOrder("BUY_123", symbol,
                    Money.of("45000.00", Currency.USD), new BigDecimal("0.1"));
            SellOrder sellOrder = new SellOrder("SELL_456", symbol,
                    Money.of("45500.00", Currency.USD), new BigDecimal("0.2"));

            BuyOrderEntity buyEntity = new BuyOrderEntity();
            SellOrderEntity sellEntity = new SellOrderEntity();

            when(buyOrderDAO.findBySymbolCode("BTC")).thenReturn(List.of(buyEntity));
            when(sellOrderDAO.findBySymbolCode("BTC")).thenReturn(List.of(sellEntity));
            when(buyOrderMapper.toDomain(buyEntity)).thenReturn(buyOrder);
            when(sellOrderMapper.toDomain(sellEntity)).thenReturn(sellOrder);

            // When
            List<IOrder> result = orderRepositoryService.findBySymbol(symbol);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(buyOrder, sellOrder);

            verify(buyOrderDAO).findBySymbolCode("BTC");
            verify(sellOrderDAO).findBySymbolCode("BTC");
        }

        @Test
        @DisplayName("Should find orders by status from both DAOs")
        void shouldFindOrdersByStatusFromBothDaos() {
            // Given
            OrderStatusEnum status = OrderStatusEnum.PENDING;

            BuyOrder buyOrder = new BuyOrder("BUY_123", Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), new BigDecimal("0.1"));
            SellOrder sellOrder = new SellOrder("SELL_456", Symbol.ethUsd(),
                    Money.of("3000.00", Currency.USD), new BigDecimal("1.5"));

            BuyOrderEntity buyEntity = new BuyOrderEntity();
            SellOrderEntity sellEntity = new SellOrderEntity();

            when(buyOrderDAO.findByStatus(status)).thenReturn(List.of(buyEntity));
            when(sellOrderDAO.findByStatus(status)).thenReturn(List.of(sellEntity));
            when(buyOrderMapper.toDomain(buyEntity)).thenReturn(buyOrder);
            when(sellOrderMapper.toDomain(sellEntity)).thenReturn(sellOrder);

            // When
            List<IOrder> result = orderRepositoryService.findByStatus(status);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(buyOrder, sellOrder);

            verify(buyOrderDAO).findByStatus(status);
            verify(sellOrderDAO).findByStatus(status);
        }

        @Test
        @DisplayName("Should find buy orders by symbol")
        void shouldFindBuyOrdersBySymbol() {
            // Given
            Symbol symbol = Symbol.btcUsd();
            BuyOrder buyOrder = new BuyOrder("BUY_123", symbol,
                    Money.of("45000.00", Currency.USD), new BigDecimal("0.1"));

            BuyOrderEntity buyEntity = new BuyOrderEntity();

            when(buyOrderDAO.findBySymbolCode("BTC")).thenReturn(List.of(buyEntity));
            when(buyOrderMapper.toDomain(buyEntity)).thenReturn(buyOrder);

            // When
            List<IBuyOrder> result = orderRepositoryService.findBuyOrdersBySymbol(symbol);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(buyOrder);

            verify(buyOrderDAO).findBySymbolCode("BTC");
            verify(buyOrderMapper).toDomain(buyEntity);
        }

        @Test
        @DisplayName("Should find sell orders by symbol")
        void shouldFindSellOrdersBySymbol() {
            // Given
            Symbol symbol = Symbol.ethUsd();
            SellOrder sellOrder = new SellOrder("SELL_456", symbol,
                    Money.of("3000.00", Currency.USD), new BigDecimal("1.5"));

            SellOrderEntity sellEntity = new SellOrderEntity();

            when(sellOrderDAO.findBySymbolCode("ETH")).thenReturn(List.of(sellEntity));
            when(sellOrderMapper.toDomain(sellEntity)).thenReturn(sellOrder);

            // When
            List<ISellOrder> result = orderRepositoryService.findSellOrdersBySymbol(symbol);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(sellOrder);

            verify(sellOrderDAO).findBySymbolCode("ETH");
            verify(sellOrderMapper).toDomain(sellEntity);
        }

        @Test
        @DisplayName("Should find all orders from both DAOs")
        void shouldFindAllOrdersFromBothDaos() {
            // Given
            BuyOrder buyOrder = new BuyOrder("BUY_123", Symbol.btcUsd(),
                    Money.of("45000.00", Currency.USD), new BigDecimal("0.1"));
            SellOrder sellOrder = new SellOrder("SELL_456", Symbol.ethUsd(),
                    Money.of("3000.00", Currency.USD), new BigDecimal("1.5"));

            BuyOrderEntity buyEntity = new BuyOrderEntity();
            SellOrderEntity sellEntity = new SellOrderEntity();

            when(buyOrderDAO.findAll()).thenReturn(List.of(buyEntity));
            when(sellOrderDAO.findAll()).thenReturn(List.of(sellEntity));
            when(buyOrderMapper.toDomain(buyEntity)).thenReturn(buyOrder);
            when(sellOrderMapper.toDomain(sellEntity)).thenReturn(sellOrder);

            // When
            List<IOrder> result = orderRepositoryService.findAll();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(buyOrder, sellOrder);

            verify(buyOrderDAO).findAll();
            verify(sellOrderDAO).findAll();
        }

        @Test
        @DisplayName("Should count orders from both DAOs")
        void shouldCountOrdersFromBothDaos() {
            // Given
            when(buyOrderDAO.count()).thenReturn(5L);
            when(sellOrderDAO.count()).thenReturn(3L);

            // When
            long count = orderRepositoryService.count();

            // Then
            assertThat(count).isEqualTo(8L);

            verify(buyOrderDAO).count();
            verify(sellOrderDAO).count();
        }

        @Test
        @DisplayName("Should return empty list for findByUserId")
        void shouldReturnEmptyListForFindByUserId() {
            // Given & When
            List<IOrder> result = orderRepositoryService.findByUserId("USER_123");

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(buyOrderDAO, sellOrderDAO);
        }
    }
}