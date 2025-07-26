package core.ms.order_book;

import core.ms.order_book.domain.entities.OrderBook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order_book.domain.value_object.*;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import core.ms.shared.domain.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Collection;
import java.util.List;

@DisplayName("Order Book Tests")
class OrderBookTest {

    private OrderBook orderBook;
    private Symbol symbol;
    private IBuyOrder buyOrder1;
    private IBuyOrder buyOrder2;
    private IBuyOrder buyOrder3;
    private ISellOrder sellOrder1;
    private ISellOrder sellOrder2;
    private ISellOrder sellOrder3;

    @BeforeEach
    void setUp() {
        symbol = Symbol.btcUsd();
        orderBook = new OrderBook(symbol);

        // Setup buy orders with different prices and times
        buyOrder1 = createMockBuyOrder("buy1", Money.of("101.00", Currency.USD),
                new BigDecimal("10"), LocalDateTime.of(2024, 1, 1, 10, 0));
        buyOrder2 = createMockBuyOrder("buy2", Money.of("100.00", Currency.USD),
                new BigDecimal("5"), LocalDateTime.of(2024, 1, 1, 10, 1));
        buyOrder3 = createMockBuyOrder("buy3", Money.of("101.00", Currency.USD),
                new BigDecimal("8"), LocalDateTime.of(2024, 1, 1, 10, 2));

        // Setup sell orders with different prices and times
        sellOrder1 = createMockSellOrder("sell1", Money.of("102.00", Currency.USD),
                new BigDecimal("6"), LocalDateTime.of(2024, 1, 1, 10, 0));
        sellOrder2 = createMockSellOrder("sell2", Money.of("103.00", Currency.USD),
                new BigDecimal("4"), LocalDateTime.of(2024, 1, 1, 10, 1));
        sellOrder3 = createMockSellOrder("sell3", Money.of("102.00", Currency.USD),
                new BigDecimal("7"), LocalDateTime.of(2024, 1, 1, 10, 2));
    }

    @Nested
    @DisplayName("Order Book Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("Should initialize empty order book correctly")
        void should_initialize_empty_order_book_correctly() {
            // Given: New order book
            OrderBook newBook = new OrderBook(symbol);

            // When/Then: Should be empty
            assertEquals(symbol, newBook.getSymbol());
            assertTrue(newBook.isEmpty());
            assertFalse(newBook.hasOrders());
            assertEquals(0, newBook.getOrderCount());
            assertEquals(BigDecimal.ZERO, newBook.getTotalBidVolume());
            assertEquals(BigDecimal.ZERO, newBook.getTotalAskVolume());
            assertFalse(newBook.getBestBid().isPresent());
            assertFalse(newBook.getBestAsk().isPresent());
            assertFalse(newBook.getSpread().isPresent());
            assertFalse(newBook.getBestBuyOrder().isPresent());
            assertFalse(newBook.getBestSellOrder().isPresent());
            assertTrue(newBook.getBidLevels().isEmpty());
            assertTrue(newBook.getAskLevels().isEmpty());
            assertNotNull(newBook.getLastUpdate());
        }

        @Test
        @DisplayName("Should reject null symbol")
        void should_reject_null_symbol() {
            // When/Then: Should throw exception
            assertThrows(NullPointerException.class, () ->
                    new OrderBook(null));
        }
    }

    @Nested
    @DisplayName("Buy Order Management Tests")
    class BuyOrderManagementTests {

        @Test
        @DisplayName("Should add buy order to correct bid level")
        void should_add_buy_order_to_correct_bid_level() {
            // Given: Empty order book
            assertTrue(orderBook.isEmpty());

            // When: Adding buy order
            orderBook.addOrder(buyOrder1);

            // Then: Should create bid level and add order
            assertFalse(orderBook.isEmpty());
            assertTrue(orderBook.hasOrders());
            assertEquals(1, orderBook.getOrderCount());
            assertEquals(Money.of("101.00", Currency.USD), orderBook.getBestBid().get());
            assertEquals("buy1", orderBook.getBestBuyOrder().get().getId());
            assertEquals(new BigDecimal("10"), orderBook.getTotalBidVolume());
            assertEquals(BigDecimal.ZERO, orderBook.getTotalAskVolume());

            // And: Should have one bid level
            Collection<BidPriceLevel> bidLevels = orderBook.getBidLevels();
            assertEquals(1, bidLevels.size());
            BidPriceLevel bidLevel = bidLevels.iterator().next();
            assertEquals(Money.of("101.00", Currency.USD), bidLevel.getPrice());
            assertEquals(1, bidLevel.getOrderCount());
            assertEquals(new BigDecimal("10"), bidLevel.getTotalQuantity());
        }

