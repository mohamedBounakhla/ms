package core.ms.order_book;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order_book.domain.entities.OrderBook;
import core.ms.order_book.domain.entities.OrderBookManager;
import core.ms.order_book.domain.value_object.MarketOverview;
import core.ms.order_book.domain.value_object.OrderMatch;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Order Book Manager Tests")
class OrderBookManagerTest {

    private OrderBookManager manager;
    private Symbol btcSymbol;
    private Symbol ethSymbol;
    private Symbol aaplSymbol;
    private IBuyOrder btcBuyOrder;
    private ISellOrder btcSellOrder;
    private IBuyOrder ethBuyOrder;
    private ISellOrder ethSellOrder;
    private IBuyOrder aaplBuyOrder;
    private ISellOrder aaplSellOrder;

    @BeforeEach
    void setUp() {
        manager = new OrderBookManager();
        btcSymbol = Symbol.btcUsd();
        ethSymbol = Symbol.ethUsd();
        aaplSymbol = createMockSymbol("AAPL");

        // Setup orders for different symbols
        btcBuyOrder = createMockBuyOrder("btc-buy-1", btcSymbol,
                Money.of("50000.00", Currency.USD), new BigDecimal("1.5"),
                LocalDateTime.of(2024, 1, 1, 10, 0));
        btcSellOrder = createMockSellOrder("btc-sell-1", btcSymbol,
                Money.of("49500.00", Currency.USD), new BigDecimal("0.8"),
                LocalDateTime.of(2024, 1, 1, 10, 1));

        ethBuyOrder = createMockBuyOrder("eth-buy-1", ethSymbol,
                Money.of("3000.00", Currency.USD), new BigDecimal("5.0"),
                LocalDateTime.of(2024, 1, 1, 10, 2));
        ethSellOrder = createMockSellOrder("eth-sell-1", ethSymbol,
                Money.of("3100.00", Currency.USD), new BigDecimal("3.0"),
                LocalDateTime.of(2024, 1, 1, 10, 3));

        aaplBuyOrder = createMockBuyOrder("aapl-buy-1", aaplSymbol,
                Money.of("180.00", Currency.USD), new BigDecimal("100"),
                LocalDateTime.of(2024, 1, 1, 10, 4));
        aaplSellOrder = createMockSellOrder("aapl-sell-1", aaplSymbol,
                Money.of("179.50", Currency.USD), new BigDecimal("150"),
                LocalDateTime.of(2024, 1, 1, 10, 5));
    }

    @Nested
    @DisplayName("Order Book Management Tests")
    class OrderBookManagementTests {

        @Test
        @DisplayName("Should create order book for new symbol")
        void should_create_order_book_for_new_symbol() {
            // Given: Empty manager
            assertEquals(0, manager.getTotalOrderBooks());

            // When: Getting order book for new symbol
            OrderBook orderBook = manager.getOrderBook(btcSymbol);

            // Then: Should create and return new book
            assertNotNull(orderBook);
            assertEquals(btcSymbol, orderBook.getSymbol());
            assertEquals(1, manager.getTotalOrderBooks());
            assertTrue(manager.getActiveSymbols().contains(btcSymbol));
            assertTrue(orderBook.isEmpty());
        }

        @Test
        @DisplayName("Should return existing order book")
        void should_return_existing_order_book() {
            // Given: Manager with existing book
            OrderBook firstCall = manager.getOrderBook(btcSymbol);

            // When: Getting same symbol again
            OrderBook secondCall = manager.getOrderBook(btcSymbol);

            // Then: Should return same instance
            assertSame(firstCall, secondCall);
            assertEquals(1, manager.getTotalOrderBooks());
        }

        @Test
        @DisplayName("Should create separate books for different symbols")
        void should_create_separate_books_for_different_symbols() {
            // Given: Manager

            // When: Getting books for different symbols
            OrderBook btcBook = manager.getOrderBook(btcSymbol);
            OrderBook ethBook = manager.getOrderBook(ethSymbol);
            OrderBook aaplBook = manager.getOrderBook(aaplSymbol);

            // Then: Should create separate books
            assertNotSame(btcBook, ethBook);
            assertNotSame(ethBook, aaplBook);
            assertEquals(3, manager.getTotalOrderBooks());

            Set<Symbol> activeSymbols = manager.getActiveSymbols();
            assertEquals(3, activeSymbols.size());
            assertTrue(activeSymbols.contains(btcSymbol));
            assertTrue(activeSymbols.contains(ethSymbol));
            assertTrue(activeSymbols.contains(aaplSymbol));
        }

