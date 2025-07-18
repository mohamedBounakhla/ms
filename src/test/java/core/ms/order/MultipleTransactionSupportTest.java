package core.ms.order;

import core.ms.order.domain.entities.*;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("3. Multiple Transaction Support Tests")
class MultipleTransactionSupportTest {

    private Symbol btcUsd;
    private Money price;

    @BeforeEach
    void setUp() {
        btcUsd = Symbol.btcUsd();
        price = Money.of("45000", Currency.USD);
    }

    @Test
    @DisplayName("Should support multiple partial transactions on same orders")
    void shouldSupportMultiplePartialTransactionsOnSameOrders() {
        // Given: Orders with 5.0 BTC
        BigDecimal orderQuantity = new BigDecimal("5.0");

        IBuyOrder buyOrder = new BuyOrder("buy-1", btcUsd, price, orderQuantity);
        ISellOrder sellOrder = new SellOrder("sell-1", btcUsd, price, orderQuantity);

        // When: Multiple partial transactions
        Transaction tx1 = new Transaction("tx-1", btcUsd, buyOrder, sellOrder, price, new BigDecimal("1.0"));
        assertEquals(OrderStatusEnum.PARTIAL, buyOrder.getStatus().getStatus());

        Transaction tx2 = new Transaction("tx-2", btcUsd, buyOrder, sellOrder, price, new BigDecimal("2.0"));
        assertEquals(OrderStatusEnum.PARTIAL, buyOrder.getStatus().getStatus());

        Transaction tx3 = new Transaction("tx-3", btcUsd, buyOrder, sellOrder, price, new BigDecimal("2.0"));

        // Then: Final transaction should complete the orders
        assertEquals(OrderStatusEnum.FILLED, buyOrder.getStatus().getStatus());
        assertEquals(OrderStatusEnum.FILLED, sellOrder.getStatus().getStatus());
        assertEquals(orderQuantity, buyOrder.getExecutedQuantity());
    }

    @Test
    @DisplayName("Should track all transactions associated with an order")
    void shouldTrackAllTransactionsAssociatedWithAnOrder() {
        // Given: Order with 3.0 BTC
        BigDecimal orderQuantity = new BigDecimal("3.0");

        IBuyOrder buyOrder = new BuyOrder("buy-1", btcUsd, price, orderQuantity);
        ISellOrder sellOrder = new SellOrder("sell-1", btcUsd, price, orderQuantity);

        // When: Multiple transactions
        Transaction tx1 = new Transaction("tx-1", btcUsd, buyOrder, sellOrder, price, new BigDecimal("1.0"));
        Transaction tx2 = new Transaction("tx-2", btcUsd, buyOrder, sellOrder, price, new BigDecimal("1.5"));
        Transaction tx3 = new Transaction("tx-3", btcUsd, buyOrder, sellOrder, price, new BigDecimal("0.5"));

        // Then: Order should track all its transactions
        List<ITransaction> buyOrderTransactions = buyOrder.getTransactions();
        List<ITransaction> sellOrderTransactions = sellOrder.getTransactions();

        assertEquals(3, buyOrderTransactions.size());
        assertEquals(3, sellOrderTransactions.size());
        assertTrue(buyOrderTransactions.contains(tx1));
        assertTrue(buyOrderTransactions.contains(tx2));
        assertTrue(buyOrderTransactions.contains(tx3));
    }

    @Test
    @DisplayName("Should prevent transaction when remaining quantity is insufficient")
    void shouldPreventTransactionWhenRemainingQuantityIsInsufficient() {
        // Given: Order with 2.0 BTC, partially executed
        BigDecimal orderQuantity = new BigDecimal("2.0");

        IBuyOrder buyOrder = new BuyOrder("buy-1", btcUsd, price, orderQuantity);
        ISellOrder sellOrder = new SellOrder("sell-1", btcUsd, price, orderQuantity);

        // Execute 1.5 BTC first
        Transaction tx1 = new Transaction("tx-1", btcUsd, buyOrder, sellOrder, price, new BigDecimal("1.5"));

        // When: Trying to execute more than remaining (0.5 BTC)
        BigDecimal excessiveQuantity = new BigDecimal("1.0");

        // Then: Should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> new Transaction("tx-2", btcUsd, buyOrder, sellOrder, price, excessiveQuantity));
    }
    @Test
    @DisplayName("Transaction processing test")
    void transactionProcessing(){

        ISellOrder mySellOrder = new SellOrder(
                "sell-1",
                Symbol.eurUsd() ,
                Money.of("50.00",Currency.USD),
                new BigDecimal("12")
        );

        IBuyOrder myBuyOrder = new BuyOrder(
                "buy-1",
                Symbol.eurUsd(),
                Money.of("50.00",Currency.USD),
                new BigDecimal("3")
        );

        Transaction t1 = new Transaction(
                "t1",
                mySellOrder.getSymbol(),
                myBuyOrder,
                mySellOrder,
                myBuyOrder.getPrice(),
                myBuyOrder.getQuantity()
        );
        //Quantité initial
        assertEquals(new BigDecimal("12"),mySellOrder.getQuantity());
        //Quantité restante
        assertEquals(new BigDecimal("9"),mySellOrder.getRemainingQuantity());
        //Ordre d'achat vide
        assertSame(BigDecimal.ZERO, myBuyOrder.getRemainingQuantity());
        //Status ordre d'achat FILLED
        assertEquals(OrderStatusEnum.FILLED, myBuyOrder.getStatus().getStatus());

    }
    @Test
    @DisplayName("Transaction processing test - Buy order larger quantity")
    void transactionProcessingBuyOrderLargerQuantity(){

        ISellOrder mySellOrder = new SellOrder(
                "sell-1",
                Symbol.eurUsd(),
                Money.of("50.00", Currency.USD),
                new BigDecimal("3")
        );

        IBuyOrder myBuyOrder = new BuyOrder(
                "buy-1",
                Symbol.eurUsd(),
                Money.of("50.00", Currency.USD),
                new BigDecimal("12")
        );

        Transaction t1 = new Transaction(
                "t1",
                mySellOrder.getSymbol(),
                myBuyOrder,
                mySellOrder,
                myBuyOrder.getPrice(),
                mySellOrder.getQuantity()  // Transaction quantity is limited by the smaller sell order
        );

        // Sell order initial quantity
        assertEquals(new BigDecimal("3"), mySellOrder.getQuantity());
        // Sell order remaining quantity (should be zero - fully filled)
        assertEquals(BigDecimal.ZERO, mySellOrder.getRemainingQuantity());
        // Sell order status should be FILLED
        assertEquals(OrderStatusEnum.FILLED, mySellOrder.getStatus().getStatus());

        // Buy order initial quantity
        assertEquals(new BigDecimal("12"), myBuyOrder.getQuantity());
        // Buy order remaining quantity (12 - 3 = 9)
        assertEquals(new BigDecimal("9"), myBuyOrder.getRemainingQuantity());
        // Buy order status should be PARTIAL (not fully filled)
        assertEquals(OrderStatusEnum.PARTIAL, myBuyOrder.getStatus().getStatus());
    }
}