        @Test
        @DisplayName("Should maintain price priority across bid levels")
        void should_maintain_price_priority_across_bid_levels() {
            // Given: Order book

            // When: Adding buy orders at different prices
            orderBook.addOrder(buyOrder2); // $100.00
            orderBook.addOrder(buyOrder1); // $101.00 (higher)

            // Then: Best bid should be highest price
            assertEquals(Money.of("101.00", Currency.USD), orderBook.getBestBid().get());
            assertEquals("buy1", orderBook.getBestBuyOrder().get().getId());

            // And: Total volume should be aggregated
            assertEquals(new BigDecimal("15"), orderBook.getTotalBidVolume());

            // And: Should have two bid levels
            Collection<BidPriceLevel> bidLevels = orderBook.getBidLevels();
            assertEquals(2, bidLevels.size());
        }

        @Test
        @DisplayName("Should handle multiple orders at same price level")
        void should_handle_multiple_orders_at_same_price_level() {
            // Given: Order book

            // When: Adding multiple orders at same price
            orderBook.addOrder(buyOrder1); // $101.00, time 10:00
            orderBook.addOrder(buyOrder3); // $101.00, time 10:02

            // Then: Should aggregate volume and maintain time priority
            assertEquals(new BigDecimal("18"), orderBook.getTotalBidVolume());
            assertEquals("buy1", orderBook.getBestBuyOrder().get().getId()); // Earlier time
            assertEquals(2, orderBook.getOrderCount());

            // And: Should have one bid level with multiple orders
            Collection<BidPriceLevel> bidLevels = orderBook.getBidLevels();
            assertEquals(1, bidLevels.size());
            BidPriceLevel bidLevel = bidLevels.iterator().next();
            assertEquals(2, bidLevel.getOrderCount());
            assertEquals(new BigDecimal("18"), bidLevel.getTotalQuantity());
        }

        @Test
        @DisplayName("Should update last update time when adding order")
        void should_update_last_update_time_when_adding_order() {
            // Given: Order book with initial time
            LocalDateTime initialTime = orderBook.getLastUpdate();

            // When: Adding order after a delay
            try {
                Thread.sleep(1); // Ensure time difference
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            orderBook.addOrder(buyOrder1);

            // Then: Last update should be more recent
            assertTrue(orderBook.getLastUpdate().isAfter(initialTime));
        }
    }

    @Nested
    @DisplayName("Sell Order Management Tests")
    class SellOrderManagementTests {

        @Test
        @DisplayName("Should add sell order to correct ask level")
        void should_add_sell_order_to_correct_ask_level() {
            // Given: Empty order book
            assertTrue(orderBook.isEmpty());

            // When: Adding sell order
            orderBook.addOrder(sellOrder1);

            // Then: Should create ask level and add order
            assertFalse(orderBook.isEmpty());
            assertTrue(orderBook.hasOrders());
            assertEquals(1, orderBook.getOrderCount());
            assertEquals(Money.of("102.00", Currency.USD), orderBook.getBestAsk().get());
            assertEquals("sell1", orderBook.getBestSellOrder().get().getId());
            assertEquals(BigDecimal.ZERO, orderBook.getTotalBidVolume());
            assertEquals(new BigDecimal("6"), orderBook.getTotalAskVolume());

            // And: Should have one ask level
            Collection<AskPriceLevel> askLevels = orderBook.getAskLevels();
            assertEquals(1, askLevels.size());
            AskPriceLevel askLevel = askLevels.iterator().next();
            assertEquals(Money.of("102.00", Currency.USD), askLevel.getPrice());
            assertEquals(1, askLevel.getOrderCount());
            assertEquals(new BigDecimal("6"), askLevel.getTotalQuantity());
        }

