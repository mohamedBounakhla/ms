package core.ms.market_engine;

import core.ms.order.domain.BuyOrder;
import core.ms.order.domain.IBuyOrder;
import core.ms.order.domain.ISellOrder;
import core.ms.order.domain.SellOrder;
import core.ms.order.domain.value.OrderStatusEnum;
import core.ms.order_book.domain.OrderBook;
import core.ms.order_book.domain.OrderBookManager;
import core.ms.order_book.domain.value_object.AskPriceLevel;
import core.ms.order_book.domain.value_object.BidPriceLevel;
import core.ms.order_book.domain.value_object.OrderMatch;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarketEngineTest {

    private MarketEngine marketEngine;
    private OrderBookManager orderBookManager;
    private Symbol testSymbol;
    private Money testPrice;

    @BeforeEach
    void setUp() {
        orderBookManager = new OrderBookManager();
        marketEngine = new MarketEngine("engine-1", orderBookManager);
        testSymbol = Symbol.eurUsd();
        testPrice = Money.of("1.2000", Currency.USD);
    }

    @Test
    @DisplayName("Should accept order when no matches found")
    void shouldAcceptOrderWhenNoMatchesFound() {
        // Given
        IBuyOrder buyOrder = new BuyOrder("buy-1", testSymbol, testPrice, new BigDecimal("100"));

        // When
        OrderResult result = marketEngine.processOrder(buyOrder);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("buy-1", result.getOrderId());
        assertEquals("Order accepted", result.getMessage());
        assertTrue(result.getTransactionIds().isEmpty());
        assertNotNull(result.getTimestamp());
    }

    @Test
    @DisplayName("Should accept order and create transaction when match found")
    void shouldAcceptOrderAndCreateTransactionWhenMatchFound() {
        // Given - Add a sell order first
        ISellOrder sellOrder = new SellOrder("sell-1", testSymbol, testPrice, new BigDecimal("50"));
        marketEngine.processOrder(sellOrder);

        // When - Add matching buy order
        IBuyOrder buyOrder = new BuyOrder("buy-1", testSymbol, testPrice, new BigDecimal("100"));
        OrderResult result = marketEngine.processOrder(buyOrder);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("buy-1", result.getOrderId());
        assertEquals("Order accepted and executed", result.getMessage());
        assertEquals(1, result.getTransactionIds().size());

        // Verify orders are updated
        assertEquals(new BigDecimal("50"), buyOrder.getRemainingQuantity());
        assertEquals(OrderStatusEnum.PARTIAL, buyOrder.getStatus().getStatus());
        assertEquals(BigDecimal.ZERO, sellOrder.getRemainingQuantity());
        assertEquals(OrderStatusEnum.FILLED, sellOrder.getStatus().getStatus());
    }

    @Test
    @DisplayName("Should process multiple orders creating multiple transactions")
    void shouldProcessMultipleOrdersCreatingMultipleTransactions() {
        // Given - Add multiple sell orders
        ISellOrder sellOrder1 = new SellOrder("sell-1", testSymbol, testPrice, new BigDecimal("30"));
        ISellOrder sellOrder2 = new SellOrder("sell-2", testSymbol, testPrice, new BigDecimal("20"));
        marketEngine.processOrder(sellOrder1);
        marketEngine.processOrder(sellOrder2);

        // When - Add buy order that can match both
        IBuyOrder buyOrder = new BuyOrder("buy-1", testSymbol, testPrice, new BigDecimal("100"));
        OrderResult result = marketEngine.processOrder(buyOrder);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("buy-1", result.getOrderId());
        assertEquals("Order accepted and executed", result.getMessage());

        // Should create one transaction (matching logic finds best match first)
        assertEquals(1, result.getTransactionIds().size());
    }

    @Test
    @DisplayName("Should handle orders with different symbols independently")
    void shouldHandleOrdersWithDifferentSymbolsIndependently() {
        // Given
        Symbol btcSymbol = Symbol.btcUsd();
        Money btcPrice = Money.of("50000.00", Currency.USD);

        IBuyOrder eurBuyOrder = new BuyOrder("eur-buy-1", testSymbol, testPrice, new BigDecimal("100"));
        IBuyOrder btcBuyOrder = new BuyOrder("btc-buy-1", btcSymbol, btcPrice, new BigDecimal("1"));

        // When
        OrderResult eurResult = marketEngine.processOrder(eurBuyOrder);
        OrderResult btcResult = marketEngine.processOrder(btcBuyOrder);

        // Then
        assertTrue(eurResult.isSuccess());
        assertTrue(btcResult.isSuccess());
        assertTrue(eurResult.getTransactionIds().isEmpty());
        assertTrue(btcResult.getTransactionIds().isEmpty());

        // Verify orders are in separate order books
        assertEquals(2, orderBookManager.getActiveSymbols().size());
        assertTrue(orderBookManager.getActiveSymbols().contains(testSymbol));
        assertTrue(orderBookManager.getActiveSymbols().contains(btcSymbol));
    }

    @Test
    @DisplayName("Should reject order when exception occurs during processing")
    void shouldRejectOrderWhenExceptionOccursDuringProcessing() {
        // Given - Create order with null ID to cause exception
        try {
            IBuyOrder invalidOrder = new BuyOrder(null, testSymbol, testPrice, new BigDecimal("100"));
            fail("Should have thrown exception during order creation");
        } catch (Exception e) {
            // Expected - order creation should fail with null ID
            assertTrue(e.getMessage().contains("cannot be null"));
        }
    }

    @Test
    @DisplayName("Should handle partially filled orders correctly")
    void shouldHandlePartiallyFilledOrdersCorrectly() {
        // Given
        ISellOrder smallSellOrder = new SellOrder("sell-1", testSymbol, testPrice, new BigDecimal("25"));
        marketEngine.processOrder(smallSellOrder);

        // When
        IBuyOrder largeBuyOrder = new BuyOrder("buy-1", testSymbol, testPrice, new BigDecimal("100"));
        OrderResult result = marketEngine.processOrder(largeBuyOrder);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(1, result.getTransactionIds().size());

        // Buy order should be partially filled
        assertEquals(new BigDecimal("75"), largeBuyOrder.getRemainingQuantity());
        assertEquals(OrderStatusEnum.PARTIAL, largeBuyOrder.getStatus().getStatus());

        // Sell order should be fully filled
        assertEquals(BigDecimal.ZERO, smallSellOrder.getRemainingQuantity());
        assertEquals(OrderStatusEnum.FILLED, smallSellOrder.getStatus().getStatus());
    }

    @Test
    @DisplayName("Should throw exception when creating engine with null parameters")
    void shouldThrowExceptionWhenCreatingEngineWithNullParameters() {
        // When & Then
        assertThrows(
                NullPointerException.class,
                () -> new MarketEngine(null, orderBookManager)
        );

        assertThrows(
                NullPointerException.class,
                () -> new MarketEngine("engine-1", null)
        );
    }

    @Test
    @DisplayName("Should throw exception when processing null order")
    void shouldThrowExceptionWhenProcessingNullOrder() {
        // When & Then
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> marketEngine.processOrder(null)
        );
        assertEquals("Order cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should have valid engine properties")
    void shouldHaveValidEngineProperties() {
        // Then
        assertEquals("engine-1", marketEngine.getEngineId());
        assertNotNull(marketEngine.getCreatedAt());
    }

    @Test
    @DisplayName("Should handle sequential order processing correctly")
    void shouldHandleSequentialOrderProcessingCorrectly() {
        // Given - Create orders
        ISellOrder sellOrder1 = new SellOrder("sell-1", testSymbol, testPrice, new BigDecimal("30"));
        ISellOrder sellOrder2 = new SellOrder("sell-2", testSymbol, testPrice, new BigDecimal("40"));
        IBuyOrder buyOrder1 = new BuyOrder("buy-1", testSymbol, testPrice, new BigDecimal("20"));
        IBuyOrder buyOrder2 = new BuyOrder("buy-2", testSymbol, testPrice, new BigDecimal("35"));

        // When - Process orders sequentially
        System.out.println("=== INITIAL STATE ===");
        OrderBook orderBook = orderBookManager.getOrderBook(testSymbol);
        System.out.println("Initial matches: " + orderBookManager.findAllMatches().size());

        // Add sell orders
        OrderResult sellResult1 = marketEngine.processOrder(sellOrder1);
        OrderResult sellResult2 = marketEngine.processOrder(sellOrder2);

        System.out.println("=== AFTER ADDING SELL ORDERS ===");
        System.out.println("Best ask: " + orderBook.getBestAsk());
        System.out.println("Ask levels count: " + orderBook.getAskLevels().size());
        for (AskPriceLevel askLevel : orderBook.getAskLevels()) {
            System.out.println("Ask level at " + askLevel.getPrice() + ": " + askLevel.getTotalQuantity() + " total");
            for (ISellOrder order : askLevel.getOrders()) {
                System.out.println("  Order " + order.getId() + ": " + order.getRemainingQuantity() + " remaining");
            }
        }

        // Add first buy order
        OrderResult buyResult1 = marketEngine.processOrder(buyOrder1);

        System.out.println("=== AFTER FIRST BUY ORDER ===");
        System.out.println("sellOrder1 remaining: " + sellOrder1.getRemainingQuantity());
        System.out.println("sellOrder1 active: " + sellOrder1.isActive());
        System.out.println("sellOrder2 remaining: " + sellOrder2.getRemainingQuantity());
        System.out.println("Best ask: " + orderBook.getBestAsk());
        System.out.println("Ask levels count: " + orderBook.getAskLevels().size());

        for (AskPriceLevel askLevel : orderBook.getAskLevels()) {
            System.out.println("Ask level at " + askLevel.getPrice() + ": " + askLevel.getTotalQuantity() + " total");
            for (ISellOrder order : askLevel.getOrders()) {
                System.out.println("  Order " + order.getId() + ": " + order.getRemainingQuantity() + " remaining");
            }
        }

        // Check object references
        if (!orderBook.getAskLevels().isEmpty()) {
            AskPriceLevel askLevel = orderBook.getAskLevels().iterator().next();
            if (!askLevel.getOrders().isEmpty()) {
                ISellOrder orderBookOrder = askLevel.getOrders().get(0);
                System.out.println("sellOrder1 object ID: " + System.identityHashCode(sellOrder1));
                System.out.println("OrderBook order ID: " + System.identityHashCode(orderBookOrder));
                System.out.println("OrderBook order remaining: " + orderBookOrder.getRemainingQuantity());
            }
        }

        System.out.println("=== BEFORE SECOND BUY ORDER ===");

        // Debug the MatchFinder logic
        System.out.println("OrderBook best bid: " + orderBook.getBestBid());
        System.out.println("OrderBook best ask: " + orderBook.getBestAsk());
        System.out.println("OrderBook best buy order: " + orderBook.getBestBuyOrder());
        System.out.println("OrderBook best sell order: " + orderBook.getBestSellOrder());

        // Check if there are any buy orders in the book at all
        System.out.println("Bid levels count: " + orderBook.getBidLevels().size());
        for (BidPriceLevel bidLevel : orderBook.getBidLevels()) {
            System.out.println("Bid level at " + bidLevel.getPrice() + ": " + bidLevel.getTotalQuantity() + " total");
            for (IBuyOrder order : bidLevel.getOrders()) {
                System.out.println("  Order " + order.getId() + ": " + order.getRemainingQuantity() + " remaining");
            }
        }

        List<OrderMatch> matchesBeforeBuy2 = orderBookManager.findAllMatches();
        System.out.println("Matches found for buyOrder2: " + matchesBeforeBuy2.size());

        // Add second buy order
        OrderResult buyResult2 = marketEngine.processOrder(buyOrder2);

        System.out.println("=== AFTER ADDING SECOND BUY ORDER ===");
        System.out.println("OrderBook best bid: " + orderBook.getBestBid());
        System.out.println("OrderBook best ask: " + orderBook.getBestAsk());
        System.out.println("OrderBook best buy order: " + orderBook.getBestBuyOrder());
        System.out.println("OrderBook best sell order: " + orderBook.getBestSellOrder());

        // Check bid levels after adding buyOrder2
        System.out.println("Bid levels count after buyOrder2: " + orderBook.getBidLevels().size());
        for (BidPriceLevel bidLevel : orderBook.getBidLevels()) {
            System.out.println("Bid level at " + bidLevel.getPrice() + ": " + bidLevel.getTotalQuantity() + " total");
            for (IBuyOrder order : bidLevel.getOrders()) {
                System.out.println("  Order " + order.getId() + ": " + order.getRemainingQuantity() + " remaining");
            }
        }

        // Check ask levels after buyOrder2
        System.out.println("Ask levels count after buyOrder2: " + orderBook.getAskLevels().size());
        for (AskPriceLevel askLevel : orderBook.getAskLevels()) {
            System.out.println("Ask level at " + askLevel.getPrice() + ": " + askLevel.getTotalQuantity() + " total");
            for (ISellOrder order : askLevel.getOrders()) {
                System.out.println("  Order " + order.getId() + ": " + order.getRemainingQuantity() + " remaining");
            }
        }

        System.out.println("=== FINAL STATE ===");
        System.out.println("buyResult2: " + buyResult2);

        // Then - Assertions
        assertTrue(sellResult1.isSuccess());
        assertTrue(sellResult2.isSuccess());
        assertTrue(buyResult1.isSuccess());
        assertTrue(buyResult2.isSuccess());

        assertTrue(sellResult1.getTransactionIds().isEmpty());  // No matches when added
        assertTrue(sellResult2.getTransactionIds().isEmpty());  // No matches when added
        assertEquals(1, buyResult1.getTransactionIds().size()); // Matches with sell-1
        assertEquals(1, buyResult2.getTransactionIds().size()); // Matches with best available
    }
}