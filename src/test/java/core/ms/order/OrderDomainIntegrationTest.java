package core.ms.order;

import core.ms.order.domain.*;
import core.ms.order.domain.value.OrderStatusEnum;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Order Domain Integration Tests")
class OrderDomainIntegrationTest {

    @Test
    @DisplayName("Should handle complete order lifecycle")
    void shouldHandleCompleteOrderLifecycle() {
        // Create orders
        Symbol btcUsd = Symbol.btcUsd();
        Money price = Money.of("45000", Currency.USD);
        BigDecimal quantity = new BigDecimal("1.0");

        IBuyOrder buyOrder = new BuyOrder("buy-1", btcUsd, price, quantity);
        ISellOrder sellOrder = new SellOrder("sell-1", btcUsd, price, quantity);

        // Verify initial state
        assertEquals(OrderStatusEnum.PENDING, buyOrder.getStatus().getStatus());
        assertEquals(OrderStatusEnum.PENDING, sellOrder.getStatus().getStatus());
        assertTrue(buyOrder.isActive());
        assertTrue(sellOrder.isActive());

        // Partial fill
        buyOrder.fillPartial();
        sellOrder.fillPartial();
        assertEquals(OrderStatusEnum.PARTIAL, buyOrder.getStatus().getStatus());
        assertEquals(OrderStatusEnum.PARTIAL, sellOrder.getStatus().getStatus());

        // Complete fill
        buyOrder.complete();
        sellOrder.complete();
        assertEquals(OrderStatusEnum.FILLED, buyOrder.getStatus().getStatus());
        assertEquals(OrderStatusEnum.FILLED, sellOrder.getStatus().getStatus());
        assertFalse(buyOrder.isActive());
        assertFalse(sellOrder.isActive());

        // Create transaction
        Transaction transaction = new Transaction("tx-1", btcUsd, buyOrder, sellOrder, price, quantity);
        assertNotNull(transaction);
        assertEquals(Money.of("45000.00", Currency.USD), transaction.getTotalValue());
    }

    @Test
    @DisplayName("Should handle order cancellation scenario")
    void shouldHandleOrderCancellationScenario() {
        Symbol btcUsd = Symbol.btcUsd();
        Money price = Money.of("45000", Currency.USD);
        BigDecimal quantity = new BigDecimal("1.0");

        BuyOrder buyOrder = new BuyOrder("buy-1", btcUsd, price, quantity);

        // Update price
        Money newPrice = Money.of("46000", Currency.USD);
        buyOrder.updatePrice(newPrice);
        assertEquals(newPrice, buyOrder.getPrice());

        // Partial cancellation
        buyOrder.cancelPartial();
        assertEquals(OrderStatusEnum.PENDING, buyOrder.getStatus().getStatus());

        // Full cancellation
        buyOrder.cancel();
        assertEquals(OrderStatusEnum.CANCELLED, buyOrder.getStatus().getStatus());
        assertFalse(buyOrder.isActive());

        // Should not allow further modifications
        assertThrows(IllegalStateException.class, () -> buyOrder.updatePrice(price));
    }
}