        @Test
        @DisplayName("Should maintain price priority across ask levels")
        void should_maintain_price_priority_across_ask_levels() {
            // Given: Order book

            // When: Adding sell orders at different prices
            orderBook.addOrder(sellOrder2); // $103.00
            orderBook.addOrder(sellOrder1); // $102.00 (lower/better)

            // Then: Best ask should be lowest price
            assertEquals(Money.of("102.00", Currency.USD), orderBook.getBestAsk().get());
            assertEquals("sell1", orderBook.getBestSellOrder().get().getId());

            // And: Total volume should be aggregated
            assertEquals(new BigDecimal("10"), orderBook.getTotalAskVolume());

            // And: Should have two ask levels
            Collection<AskPriceLevel> askLevels = orderBook.getAskLevels();
            assertEquals(2, askLevels.size());
        }

        @Test
        @DisplayName("Should handle multiple orders at same ask level")
        void should_handle_multiple_orders_at_same_ask_level() {
            // Given: Order book

            // When: Adding multiple orders at same price
            orderBook.addOrder(sellOrder1); // $102.00, time 10:00
            orderBook.addOrder(sellOrder3); // $102.00, time 10:02

            // Then: Should aggregate volume and maintain time priority
            assertEquals(new BigDecimal("13"), orderBook.getTotalAskVolume());
            assertEquals("sell1", orderBook.getBestSellOrder().get().getId()); // Earlier time
            assertEquals(2, orderBook.getOrderCount());

            // And: Should have one ask level with multiple orders
            Collection<AskPriceLevel> askLevels = orderBook.getAskLevels();
            assertEquals(1, askLevels.size());
            AskPriceLevel askLevel = askLevels.iterator().next();
            assertEquals(2, askLevel.getOrderCount());
            assertEquals(new BigDecimal("13"), askLevel.getTotalQuantity());
        }
    }

    @Nested
    @DisplayName("Order Removal Tests")
    class OrderRemovalTests {

        @Test
        @DisplayName("Should remove buy order and update metrics")
        void should_remove_buy_order_and_update_metrics() {
            // Given: Order book with buy orders
            orderBook.addOrder(buyOrder1);
            orderBook.addOrder(buyOrder2);
            assertEquals(2, orderBook.getOrderCount());
            assertEquals(new BigDecimal("15"), orderBook.getTotalBidVolume());

            // When: Removing buy order
            boolean removed = orderBook.removeOrder(buyOrder1);

            // Then: Should remove successfully and update metrics
            assertTrue(removed);
            assertEquals(1, orderBook.getOrderCount());
            assertEquals(new BigDecimal("5"), orderBook.getTotalBidVolume());
            assertEquals(Money.of("100.00", Currency.USD), orderBook.getBestBid().get());
            assertEquals("buy2", orderBook.getBestBuyOrder().get().getId());
        }

        @Test
        @DisplayName("Should remove sell order and update metrics")
        void should_remove_sell_order_and_update_metrics() {
            // Given: Order book with sell orders
            orderBook.addOrder(sellOrder1);
            orderBook.addOrder(sellOrder2);
            assertEquals(2, orderBook.getOrderCount());
            assertEquals(new BigDecimal("10"), orderBook.getTotalAskVolume());

            // When: Removing sell order
            boolean removed = orderBook.removeOrder(sellOrder1);

            // Then: Should remove successfully and update metrics
            assertTrue(removed);
            assertEquals(1, orderBook.getOrderCount());
            assertEquals(new BigDecimal("4"), orderBook.getTotalAskVolume());
            assertEquals(Money.of("103.00", Currency.USD), orderBook.getBestAsk().get());
            assertEquals("sell2", orderBook.getBestSellOrder().get().getId());
        }