        @Test
        @DisplayName("Should create order book explicitly")
        void should_create_order_book_explicitly() {
            // Given: Empty manager
            assertEquals(0, manager.getTotalOrderBooks());

            // When: Creating order book explicitly
            OrderBook orderBook = manager.createOrderBook(btcSymbol);

            // Then: Should create new book
            assertNotNull(orderBook);
            assertEquals(btcSymbol, orderBook.getSymbol());
            assertEquals(1, manager.getTotalOrderBooks());

            // And: Should be retrievable
            OrderBook retrieved = manager.getOrderBook(btcSymbol);
            assertSame(orderBook, retrieved);
        }

        @Test
        @DisplayName("Should reject creating duplicate order book")
        void should_reject_creating_duplicate_order_book() {
            // Given: Manager with existing book
            manager.createOrderBook(btcSymbol);

            // When/Then: Should throw exception for duplicate
            assertThrows(IllegalArgumentException.class, () ->
                    manager.createOrderBook(btcSymbol));
        }

        @Test
        @DisplayName("Should remove order book")
        void should_remove_order_book() {
            // Given: Manager with order books
            manager.getOrderBook(btcSymbol);
            manager.getOrderBook(ethSymbol);
            assertEquals(2, manager.getTotalOrderBooks());

            // When: Removing one book
            boolean removed = manager.removeOrderBook(btcSymbol);

            // Then: Should remove successfully
            assertTrue(removed);
            assertEquals(1, manager.getTotalOrderBooks());
            assertFalse(manager.getActiveSymbols().contains(btcSymbol));
            assertTrue(manager.getActiveSymbols().contains(ethSymbol));
        }

        @Test
        @DisplayName("Should return false when removing non-existent book")
        void should_return_false_when_removing_non_existent_book() {
            // Given: Empty manager
            assertEquals(0, manager.getTotalOrderBooks());

            // When: Removing non-existent book
            boolean removed = manager.removeOrderBook(btcSymbol);

            // Then: Should return false
            assertFalse(removed);
            assertEquals(0, manager.getTotalOrderBooks());
        }

        @Test
        @DisplayName("Should handle null symbol gracefully")
        void should_handle_null_symbol_gracefully() {
            // When/Then: Should throw exception for null symbol
            assertThrows(NullPointerException.class, () ->
                    manager.getOrderBook(null));
            assertThrows(NullPointerException.class, () ->
                    manager.createOrderBook(null));
            assertThrows(NullPointerException.class, () ->
                    manager.removeOrderBook(null));
        }
    }

    @Nested
    @DisplayName("Order Management Tests")
    class OrderManagementTests {

        @Test
        @DisplayName("Should add order to correct book")
        void should_add_order_to_correct_book() {
            // Given: Manager and order

            // When: Adding order
            manager.addOrderToBook(btcBuyOrder);

            // Then: Should be in correct book
            OrderBook btcBook = manager.getOrderBook(btcSymbol);
            assertEquals(1, btcBook.getOrderCount());
            assertEquals(btcBuyOrder.getId(), btcBook.getBestBuyOrder().get().getId());
            assertEquals(new BigDecimal("1.5"), btcBook.getTotalBidVolume());

            // And: Other books should remain empty
            OrderBook ethBook = manager.getOrderBook(ethSymbol);
            assertTrue(ethBook.isEmpty());
        }

        @Test
        @DisplayName("Should add multiple orders to different books")
        void should_add_multiple_orders_to_different_books() {
            // Given: Manager and orders for different symbols

            // When: Adding orders to different books
            manager.addOrderToBook(btcBuyOrder);
            manager.addOrderToBook(ethBuyOrder);
            manager.addOrderToBook(aaplBuyOrder);

            // Then: Each book should contain its order
            OrderBook btcBook = manager.getOrderBook(btcSymbol);
            OrderBook ethBook = manager.getOrderBook(ethSymbol);
            OrderBook aaplBook = manager.getOrderBook(aaplSymbol);

            assertEquals(1, btcBook.getOrderCount());
            assertEquals(1, ethBook.getOrderCount());
            assertEquals(1, aaplBook.getOrderCount());

            assertEquals("btc-buy-1", btcBook.getBestBuyOrder().get().getId());
            assertEquals("eth-buy-1", ethBook.getBestBuyOrder().get().getId());
            assertEquals("aapl-buy-1", aaplBook.getBestBuyOrder().get().getId());
        }

