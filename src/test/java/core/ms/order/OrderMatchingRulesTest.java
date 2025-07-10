package core.ms.order;

import core.ms.order.domain.*;
import core.ms.order.domain.value.OrderStatusEnum;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("4. Order Matching Rules Tests")
class OrderMatchingRulesTest {

    private Symbol btcUsd;

    @BeforeEach
    void setUp() {
        btcUsd = Symbol.btcUsd();
    }

    @Test
    @DisplayName("Should allow matching when buy price is greater than or equal to sell price")
    void shouldAllowMatchingWhenBuyPriceIsGreaterThanOrEqualToSellPrice() {
        // Given: Buy at $45,000, Sell at $44,000
        Money buyPrice = Money.of("45000", Currency.USD);
        Money sellPrice = Money.of("44000", Currency.USD);
        Money executionPrice = Money.of("44500", Currency.USD); // Mid-point
        BigDecimal quantity = new BigDecimal("1.0");

        IBuyOrder buyOrder = new BuyOrder("buy-1", btcUsd, buyPrice, quantity);
        ISellOrder sellOrder = new SellOrder("sell-1", btcUsd, sellPrice, quantity);

        // When: Creating transaction with execution price
        // Then: Should succeed
        assertDoesNotThrow(() ->
                new Transaction("tx-1", btcUsd, buyOrder, sellOrder, executionPrice, quantity));
    }

    @Test
    @DisplayName("Should prevent matching when buy price is less than sell price")
    void shouldPreventMatchingWhenBuyPriceIsLessThanSellPrice() {
        // Given: Buy at $44,000, Sell at $45,000 (no overlap)
        Money buyPrice = Money.of("44000", Currency.USD);
        Money sellPrice = Money.of("45000", Currency.USD);
        Money executionPrice = Money.of("44500", Currency.USD);
        BigDecimal quantity = new BigDecimal("1.0");

        IBuyOrder buyOrder = new BuyOrder("buy-1", btcUsd, buyPrice, quantity);
        ISellOrder sellOrder = new SellOrder("sell-1", btcUsd, sellPrice, quantity);

        // When: Trying to create transaction
        // Then: Should fail - prices don't overlap
        assertThrows(IllegalArgumentException.class,
                () -> new Transaction("tx-1", btcUsd, buyOrder, sellOrder, executionPrice, quantity));
    }