        @Test
        @DisplayName("Should remove empty price levels")
        void should_remove_empty_price_levels() {
            // Given: Price level with single order
            orderBook.addOrder(buyOrder1);
            assertEquals(1, orderBook.getBidLevels().size());

            // When: Removing the only order at that price
            boolean removed = orderBook.removeOrder(buyOrder1);

            // Then: Price level should be removed
            assertTrue(removed);
            assertTrue(orderBook.getBidLevels().isEmpty());
            assertFalse(orderBook.getBestBid().isPresent());
            assertFalse(orderBook.getBestBuyOrder().isPresent());
            assertEquals(BigDecimal.ZERO, orderBook.getTotalBidVolume());
            assertTrue(orderBook.isEmpty());
        }

        @Test
        @DisplayName("Should maintain best prices after removal")
        void should_maintain_best_prices_after_removal() {
            // Given: Multiple price levels
            orderBook.addOrder(buyOrder1); // $101.00 (best)
            orderBook.addOrder(buyOrder2); // $100.00 (second best)
            orderBook.addOrder(sellOrder1); // $102.00 (best ask)
            orderBook.addOrder(sellOrder2); // $103.00 (second best ask)

            // When: Removing best orders
            orderBook.removeOrder(buyOrder1);
            orderBook.removeOrder(sellOrder1);

            // Then: Second best should become best
            assertEquals(Money.of("100.00", Currency.USD), orderBook.getBestBid().get());
            assertEquals(Money.of("103.00", Currency.USD), orderBook.getBestAsk().get());
            assertEquals("buy2", orderBook.getBestBuyOrder().get().getId());
            assertEquals("sell2", orderBook.getBestSellOrder().get().getId());
        }

        @Test
        @DisplayName("Should return false when removing non-existent order")
        void should_return_false_when_removing_non_existent_order() {
            // Given: Order book with one order
            orderBook.addOrder(buyOrder1);
            int initialCount = orderBook.getOrderCount();

            // When: Removing different order
            boolean removed = orderBook.removeOrder(buyOrder2);

            // Then: Should return false and not affect book
            assertFalse(removed);
            assertEquals(initialCount, orderBook.getOrderCount());
            assertEquals(buyOrder1.getId(), orderBook.getBestBuyOrder().get().getId());
        }

        @Test
        @DisplayName("Should update last update time when removing order")
        void should_update_last_update_time_when_removing_order() {
            // Given: Order book with order
            orderBook.addOrder(buyOrder1);
            LocalDateTime timeAfterAdd = orderBook.getLastUpdate();

            // When: Removing order after a delay
            try {
                Thread.sleep(1); // Ensure time difference
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            orderBook.removeOrder(buyOrder1);

            // Then: Last update should be more recent
            assertTrue(orderBook.getLastUpdate().isAfter(timeAfterAdd));
        }
    }

    @Nested
    @DisplayName("Spread Calculation Tests")
    class SpreadCalculationTests {

        @Test
        @DisplayName("Should calculate spread correctly")
        void should_calculate_spread_correctly() {
            // Given: Order book with bid and ask
            orderBook.addOrder(buyOrder2);  // $100.00 (bid)
            orderBook.addOrder(sellOrder1); // $102.00 (ask)

            // When: Getting spread
            Optional<Money> spread = orderBook.getSpread();

            // Then: Should be ask - bid
            assertTrue(spread.isPresent());
            assertEquals(Money.of("2.00", Currency.USD), spread.get());
        }

        @Test
        @DisplayName("Should calculate spread with multiple levels")
        void should_calculate_spread_with_multiple_levels() {
            // Given: Order book with multiple levels
            orderBook.addOrder(buyOrder1); // $101.00 (best bid)
            orderBook.addOrder(buyOrder2); // $100.00
            orderBook.addOrder(sellOrder1); // $102.00 (best ask)
            orderBook.addOrder(sellOrder2); // $103.00

            // When: Getting spread
            Optional<Money> spread = orderBook.getSpread();

            // Then: Should use best bid and ask
            assertTrue(spread.isPresent());
            assertEquals(Money.of("1.00", Currency.USD), spread.get());
        }

        @Test
        @DisplayName("Should return empty spread when missing bid side")
        void should_return_empty_spread_when_missing_bid_side() {
            // Given: Order book with only ask
            orderBook.addOrder(sellOrder1);

            // When: Getting spread
            Optional<Money> spread = orderBook.getSpread();

            // Then: Should be empty
            assertFalse(spread.isPresent());
        }