        @Test
        @DisplayName("Should remove order from correct book")
        void should_remove_order_from_correct_book() {
            // Given: Manager with orders in different books
            manager.addOrderToBook(btcBuyOrder);
            manager.addOrderToBook(ethBuyOrder);

            // When: Removing order from specific book
            boolean removed = manager.removeOrderFromBook(btcBuyOrder, btcSymbol);

            // Then: Should remove from correct book only
            assertTrue(removed);

            OrderBook btcBook = manager.getOrderBook(btcSymbol);
            OrderBook ethBook = manager.getOrderBook(ethSymbol);

            assertEquals(0, btcBook.getOrderCount());
            assertEquals(1, ethBook.getOrderCount());
            assertEquals("eth-buy-1", ethBook.getBestBuyOrder().get().getId());
        }

        @Test
        @DisplayName("Should return false when removing order from wrong book")
        void should_return_false_when_removing_order_from_wrong_book() {
            // Given: Order in BTC book
            manager.addOrderToBook(btcBuyOrder);

            // When: Trying to remove from ETH book
            boolean removed = manager.removeOrderFromBook(btcBuyOrder, ethSymbol);

            // Then: Should return false
            assertFalse(removed);

            // And: Order should still be in BTC book
            OrderBook btcBook = manager.getOrderBook(btcSymbol);
            assertEquals(1, btcBook.getOrderCount());
        }

        @Test
        @DisplayName("Should return false when removing from non-existent book")
        void should_return_false_when_removing_from_non_existent_book() {
            // Given: Manager without the symbol's book

            // When: Removing order from non-existent book
            boolean removed = manager.removeOrderFromBook(btcBuyOrder, btcSymbol);

            // Then: Should return false
            assertFalse(removed);
            assertEquals(0, manager.getTotalOrderBooks());
        }
    }

    @Nested
    @DisplayName("Cross-Book Operations Tests")
    class CrossBookOperationsTests {

        @Test
        @DisplayName("Should find matches across all books")
        void should_find_matches_across_all_books() {
            // Given: Multiple books with some having matches
            // BTC: Can match (buy $50k >= sell $49.5k)
            manager.addOrderToBook(btcBuyOrder);  // $50,000
            manager.addOrderToBook(btcSellOrder); // $49,500

            // ETH: Cannot match (buy $3k < sell $3.1k)
            manager.addOrderToBook(ethBuyOrder);  // $3,000
            manager.addOrderToBook(ethSellOrder); // $3,100

            // AAPL: Can match (buy $180 >= sell $179.50)
            manager.addOrderToBook(aaplBuyOrder);  // $180.00
            manager.addOrderToBook(aaplSellOrder); // $179.50

            // When: Finding all matches
            List<OrderMatch> matches = manager.findAllMatches();

            // Then: Should find matches from matching books only
            assertEquals(2, matches.size());

            // Verify BTC match
            OrderMatch btcMatch = matches.stream()
                    .filter(m -> m.getBuyOrder().getSymbol().equals(btcSymbol))
                    .findFirst().orElse(null);
            assertNotNull(btcMatch);
            assertEquals("btc-buy-1", btcMatch.getBuyOrder().getId());
            assertEquals("btc-sell-1", btcMatch.getSellOrder().getId());

            // Verify AAPL match
            OrderMatch aaplMatch = matches.stream()
                    .filter(m -> m.getBuyOrder().getSymbol().equals(aaplSymbol))
                    .findFirst().orElse(null);
            assertNotNull(aaplMatch);
            assertEquals("aapl-buy-1", aaplMatch.getBuyOrder().getId());
            assertEquals("aapl-sell-1", aaplMatch.getSellOrder().getId());

            // Verify no ETH match (no match should exist)
            boolean hasEthMatch = matches.stream()
                    .anyMatch(m -> m.getBuyOrder().getSymbol().equals(ethSymbol));
            assertFalse(hasEthMatch);
        }

