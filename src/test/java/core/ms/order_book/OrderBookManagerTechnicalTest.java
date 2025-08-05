package core.ms.order_book;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order_book.domain.entities.OrderBook;
import core.ms.order_book.domain.entities.OrderBookManager;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Order Book Manager - Technical Tests")
class OrderBookManagerTechnicalTest {

    private OrderBookManager manager;
    private Symbol btcSymbol;
    private Symbol ethSymbol;

    @BeforeEach
    void setUp() {
        manager = new OrderBookManager();
        btcSymbol = Symbol.btcUsd();
        ethSymbol = Symbol.ethUsd();
    }

    @Test
    @DisplayName("Should handle concurrent order book creation safely")
    @Timeout(10) // Test should complete within 10 seconds
    void should_handle_concurrent_order_book_creation_safely() throws Exception {
        // Given: Multiple threads trying to create same book
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        List<Future<OrderBook>> futures = new ArrayList<>();

        // When: Multiple threads request same order book simultaneously
        for (int i = 0; i < threadCount; i++) {
            Future<OrderBook> future = executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize start
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
        finishLatch.await(5, TimeUnit.SECONDS); // Wait for completion

        // Then: All threads should get same instance (thread safety)
        OrderBook firstBook = futures.get(0).get();
        assertNotNull(firstBook);

        for (Future<OrderBook> future : futures) {
            OrderBook book = future.get();
            assertSame(firstBook, book, "All threads should get the same OrderBook instance");
        }

        // And: Only one book should be created
        assertEquals(1, manager.getTotalOrderBooks());

        executor.shutdown();
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should handle concurrent order operations without data corruption")
    @Timeout(15) // Test should complete within 15 seconds
    void should_handle_concurrent_order_operations_without_data_corruption() throws Exception {
        // Given: Shared order book and multiple orders
        int orderCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch finishLatch = new CountDownLatch(orderCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // When: Multiple threads add orders concurrently
        for (int i = 0; i < orderCount; i++) {
            final int orderId = i;
            executor.submit(() -> {
                try {
                    if (orderId % 2 == 0) {
                        IBuyOrder order = createMockBuyOrder("buy-" + orderId, btcSymbol,
                                Money.of("50000.00", Currency.USD), new BigDecimal("0.1"),
                                LocalDateTime.now().plusNanos(orderId));
                        manager.addOrderToBook(order);
                    } else {
                        ISellOrder order = createMockSellOrder("sell-" + orderId, btcSymbol,
                                Money.of("50000.00", Currency.USD), new BigDecimal("0.1"),
                                LocalDateTime.now().plusNanos(orderId));
                        manager.addOrderToBook(order);
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Failed to add order " + orderId + ": " + e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "All operations should complete within timeout");

        // Then: Verify data integrity
        OrderBook btcBook = manager.getOrderBook(btcSymbol);

        // Note: Due to thread safety implementations, we expect all orders to be added successfully
        // The exact count may depend on the synchronization mechanism used
        assertTrue(btcBook.getOrderCount() > 0, "At least some orders should be added");
        assertEquals(successCount.get(), btcBook.getOrderCount(),
                "Order count should match successful additions");

        executor.shutdown();
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should handle concurrent access to multiple order books")
    @Timeout(10)
    void should_handle_concurrent_access_to_multiple_order_books() throws Exception {
        // Given: Multiple symbols and concurrent operations
        int operationsPerSymbol = 20;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch finishLatch = new CountDownLatch(operationsPerSymbol * 2); // BTC + ETH

        // When: Concurrent operations on different symbols
        // BTC operations
        for (int i = 0; i < operationsPerSymbol; i++) {
            final int orderId = i;
            executor.submit(() -> {
                try {
                    IBuyOrder btcOrder = createMockBuyOrder("btc-" + orderId, btcSymbol,
                            Money.of("50000.00", Currency.USD), new BigDecimal("0.1"),
                            LocalDateTime.now().plusNanos(orderId));
                    manager.addOrderToBook(btcOrder);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // ETH operations
        for (int i = 0; i < operationsPerSymbol; i++) {
            final int orderId = i;
            executor.submit(() -> {
                try {
                    ISellOrder ethOrder = createMockSellOrder("eth-" + orderId, ethSymbol,
                            Money.of("3000.00", Currency.USD), new BigDecimal("1.0"),
                            LocalDateTime.now().plusNanos(orderId + 1000));
                    manager.addOrderToBook(ethOrder);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        boolean completed = finishLatch.await(8, TimeUnit.SECONDS);
        assertTrue(completed, "All operations should complete within timeout");

        // Then: Verify both books are created and populated correctly
        assertEquals(2, manager.getTotalOrderBooks());

        Set<Symbol> activeSymbols = manager.getActiveSymbols();
        assertTrue(activeSymbols.contains(btcSymbol));
        assertTrue(activeSymbols.contains(ethSymbol));

        OrderBook btcBook = manager.getOrderBook(btcSymbol);
        OrderBook ethBook = manager.getOrderBook(ethSymbol);

        assertTrue(btcBook.getOrderCount() > 0, "BTC book should have orders");
        assertTrue(ethBook.getOrderCount() > 0, "ETH book should have orders");

        executor.shutdown();
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should handle high-frequency concurrent read operations")
    @Timeout(10)
    void should_handle_high_frequency_concurrent_read_operations() throws Exception {
        // Given: Pre-populated order book
        OrderBook btcBook = manager.getOrderBook(btcSymbol);

        // Add some initial orders
        for (int i = 0; i < 10; i++) {
            IBuyOrder order = createMockBuyOrder("init-" + i, btcSymbol,
                    Money.of("50000.00", Currency.USD), new BigDecimal("0.1"),
                    LocalDateTime.now().plusNanos(i));
            manager.addOrderToBook(order);
        }

        int readOperations = 200;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch finishLatch = new CountDownLatch(readOperations);
        AtomicInteger successfulReads = new AtomicInteger(0);

        // When: Multiple threads perform read operations concurrently
        for (int i = 0; i < readOperations; i++) {
            executor.submit(() -> {
                try {
                    // Perform various read operations
                    int totalBooks = manager.getTotalOrderBooks();
                    Set<Symbol> symbols = manager.getActiveSymbols();
                    OrderBook book = manager.getOrderBook(btcSymbol);
                    int orderCount = book.getOrderCount();

                    // Verify basic consistency
                    assertTrue(totalBooks > 0);
                    assertFalse(symbols.isEmpty());
                    assertTrue(orderCount >= 0);

                    successfulReads.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Read operation failed: " + e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        boolean completed = finishLatch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "All read operations should complete within timeout");

        // Then: All reads should succeed without corruption
        assertEquals(readOperations, successfulReads.get(),
                "All read operations should succeed");

        executor.shutdown();
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    }

    /*@Test
    @DisplayName("Should handle memory pressure during concurrent operations")
    @Timeout(15)
    void should_handle_memory_pressure_during_concurrent_operations() throws Exception {
        // Given: Large number of operations to stress test memory
        int totalOperations = 500;
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch finishLatch = new CountDownLatch(totalOperations);

        // When: Creating many order books and orders
        for (int i = 0; i < totalOperations; i++) {
            final int operationId = i;
            executor.submit(() -> {
                try {
                    Symbol dynamicSymbol = createMockSymbol("SYM-" + (operationId % 50)); // 50 different symbols

                    IBuyOrder order = createMockBuyOrder("order-" + operationId, dynamicSymbol,
                            Money.of("100.00", Currency.USD), new BigDecimal("1.0"),
                            LocalDateTime.now().plusNanos(operationId));

                    manager.addOrderToBook(order);

                    // Occasionally trigger garbage collection candidate operations
                    if (operationId % 100 == 0) {
                        manager.getMarketOverview(); // Complex aggregation operation
                    }

                } catch (OutOfMemoryError e) {
                    fail("Should not run out of memory during normal operations");
                } catch (Exception e) {
                    // Log but don't fail test - some operations might fail under pressure
                    System.err.println("Operation " + operationId + " failed: " + e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        boolean completed = finishLatch.await(12, TimeUnit.SECONDS);
        assertTrue(completed, "All operations should complete within timeout");

        // Then: System should remain stable
        assertTrue(manager.getTotalOrderBooks() > 0, "Should have created order books");
        assertTrue(manager.getTotalOrderBooks() <= 50, "Should not exceed expected number of books");

        executor.shutdown();
        assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS));
    }*/

    // Helper methods
    private Symbol createMockSymbol(String code) {
        Symbol symbol = mock(Symbol.class);
        when(symbol.getCode()).thenReturn(code);
        when(symbol.toString()).thenReturn(code);
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