        @Test
        @DisplayName("Should return empty spread when missing ask side")
        void should_return_empty_spread_when_missing_ask_side() {
            // Given: Order book with only bid
            orderBook.addOrder(buyOrder1);

            // When: Getting spread
            Optional<Money> spread = orderBook.getSpread();

            // Then: Should be empty
            assertFalse(spread.isPresent());
        }

        @Test
        @DisplayName("Should return empty spread when no orders")
        void should_return_empty_spread_when_no_orders() {
            // Given: Empty order book
            assertTrue(orderBook.isEmpty());

            // When: Getting spread
            Optional<Money> spread = orderBook.getSpread();

            // Then: Should be empty
            assertFalse(spread.isPresent());
        }
    }

    @Nested
    @DisplayName("Market Depth Tests")
    class MarketDepthTests {

        @Test
        @DisplayName("Should return correct market depth")
        void should_return_correct_market_depth() {
            // Given: Order book with multiple levels
            setupOrderBookWithMultipleLevels();

            // When: Getting market depth
            MarketDepth depth = orderBook.getMarketDepth(2);

            // Then: Should return top 2 levels each side
            assertEquals(2, depth.getBidLevels().size());
            assertEquals(2, depth.getAskLevels().size());
            assertEquals(symbol, depth.getSymbol());
            assertNotNull(depth.getTimestamp());

            // And: Should be price-ordered
            List<BidPriceLevel> bids = depth.getBidLevels();
            assertEquals(Money.of("101.00", Currency.USD), bids.get(0).getPrice()); // Best
            assertEquals(Money.of("100.00", Currency.USD), bids.get(1).getPrice()); // Second

            List<AskPriceLevel> asks = depth.getAskLevels();
            assertEquals(Money.of("102.00", Currency.USD), asks.get(0).getPrice()); // Best
            assertEquals(Money.of("103.00", Currency.USD), asks.get(1).getPrice()); // Second
        }

        @Test
        @DisplayName("Should limit depth to available levels")
        void should_limit_depth_to_available_levels() {
            // Given: Order book with 1 level each side
            orderBook.addOrder(buyOrder1);
            orderBook.addOrder(sellOrder1);

            // When: Requesting 5 levels
            MarketDepth depth = orderBook.getMarketDepth(5);

            // Then: Should return only available levels
            assertEquals(1, depth.getBidLevels().size());
            assertEquals(1, depth.getAskLevels().size());
            assertEquals(Money.of("101.00", Currency.USD), depth.getBidLevels().get(0).getPrice());
            assertEquals(Money.of("102.00", Currency.USD), depth.getAskLevels().get(0).getPrice());
        }

        @Test
        @DisplayName("Should handle empty order book")
        void should_handle_empty_order_book() {
            // Given: Empty order book
            assertTrue(orderBook.isEmpty());

            // When: Getting market depth
            MarketDepth depth = orderBook.getMarketDepth(5);

            // Then: Should return empty depth
            assertTrue(depth.getBidLevels().isEmpty());
            assertTrue(depth.getAskLevels().isEmpty());
            assertTrue(depth.isEmpty());
            assertEquals(0, depth.getLevelCount());
        }

        @Test
        @DisplayName("Should reject invalid depth levels")
        void should_reject_invalid_depth_levels() {
            // Given: Order book

            // When/Then: Should throw exception for invalid levels
            assertThrows(IllegalArgumentException.class, () ->
                    orderBook.getMarketDepth(0));
            assertThrows(IllegalArgumentException.class, () ->
                    orderBook.getMarketDepth(-1));
        }

        @Test
        @DisplayName("Should calculate depth metrics correctly")
        void should_calculate_depth_metrics_correctly() {
            // Given: Order book with multiple levels
            setupOrderBookWithMultipleLevels();

            // When: Getting market depth
            MarketDepth depth = orderBook.getMarketDepth(10);

            // Then: Should calculate metrics correctly
            assertEquals(new BigDecimal("15"), depth.getTotalBidVolume());
            assertEquals(new BigDecimal("10"), depth.getTotalAskVolume());
            assertEquals(Money.of("1.00", Currency.USD), depth.getSpread());
            assertEquals(Money.of("101.00", Currency.USD), depth.getBestBid().get().getPrice());
            assertEquals(Money.of("102.00", Currency.USD), depth.getBestAsk().get().getPrice());
        }
    }