        @Test
        @DisplayName("Should return empty list when no matches exist")
        void should_return_empty_list_when_no_matches_exist() {
            // Given: Books with no matching orders
            manager.addOrderToBook(ethBuyOrder);  // $3,000
            manager.addOrderToBook(ethSellOrder); // $3,100 (no match)

            // When: Finding matches
            List<OrderMatch> matches = manager.findAllMatches();

            // Then: Should return empty list
            assertTrue(matches.isEmpty());
        }

        @Test
        @DisplayName("Should return empty list for empty manager")
        void should_return_empty_list_for_empty_manager() {
            // Given: Empty manager
            assertEquals(0, manager.getTotalOrderBooks());

            // When: Finding matches
            List<OrderMatch> matches = manager.findAllMatches();

            // Then: Should return empty list
            assertTrue(matches.isEmpty());
        }

        @Test
        @DisplayName("Should aggregate data correctly in market overview")
        void should_aggregate_data_correctly_in_market_overview() {
            // Given: Manager with multiple populated books
            setupManagerWithMultipleBooks();

            // When: Getting market overview
            MarketOverview overview = manager.getMarketOverview();

            // Then: Should aggregate all data correctly
            assertEquals(3, overview.getTotalOrderBooks());
            assertEquals(6, overview.getTotalOrders()); // 2 orders per symbol

            Set<Symbol> activeSymbols = overview.getActiveSymbols();
            assertEquals(3, activeSymbols.size());
            assertTrue(activeSymbols.contains(btcSymbol));
            assertTrue(activeSymbols.contains(ethSymbol));
            assertTrue(activeSymbols.contains(aaplSymbol));

            // Verify volume calculations
            Map<Symbol, BigDecimal> volumes = overview.getTotalVolume();

            // BTC: Buy 1.5 + Sell 0.8 = 2.3
            assertEquals(new BigDecimal("2.3"), volumes.get(btcSymbol));

            // ETH: Buy 5.0 + Sell 3.0 = 8.0
            assertEquals(new BigDecimal("8.0"), volumes.get(ethSymbol));

            // AAPL: Buy 100 + Sell 150 = 250
            assertEquals(new BigDecimal("250"), volumes.get(aaplSymbol));

            assertNotNull(overview.getTimestamp());
        }

        @Test
        @DisplayName("Should return correct volume for specific symbol")
        void should_return_correct_volume_for_specific_symbol() {
            // Given: Manager with orders
            setupManagerWithMultipleBooks();
            MarketOverview overview = manager.getMarketOverview();

            // When: Getting volume for specific symbols
            BigDecimal btcVolume = overview.getVolumeForSymbol(btcSymbol);
            BigDecimal ethVolume = overview.getVolumeForSymbol(ethSymbol);
            BigDecimal unknownVolume = overview.getVolumeForSymbol(createMockSymbol("UNKNOWN"));

            // Then: Should return correct volumes
            assertEquals(new BigDecimal("2.3"), btcVolume);
            assertEquals(new BigDecimal("8.0"), ethVolume);
            assertEquals(BigDecimal.ZERO, unknownVolume); // Unknown symbol
        }

        @Test
        @DisplayName("Should handle empty books in market overview")
        void should_handle_empty_books_in_market_overview() {
            // Given: Manager with empty books
            manager.getOrderBook(btcSymbol); // Creates empty book
            manager.getOrderBook(ethSymbol); // Creates empty book

            // When: Getting market overview
            MarketOverview overview = manager.getMarketOverview();

            // Then: Should handle empty books correctly
            assertEquals(2, overview.getTotalOrderBooks());
            assertEquals(0, overview.getTotalOrders());
            assertEquals(2, overview.getActiveSymbols().size());

            Map<Symbol, BigDecimal> volumes = overview.getTotalVolume();
            assertEquals(BigDecimal.ZERO, volumes.get(btcSymbol));
            assertEquals(BigDecimal.ZERO, volumes.get(ethSymbol));
        }

