package core.ms.order_book;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order_book.domain.entities.OrderBook;
import core.ms.order_book.domain.entities.OrderBookManager;
import core.ms.order_book.domain.value_object.MarketOverview;
import core.ms.order_book.domain.value_object.OrderMatch;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Order Book Manager - Business Logic Tests")
class OrderBookManagerBusinessTest {

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
    @DisplayName("Order Book Lifecycle Management")
    class OrderBookLifecycleTests {

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
        @DisplayName("Should return existing order book for same symbol")
        void should_return_existing_order_book_for_same_symbol() {
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

            // Then: Should create new book and be retrievable
            assertNotNull(orderBook);
            assertEquals(btcSymbol, orderBook.getSymbol());
            assertEquals(1, manager.getTotalOrderBooks());

            OrderBook retrieved = manager.getOrderBook(btcSymbol);
            assertSame(orderBook, retrieved);
        }

        @Test
        @DisplayName("Should reject creating duplicate order book")
        void should_reject_creating_duplicate_order_book() {
            // Given: Manager with existing book
            manager.createOrderBook(btcSymbol);

            // When/Then: Should throw exception for duplicate
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> manager.createOrderBook(btcSymbol));
            assertTrue(exception.getMessage().contains("already exists"));
        }

        @Test
        @DisplayName("Should remove order book successfully")
        void should_remove_order_book_successfully() {
            // Given: Manager with multiple order books
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
        @DisplayName("Should handle null symbol validation")
        void should_handle_null_symbol_validation() {
            // When/Then: Should throw exception for null symbol
            assertThrows(NullPointerException.class, () -> manager.getOrderBook(null));
            assertThrows(NullPointerException.class, () -> manager.createOrderBook(null));
            assertThrows(NullPointerException.class, () -> manager.removeOrderBook(null));
        }
    }

    @Nested
    @DisplayName("Order Routing and Management")
    class OrderRoutingTests {

        @Test
        @DisplayName("Should route order to correct symbol book")
        void should_route_order_to_correct_symbol_book() {
            // When: Adding order
            manager.addOrderToBook(btcBuyOrder);

            // Then: Should be in correct book only
            OrderBook btcBook = manager.getOrderBook(btcSymbol);
            assertEquals(1, btcBook.getOrderCount());
            assertEquals(btcBuyOrder.getId(), btcBook.getBestBuyOrder().get().getId());

            // And: Other books should remain empty
            OrderBook ethBook = manager.getOrderBook(ethSymbol);
            assertTrue(ethBook.isEmpty());
        }

        @Test
        @DisplayName("Should manage orders across multiple symbol books")
        void should_manage_orders_across_multiple_symbol_books() {
            // When: Adding orders to different books
            manager.addOrderToBook(btcBuyOrder);
            manager.addOrderToBook(ethBuyOrder);
            manager.addOrderToBook(aaplBuyOrder);

            // Then: Each book should contain its respective order
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
        @DisplayName("Should remove order from specified symbol book")
        void should_remove_order_from_specified_symbol_book() {
            // Given: Orders in different books
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
        @DisplayName("Should return false when removing order from incorrect book")
        void should_return_false_when_removing_order_from_incorrect_book() {
            // Given: Order in BTC book only
            manager.addOrderToBook(btcBuyOrder);

            // When: Trying to remove from ETH book
            boolean removed = manager.removeOrderFromBook(btcBuyOrder, ethSymbol);

            // Then: Should return false
            assertFalse(removed);

            // And: Order should remain in BTC book
            OrderBook btcBook = manager.getOrderBook(btcSymbol);
            assertEquals(1, btcBook.getOrderCount());
        }

        @Test
        @DisplayName("Should return false when removing from non-existent book")
        void should_return_false_when_removing_from_non_existent_book() {
            // When: Removing order from non-existent book
            boolean removed = manager.removeOrderFromBook(btcBuyOrder, btcSymbol);

            // Then: Should return false
            assertFalse(removed);
            assertEquals(0, manager.getTotalOrderBooks());
        }
    }

    @Nested
    @DisplayName("Cross-Book Market Operations")
    class CrossBookMarketOperationsTests {

        @Test
        @DisplayName("Should find matches across all eligible books")
        void should_find_matches_across_all_eligible_books() {
            // Given: Multiple books with varying match eligibility
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

            // Then: Should find matches from eligible books only
            assertEquals(2, matches.size());

            // Verify BTC match exists
            boolean hasBtcMatch = matches.stream()
                    .anyMatch(m -> m.getBuyOrder().getId().equals("btc-buy-1") &&
                            m.getSellOrder().getId().equals("btc-sell-1"));
            assertTrue(hasBtcMatch);

            // Verify AAPL match exists
            boolean hasAaplMatch = matches.stream()
                    .anyMatch(m -> m.getBuyOrder().getId().equals("aapl-buy-1") &&
                            m.getSellOrder().getId().equals("aapl-sell-1"));
            assertTrue(hasAaplMatch);

            // Verify no ETH match (price spread prevents matching)
            boolean hasEthMatch = matches.stream()
                    .anyMatch(m -> m.getBuyOrder().getSymbol().equals(ethSymbol));
            assertFalse(hasEthMatch);
        }

        @Test
        @DisplayName("Should return empty list when no matches exist")
        void should_return_empty_list_when_no_matches_exist() {
            // Given: Books with non-matching orders
            manager.addOrderToBook(ethBuyOrder);  // $3,000
            manager.addOrderToBook(ethSellOrder); // $3,100 (spread prevents match)

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
        @DisplayName("Should provide comprehensive market overview")
        void should_provide_comprehensive_market_overview() {
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

            // Verify volume calculations per symbol
            Map<Symbol, BigDecimal> volumes = overview.getTotalVolume();
            assertEquals(new BigDecimal("2.3"), volumes.get(btcSymbol));  // 1.5 + 0.8
            assertEquals(new BigDecimal("8.0"), volumes.get(ethSymbol));  // 5.0 + 3.0
            assertEquals(new BigDecimal("250"), volumes.get(aaplSymbol)); // 100 + 150

            assertNotNull(overview.getTimestamp());
        }

        @Test
        @DisplayName("Should return correct volume for specific symbols")
        void should_return_correct_volume_for_specific_symbols() {
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
        @DisplayName("Should return all managed order books")
        void should_return_all_managed_order_books() {
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

    // Helper methods
    private void setupManagerWithMultipleBooks() {
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