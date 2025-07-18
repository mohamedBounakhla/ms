package core.ms.order;

import core.ms.order.domain.entities.BuyOrder;
import core.ms.shared.domain.*;
import core.ms.order.domain.value_objects.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Abstract Order Tests")
class AbstractOrderTest {

    private Symbol btcUsd;
    private Money price;
    private BigDecimal quantity;
    private BuyOrder buyOrder;

    @BeforeEach
    void setUp() {
        btcUsd = Symbol.btcUsd();
        price = Money.of("45000", Currency.USD);
        quantity = new BigDecimal("0.5");
        buyOrder = new BuyOrder("order-1", btcUsd, price, quantity);
    }

    @Test
    @DisplayName("Should create order with valid parameters")
    void shouldCreateOrderWithValidParameters() {
        assertNotNull(buyOrder);
        assertEquals("order-1", buyOrder.getId());
        assertEquals(btcUsd, buyOrder.getSymbol());
        assertEquals(price, buyOrder.getPrice());
        assertEquals(quantity, buyOrder.getQuantity());
        assertEquals(OrderStatusEnum.PENDING, buyOrder.getStatus().getStatus());
        assertNotNull(buyOrder.getCreatedAt());
        assertNotNull(buyOrder.getUpdatedAt());
        assertTrue(buyOrder.isActive());
    }

    @Test
    @DisplayName("Should validate price currency matches symbol quote currency")
    void shouldValidatePriceCurrencyMatchesSymbolQuoteCurrency() {
        Money eurPrice = Money.of("40000", Currency.EUR);

        assertThrows(IllegalArgumentException.class,
                () -> new BuyOrder("order-2", btcUsd, eurPrice, quantity));
    }

    @Test
    @DisplayName("Should validate quantity is positive")
    void shouldValidateQuantityIsPositive() {
        BigDecimal negativeQuantity = new BigDecimal("-0.5");
        BigDecimal zeroQuantity = BigDecimal.ZERO;

        assertThrows(IllegalArgumentException.class,
                () -> new BuyOrder("order-2", btcUsd, price, negativeQuantity));
        assertThrows(IllegalArgumentException.class,
                () -> new BuyOrder("order-3", btcUsd, price, zeroQuantity));
    }

    @Test
    @DisplayName("Should calculate total value correctly")
    void shouldCalculateTotalValueCorrectly() {
        Money expectedTotal = Money.of("22500.00", Currency.USD); // 45000 * 0.5
        assertEquals(expectedTotal, buyOrder.getTotalValue());
    }

    @Test
    @DisplayName("Should update price when order is active")
    void shouldUpdatePriceWhenOrderIsActive() {
        Money newPrice = Money.of("46000", Currency.USD);
        LocalDateTime beforeUpdate = buyOrder.getUpdatedAt();

        buyOrder.updatePrice(newPrice);

        assertEquals(newPrice, buyOrder.getPrice());
        assertTrue(buyOrder.getUpdatedAt().isAfter(beforeUpdate));
    }

    @Test
    @DisplayName("Should not update price when order is terminal")
    void shouldNotUpdatePriceWhenOrderIsTerminal() {
        buyOrder.complete(); // Make order terminal
        Money newPrice = Money.of("46000", Currency.USD);

        assertThrows(IllegalStateException.class,
                () -> buyOrder.updatePrice(newPrice));
    }

    @Test
    @DisplayName("Should validate price currency when updating")
    void shouldValidatePriceCurrencyWhenUpdating() {
        Money eurPrice = Money.of("40000", Currency.EUR);

        assertThrows(IllegalArgumentException.class,
                () -> buyOrder.updatePrice(eurPrice));
    }

    @Test
    @DisplayName("Should handle state transitions correctly")
    void shouldHandleStateTransitionsCorrectly() {
        // Test PENDING → PARTIAL
        buyOrder.fillPartial();
        assertEquals(OrderStatusEnum.PARTIAL, buyOrder.getStatus().getStatus());
        assertTrue(buyOrder.isActive());

        // Test PARTIAL → FILLED
        buyOrder.complete();
        assertEquals(OrderStatusEnum.FILLED, buyOrder.getStatus().getStatus());
        assertFalse(buyOrder.isActive());
    }

    @Test
    @DisplayName("Should handle cancellation correctly")
    void shouldHandleCancellationCorrectly() {
        buyOrder.cancel();
        assertEquals(OrderStatusEnum.CANCELLED, buyOrder.getStatus().getStatus());
        assertFalse(buyOrder.isActive());
    }

    @Test
    @DisplayName("Should handle partial cancellation correctly")
    void shouldHandlePartialCancellationCorrectly() {
        assertDoesNotThrow(() -> buyOrder.cancelPartial());
        assertEquals(OrderStatusEnum.PENDING, buyOrder.getStatus().getStatus());
    }
}