        @Test
        @DisplayName("Should return all order books")
        void should_return_all_order_books() {
            // Given: Manager with multiple books
            OrderBook btcBook = manager.getOrderBook(btcSymbol);
            OrderBook ethBook = manager.getOrderBook(ethSymbol);
            OrderBook aaplBook = manager.getOrderBook(aaplSymbol);

            // When: Getting all order books
            Collection<OrderBook> allBooks = manager.getAllOrderBooks();

            // Then: Should return all books
            assertEquals(3, allBooks.size());
            assertTrue(allBooks.contains(btcBook));
            assertTrue(allBooks.contains(ethBook));
            assertTrue(allBooks.contains(aaplBook));
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent order book creation")
        void should_handle_concurrent_order_book_creation() throws Exception {
            // Given: Multiple threads trying to create same book
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch finishLatch = new CountDownLatch(threadCount);

            List<Future<OrderBook>> futures = new ArrayList<>();

            // When: Multiple threads request same order book
            for (int i = 0; i < threadCount; i++) {
                Future<OrderBook> future = executor.submit(() -> {
                    try {
                        startLatch.await(); // Wait for all threads to be ready
                        return manager.getOrderBook(btcSymbol);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    } finally {
                        finishLatch.countDown();
                    }
                });
                futures.add(future);
            }

            startLatch.countDown(); // Start all threads
            finishLatch.await(); // Wait for completion

            // Then: All threads should get same instance
            OrderBook firstBook = futures.get(0).get();
            for (Future<OrderBook> future : futures) {
                assertSame(firstBook, future.get());
            }

            // And: Only one book should be created
            assertEquals(1, manager.getTotalOrderBooks());

            executor.shutdown();
        }



        @Test
        @DisplayName("Should handle concurrent order operations")
        void should_handle_concurrent_order_operations() throws Exception {
            // Given: Shared order book and multiple orders
            OrderBook btcBook = manager.getOrderBook(btcSymbol);
            int orderCount = 20;
            ExecutorService executor = Executors.newFixedThreadPool(5);
            CountDownLatch finishLatch = new CountDownLatch(orderCount);

            // When: Multiple threads add orders concurrently
            for (int i = 0; i < orderCount; i++) {
                final int orderId = i;
                executor.submit(() -> {
                    try {
                        if (orderId % 2 == 0) {
                            IBuyOrder order = createMockBuyOrder("buy-" + orderId, btcSymbol,
                                    Money.of("50000.00", Currency.USD), new BigDecimal("0.1"),
                                    LocalDateTime.now());
                            manager.addOrderToBook(order);
                        } else {
                            ISellOrder order = createMockSellOrder("sell-" + orderId, btcSymbol,
                                    Money.of("50000.00", Currency.USD), new BigDecimal("0.1"),
                                    LocalDateTime.now());
                            manager.addOrderToBook(order);
                        }
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }

            finishLatch.await(); // Wait for completion

            // Then: All orders should be added successfully
            assertEquals(orderCount, btcBook.getOrderCount());

            executor.shutdown();
        }
    }

    // Helper methods
    private void setupManagerWithMultipleBooks() {
        // Add orders to create books with data
        manager.addOrderToBook(btcBuyOrder);
        manager.addOrderToBook(btcSellOrder);
        manager.addOrderToBook(ethBuyOrder);
        manager.addOrderToBook(ethSellOrder);
        manager.addOrderToBook(aaplBuyOrder);
        manager.addOrderToBook(aaplSellOrder);
    }

    private Symbol createMockSymbol(String code) {
        Symbol symbol = mock(Symbol.class);
        when(symbol.getCode()).thenReturn(code);
        when(symbol.toString()).thenReturn(code);

        // Remove these lines - can't mock equals() and hashCode()
        // when(symbol.equals(any(Object.class)))...
        // when(symbol.hashCode())...

        return symbol;
    }

    private IBuyOrder createMockBuyOrder(String id, Symbol symbol, Money price,
                                         BigDecimal quantity, LocalDateTime time) {
        IBuyOrder order = mock(IBuyOrder.class);
        when(order.getId()).thenReturn(id);
        when(order.getSymbol()).thenReturn(symbol);
        when(order.getPrice()).thenReturn(price);
        when(order.getRemainingQuantity()).thenReturn(quantity);
        when(order.getCreatedAt()).thenReturn(time);
        when(order.isActive()).thenReturn(true);
        return order;
    }

    private ISellOrder createMockSellOrder(String id, Symbol symbol, Money price,
                                           BigDecimal quantity, LocalDateTime time) {
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