    @Nested
    @DisplayName("Order Matching Tests")
    class OrderMatchingTests {

        @Test
        @DisplayName("Should find match when bid meets ask")
        void should_find_match_when_bid_meets_ask() {
            // Given: Orders that can match
            IBuyOrder matchingBuyOrder = createMockBuyOrder("buy-match",
                    Money.of("103.00", Currency.USD), new BigDecimal("10"),
                    LocalDateTime.of(2024, 1, 1, 10, 0));
            orderBook.addOrder(matchingBuyOrder);
            orderBook.addOrder(sellOrder1); // $102.00

            // When: Finding matches
            List<OrderMatch> matches = orderBook.findMatches();

            // Then: Should find one match
            assertEquals(1, matches.size());
            OrderMatch match = matches.get(0);
            assertEquals("buy-match", match.getBuyOrder().getId());
            assertEquals("sell1", match.getSellOrder().getId());
            assertEquals(new BigDecimal("6"), match.getMatchableQuantity());
            assertTrue(match.isValid());
        }

        @Test
        @DisplayName("Should not find match when bid below ask")
        void should_not_find_match_when_bid_below_ask() {
            // Given: Orders that cannot match
            orderBook.addOrder(buyOrder2); // $100.00
            orderBook.addOrder(sellOrder1); // $102.00

            // When: Finding matches
            List<OrderMatch> matches = orderBook.findMatches();

            // Then: Should find no matches
            assertTrue(matches.isEmpty());
        }

        @Test
        @DisplayName("Should not find match in empty order book")
        void should_not_find_match_in_empty_order_book() {
            // Given: Empty order book
            assertTrue(orderBook.isEmpty());

            // When: Finding matches
            List<OrderMatch> matches = orderBook.findMatches();

            // Then: Should find no matches
            assertTrue(matches.isEmpty());
        }

        @Test
        @DisplayName("Should not find match with only one side")
        void should_not_find_match_with_only_one_side() {
            // Given: Order book with only buy orders
            orderBook.addOrder(buyOrder1);
            orderBook.addOrder(buyOrder2);

            // When: Finding matches
            List<OrderMatch> matches = orderBook.findMatches();

            // Then: Should find no matches
            assertTrue(matches.isEmpty());
        }
    }

    @Nested
    @DisplayName("Order Validation Tests")
    class OrderValidationTests {

        @Test
        @DisplayName("Should reject order with wrong symbol")
        void should_reject_order_with_wrong_symbol() {
            // Given: Order with different symbol
            IBuyOrder wrongSymbolOrder = createMockBuyOrder("wrong-symbol",
                    Money.of("100.00", Currency.USD), new BigDecimal("10"),
                    LocalDateTime.of(2024, 1, 1, 10, 0));
            when(wrongSymbolOrder.getSymbol()).thenReturn(Symbol.ethUsd());

            // When/Then: Should throw exception
            assertThrows(IllegalArgumentException.class, () ->
                    orderBook.addOrder(wrongSymbolOrder));
        }

        @Test
        @DisplayName("Should reject inactive order")
        void should_reject_inactive_order() {
            // Given: Inactive order
            IBuyOrder inactiveOrder = createMockBuyOrder("inactive",
                    Money.of("100.00", Currency.USD), new BigDecimal("10"),
                    LocalDateTime.of(2024, 1, 1, 10, 0));
            when(inactiveOrder.isActive()).thenReturn(false);

            // When/Then: Should throw exception
            assertThrows(IllegalArgumentException.class, () ->
                    orderBook.addOrder(inactiveOrder));
        }

        @Test
        @DisplayName("Should reject duplicate order")
        void should_reject_duplicate_order() {
            // Given: Order already in book
            orderBook.addOrder(buyOrder1);

            // When/Then: Should throw exception for duplicate
            assertThrows(IllegalArgumentException.class, () ->
                    orderBook.addOrder(buyOrder1));
        }