    @Test
    @DisplayName("Should validate execution price is within buy-sell price range")
    void shouldValidateExecutionPriceIsWithinBuySellPriceRange() {
        // Given: Buy at $46,000, Sell at $44,000
        Money buyPrice = Money.of("46000", Currency.USD);
        Money sellPrice = Money.of("44000", Currency.USD);
        BigDecimal quantity = new BigDecimal("1.0");

        // Valid execution prices (between sell and buy)
        Money validPrice1 = Money.of("44000", Currency.USD); // Sell price
        Money validPrice2 = Money.of("45000", Currency.USD); // Mid-point
        Money validPrice3 = Money.of("46000", Currency.USD); // Buy price

        // Invalid execution prices (outside range)
        Money tooLow = Money.of("43000", Currency.USD);   // Below sell price
        Money tooHigh = Money.of("47000", Currency.USD);  // Above buy price

        // ===== FIX: CREATE FRESH ORDERS FOR EACH TRANSACTION =====

        // Test 1: Valid price at sell price
        IBuyOrder buyOrder1 = new BuyOrder("buy-1", btcUsd, buyPrice, quantity);
        ISellOrder sellOrder1 = new SellOrder("sell-1", btcUsd, sellPrice, quantity);
        assertDoesNotThrow(() -> new Transaction("tx-1", btcUsd, buyOrder1, sellOrder1, validPrice1, quantity));

        // Test 2: Valid price at mid-point
        IBuyOrder buyOrder2 = new BuyOrder("buy-2", btcUsd, buyPrice, quantity);
        ISellOrder sellOrder2 = new SellOrder("sell-2", btcUsd, sellPrice, quantity);
        assertDoesNotThrow(() -> new Transaction("tx-2", btcUsd, buyOrder2, sellOrder2, validPrice2, quantity));

        // Test 3: Valid price at buy price
        IBuyOrder buyOrder3 = new BuyOrder("buy-3", btcUsd, buyPrice, quantity);
        ISellOrder sellOrder3 = new SellOrder("sell-3", btcUsd, sellPrice, quantity);
        assertDoesNotThrow(() -> new Transaction("tx-3", btcUsd, buyOrder3, sellOrder3, validPrice3, quantity));

        // Test 4: Invalid price too low
        IBuyOrder buyOrder4 = new BuyOrder("buy-4", btcUsd, buyPrice, quantity);
        ISellOrder sellOrder4 = new SellOrder("sell-4", btcUsd, sellPrice, quantity);
        assertThrows(IllegalArgumentException.class,
                () -> new Transaction("tx-4", btcUsd, buyOrder4, sellOrder4, tooLow, quantity));

        // Test 5: Invalid price too high
        IBuyOrder buyOrder5 = new BuyOrder("buy-5", btcUsd, buyPrice, quantity);
        ISellOrder sellOrder5 = new SellOrder("sell-5", btcUsd, sellPrice, quantity);
        assertThrows(IllegalArgumentException.class,
                () -> new Transaction("tx-5", btcUsd, buyOrder5, sellOrder5, tooHigh, quantity));
    }
    @Test
    @DisplayName("Should validate execution price is within buy-sell price range - Multiple Transactions")
    void shouldValidateExecutionPriceIsWithinBuySellPriceRangeMultipleTransactions() {
        // Given: Buy at $46,000, Sell at $44,000 with LARGER quantities
        Money buyPrice = Money.of("46000", Currency.USD);
        Money sellPrice = Money.of("44000", Currency.USD);
        BigDecimal orderQuantity = new BigDecimal("10.0"); // Large enough for multiple transactions
        BigDecimal transactionQuantity = new BigDecimal("1.0"); // Small transaction size

        IBuyOrder buyOrder = new BuyOrder("buy-1", btcUsd, buyPrice, orderQuantity);
        ISellOrder sellOrder = new SellOrder("sell-1", btcUsd, sellPrice, orderQuantity);

        // Valid execution prices (between sell and buy)
        Money validPrice1 = Money.of("44000", Currency.USD); // Sell price
        Money validPrice2 = Money.of("45000", Currency.USD); // Mid-point
        Money validPrice3 = Money.of("46000", Currency.USD); // Buy price

        // Should succeed for valid prices
        assertDoesNotThrow(() -> new Transaction("tx-1", btcUsd, buyOrder, sellOrder, validPrice1, transactionQuantity));
        assertDoesNotThrow(() -> new Transaction("tx-2", btcUsd, buyOrder, sellOrder, validPrice2, transactionQuantity));
        assertDoesNotThrow(() -> new Transaction("tx-3", btcUsd, buyOrder, sellOrder, validPrice3, transactionQuantity));

        // Orders should still be PARTIAL (not FILLED) since we have remaining quantity
        assertEquals(OrderStatusEnum.PARTIAL, buyOrder.getStatus().getStatus());
        assertEquals(OrderStatusEnum.PARTIAL, sellOrder.getStatus().getStatus());

        // Verify executed and remaining quantities
        assertEquals(new BigDecimal("3.0"), buyOrder.getExecutedQuantity());
        assertEquals(new BigDecimal("7.0"), buyOrder.getRemainingQuantity());
    }

    @Test
    @DisplayName("Should determine optimal execution price for matching orders")
    void shouldDetermineOptimalExecutionPriceForMatchingOrders() {
        // Given: Buy at $46,000, Sell at $44,000
        Money buyPrice = Money.of("46000", Currency.USD);
        Money sellPrice = Money.of("44000", Currency.USD);
        BigDecimal quantity = new BigDecimal("1.0");

        IBuyOrder buyOrder = new BuyOrder("buy-1", btcUsd, buyPrice, quantity);
        ISellOrder sellOrder = new SellOrder("sell-1", btcUsd, sellPrice, quantity);

        // When: Determining optimal execution price
        Money optimalPrice = Transaction.determineExecutionPrice(buyOrder, sellOrder);

        // Then: Should be mid-point (or some fair price discovery algorithm)
        Money expectedPrice = Money.of("45000", Currency.USD); // Mid-point
        assertEquals(expectedPrice, optimalPrice);
    }

}