        @Test
        @DisplayName("Should reject null order")
        void should_reject_null_order() {
            // When/Then: Should throw exception
            assertThrows(NullPointerException.class, () ->
                    orderBook.addOrder(null));
        }

        @Test
        @DisplayName("Should reject null order for removal")
        void should_reject_null_order_for_removal() {
            // When/Then: Should throw exception
            assertThrows(NullPointerException.class, () ->
                    orderBook.removeOrder(null));
        }
    }

    @Nested
    @DisplayName("State Query Tests")
    class StateQueryTests {

        @Test
        @DisplayName("Should return correct order count")
        void should_return_correct_order_count() {
            // Given: Empty order book
            assertEquals(0, orderBook.getOrderCount());

            // When: Adding orders
            orderBook.addOrder(buyOrder1);
            orderBook.addOrder(buyOrder2);
            orderBook.addOrder(sellOrder1);

            // Then: Should return correct count
            assertEquals(3, orderBook.getOrderCount());

            // When: Removing order
            orderBook.removeOrder(buyOrder1);

            // Then: Should update count
            assertEquals(2, orderBook.getOrderCount());
        }

        @Test
        @DisplayName("Should return correct volume metrics")
        void should_return_correct_volume_metrics() {
            // Given: Order book with mixed orders
            orderBook.addOrder(buyOrder1);  // 10
            orderBook.addOrder(buyOrder2);  // 5
            orderBook.addOrder(sellOrder1); // 6
            orderBook.addOrder(sellOrder2); // 4

            // When/Then: Should calculate volumes correctly
            assertEquals(new BigDecimal("15"), orderBook.getTotalBidVolume());
            assertEquals(new BigDecimal("10"), orderBook.getTotalAskVolume());
        }

        @Test
        @DisplayName("Should provide defensive copies of levels")
        void should_provide_defensive_copies_of_levels() {
            // Given: Order book with orders
            orderBook.addOrder(buyOrder1);
            orderBook.addOrder(sellOrder1);

            // When: Getting level collections
            Collection<BidPriceLevel> bidLevels = orderBook.getBidLevels();
            Collection<AskPriceLevel> askLevels = orderBook.getAskLevels();

            // Then: Should be defensive copies
            assertEquals(1, bidLevels.size());
            assertEquals(1, askLevels.size());

            // And: Modifying returned collections should not affect book
            bidLevels.clear();
            askLevels.clear();
            assertEquals(1, orderBook.getBidLevels().size());
            assertEquals(1, orderBook.getAskLevels().size());
        }
    }

    // Helper methods
    private void setupOrderBookWithMultipleLevels() {
        // Add multiple bid levels
        orderBook.addOrder(buyOrder1); // $101.00
        orderBook.addOrder(buyOrder2); // $100.00

        // Add multiple ask levels
        orderBook.addOrder(sellOrder1); // $102.00
        orderBook.addOrder(sellOrder2); // $103.00
    }

    private IBuyOrder createMockBuyOrder(String id, Money price, BigDecimal quantity, LocalDateTime time) {
        IBuyOrder order = mock(IBuyOrder.class);
        when(order.getId()).thenReturn(id);
        when(order.getSymbol()).thenReturn(symbol);
        when(order.getPrice()).thenReturn(price);
        when(order.getRemainingQuantity()).thenReturn(quantity);
        when(order.getCreatedAt()).thenReturn(time);
        when(order.isActive()).thenReturn(true);
        return order;
    }

    private ISellOrder createMockSellOrder(String id, Money price, BigDecimal quantity, LocalDateTime time) {
        ISellOrder order = mock(ISellOrder.class);
        when(order.getId()).thenReturn(id);
        when(order.getSymbol()).thenReturn(symbol);
        when(order.getPrice()).thenReturn(price);
        when(order.getRemainingQuantity()).thenReturn(quantity);
        when(order.getCreatedAt()).thenReturn(time);
        when(order.isActive()).thenReturn(true);
        return order;
    }
}