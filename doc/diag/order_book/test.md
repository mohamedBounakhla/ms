# Order Book Business Logic Test Strategy

## Testing Priority Order

### **Phase 1: Foundation Components (Test First)**
These are the building blocks that everything else depends on.

---

## 1. **Priority Calculators** 🔧
*Test these first - they're used everywhere*

### **BuyOrderPriorityCalculator**
```java
@Test
public void should_prioritize_higher_price_over_lower_price() {
    // Given: Two buy orders with different prices
    Money higherPrice = Money.of("100.50");
    Money lowerPrice = Money.of("100.00");
    
    // When: Comparing prices
    boolean result = calculator.isPriceBetter(higherPrice, lowerPrice);
    
    // Then: Higher price should be better
    assertTrue(result);
}

@Test
public void should_prioritize_earlier_time_when_same_price() {
    // Given: Two buy orders at same price, different times
    IBuyOrder earlierOrder = createBuyOrder(price, earlierTime);
    IBuyOrder laterOrder = createBuyOrder(price, laterTime);
    
    // When: Comparing priority
    boolean result = calculator.isHigherPriority(earlierOrder, laterOrder);
    
    // Then: Earlier order should have priority
    assertTrue(result);
}

@Test
public void should_handle_equal_price_and_time() {
    // Edge case: Identical orders
}

@Test
public void should_calculate_price_difference_correctly() {
    // Mathematical accuracy test
}
```

### **SellOrderPriorityCalculator**
```java
@Test
public void should_prioritize_lower_price_over_higher_price() {
    // Given: Two sell orders with different prices
    Money lowerPrice = Money.of("99.50");
    Money higherPrice = Money.of("100.00");
    
    // When: Comparing prices
    boolean result = calculator.isPriceBetter(lowerPrice, higherPrice);
    
    // Then: Lower price should be better
    assertTrue(result);
}
```

---

## 2. **Price Levels** 📊
*Test these second - they manage order collections*

### **BidPriceLevel**
```java
@Test
public void should_add_buy_order_successfully() {
    // Given: Empty bid level at price $100
    BidPriceLevel level = new BidPriceLevel(Money.of("100.00"));
    IBuyOrder order = createBuyOrder("100.00", "10");
    
    // When: Adding order
    level.addOrder(order);
    
    // Then: Level should contain order and update metrics
    assertEquals(1, level.getOrderCount());
    assertEquals(new BigDecimal("10"), level.getTotalQuantity());
    assertTrue(level.getFirstOrder().isPresent());
}

@Test
public void should_maintain_time_priority_within_level() {
    // Given: Bid level with first order
    BidPriceLevel level = new BidPriceLevel(Money.of("100.00"));
    IBuyOrder firstOrder = createBuyOrder("100.00", "10", earlierTime);
    IBuyOrder secondOrder = createBuyOrder("100.00", "5", laterTime);
    
    // When: Adding orders in sequence
    level.addOrder(firstOrder);
    level.addOrder(secondOrder);
    
    // Then: First order should be at front of queue
    assertEquals(firstOrder.getId(), level.getFirstOrder().get().getId());
    assertEquals(Arrays.asList(firstOrder, secondOrder), level.getOrders());
}

@Test
public void should_reject_order_with_wrong_price() {
    // Given: Bid level at $100
    BidPriceLevel level = new BidPriceLevel(Money.of("100.00"));
    IBuyOrder wrongPriceOrder = createBuyOrder("99.00", "10");
    
    // When/Then: Should throw exception
    assertThrows(IllegalArgumentException.class, () -> 
        level.addOrder(wrongPriceOrder));
}

@Test
public void should_remove_order_and_update_metrics() {
    // Given: Level with multiple orders
    // When: Removing specific order
    // Then: Metrics should be recalculated correctly
}

@Test
public void should_become_empty_when_all_orders_removed() {
    // Given: Level with orders
    // When: Removing all orders
    // Then: isEmpty() should return true
}
```

### **AskPriceLevel**
```java
// Mirror tests for sell orders with appropriate price logic
@Test
public void should_add_sell_order_successfully() { /* ... */ }

@Test
public void should_maintain_time_priority_within_level() { /* ... */ }
```

---

## 3. **Match Finding Logic** 🎯
*Critical business logic - test thoroughly*

### **MatchFinder**
```java
@Test
public void should_find_match_when_bid_meets_ask() {
    // Given: Buy order at $100, Sell order at $99
    OrderBook book = new OrderBook(symbol);
    IBuyOrder buyOrder = createBuyOrder("100.00", "10");
    ISellOrder sellOrder = createSellOrder("99.00", "5");
    
    book.addOrder(buyOrder);
    book.addOrder(sellOrder);
    
    // When: Finding matches
    List<OrderMatch> matches = book.findMatches();
    
    // Then: Should find one match
    assertEquals(1, matches.size());
    OrderMatch match = matches.get(0);
    assertEquals(buyOrder.getId(), match.getBuyOrder().getId());
    assertEquals(sellOrder.getId(), match.getSellOrder().getId());
}

@Test
public void should_not_find_match_when_bid_below_ask() {
    // Given: Buy order at $99, Sell order at $100
    OrderBook book = new OrderBook(symbol);
    IBuyOrder buyOrder = createBuyOrder("99.00", "10");
    ISellOrder sellOrder = createSellOrder("100.00", "5");
    
    book.addOrder(buyOrder);
    book.addOrder(sellOrder);
    
    // When: Finding matches
    List<OrderMatch> matches = book.findMatches();
    
    // Then: Should find no matches
    assertTrue(matches.isEmpty());
}

@Test
public void should_calculate_correct_match_quantity() {
    // Given: Buy 10 shares, Sell 5 shares
    IBuyOrder buyOrder = createBuyOrder("100.00", "10");
    ISellOrder sellOrder = createSellOrder("99.00", "5");
    
    // When: Creating match
    OrderMatch match = new OrderMatch(buyOrder, sellOrder);
    
    // Then: Should match minimum quantity
    assertEquals(new BigDecimal("5"), match.getMatchableQuantity());
}

@Test
public void should_calculate_midpoint_price() {
    // Given: Buy at $100, Sell at $98
    IBuyOrder buyOrder = createBuyOrder("100.00", "10");
    ISellOrder sellOrder = createSellOrder("98.00", "5");
    
    // When: Creating match
    OrderMatch match = new OrderMatch(buyOrder, sellOrder);
    
    // Then: Should use midpoint pricing
    assertEquals(Money.of("99.00"), match.getSuggestedPrice());
}

@Test
public void should_reject_incompatible_symbols() {
    // Given: Orders for different symbols
    IBuyOrder buyOrder = createBuyOrder("AAPL", "100.00", "10");
    ISellOrder sellOrder = createSellOrder("MSFT", "100.00", "10");
    
    // When/Then: Should throw exception
    assertThrows(IllegalArgumentException.class, () -> 
        new OrderMatch(buyOrder, sellOrder));
}

@Test
public void should_reject_inactive_orders() {
    // Given: One inactive order
    // When/Then: Should throw exception or return invalid match
}
```

---

## **Phase 2: Core Aggregate (Test Second)**

## 4. **OrderBook** 📚
*Main business aggregate - comprehensive testing needed*

### **Order Management**
```java
@Test
public void should_add_buy_order_to_correct_bid_level() {
    // Given: Empty order book
    OrderBook book = new OrderBook(symbol);
    IBuyOrder order = createBuyOrder("100.00", "10");
    
    // When: Adding buy order
    book.addOrder(order);
    
    // Then: Should create bid level and add order
    assertEquals(Money.of("100.00"), book.getBestBid().get());
    assertEquals(order.getId(), book.getBestBuyOrder().get().getId());
    assertEquals(new BigDecimal("10"), book.getTotalBidVolume());
}

@Test
public void should_add_sell_order_to_correct_ask_level() {
    // Given: Empty order book
    OrderBook book = new OrderBook(symbol);
    ISellOrder order = createSellOrder("100.00", "10");
    
    // When: Adding sell order
    book.addOrder(order);
    
    // Then: Should create ask level and add order
    assertEquals(Money.of("100.00"), book.getBestAsk().get());
    assertEquals(order.getId(), book.getBestSellOrder().get().getId());
    assertEquals(new BigDecimal("10"), book.getTotalAskVolume());
}

@Test
public void should_maintain_price_priority_across_levels() {
    // Given: Order book
    OrderBook book = new OrderBook(symbol);
    
    // When: Adding buy orders at different prices
    book.addOrder(createBuyOrder("99.00", "10"));  // Lower price
    book.addOrder(createBuyOrder("101.00", "5"));  // Higher price
    book.addOrder(createBuyOrder("100.00", "8"));  // Middle price
    
    // Then: Best bid should be highest price
    assertEquals(Money.of("101.00"), book.getBestBid().get());
    
    // And: Market depth should be price-ordered
    MarketDepth depth = book.getMarketDepth(3);
    List<BidPriceLevel> bids = depth.getBidLevels();
    assertEquals(Money.of("101.00"), bids.get(0).getPrice()); // Best
    assertEquals(Money.of("100.00"), bids.get(1).getPrice()); // Second
    assertEquals(Money.of("99.00"), bids.get(2).getPrice());  // Third
}

@Test
public void should_handle_multiple_orders_at_same_price() {
    // Given: Order book
    OrderBook book = new OrderBook(symbol);
    
    // When: Adding multiple orders at same price
    IBuyOrder first = createBuyOrder("100.00", "10", earlierTime);
    IBuyOrder second = createBuyOrder("100.00", "5", laterTime);
    book.addOrder(first);
    book.addOrder(second);
    
    // Then: Should aggregate volume and maintain time priority
    assertEquals(new BigDecimal("15"), book.getTotalBidVolume());
    assertEquals(first.getId(), book.getBestBuyOrder().get().getId());
}
```

### **Order Removal**
```java
@Test
public void should_remove_order_and_update_metrics() {
    // Given: Order book with orders
    OrderBook book = new OrderBook(symbol);
    IBuyOrder order = createBuyOrder("100.00", "10");
    book.addOrder(order);
    
    // When: Removing order
    boolean removed = book.removeOrder(order);
    
    // Then: Should remove successfully and update metrics
    assertTrue(removed);
    assertEquals(BigDecimal.ZERO, book.getTotalBidVolume());
    assertFalse(book.getBestBid().isPresent());
}

@Test
public void should_remove_empty_price_levels() {
    // Given: Price level with single order
    OrderBook book = new OrderBook(symbol);
    IBuyOrder order = createBuyOrder("100.00", "10");
    book.addOrder(order);
    
    // When: Removing the only order at that price
    book.removeOrder(order);
    
    // Then: Price level should be removed
    assertTrue(book.getBidLevels().isEmpty());
}

@Test
public void should_maintain_best_prices_after_removal() {
    // Given: Multiple price levels
    OrderBook book = new OrderBook(symbol);
    IBuyOrder bestOrder = createBuyOrder("101.00", "10");
    IBuyOrder secondOrder = createBuyOrder("100.00", "5");
    book.addOrder(bestOrder);
    book.addOrder(secondOrder);
    
    // When: Removing best order
    book.removeOrder(bestOrder);
    
    // Then: Second best should become best
    assertEquals(Money.of("100.00"), book.getBestBid().get());
}
```

### **Spread Calculation**
```java
@Test
public void should_calculate_spread_correctly() {
    // Given: Order book with bid and ask
    OrderBook book = new OrderBook(symbol);
    book.addOrder(createBuyOrder("99.00", "10"));   // Bid
    book.addOrder(createSellOrder("101.00", "5"));  // Ask
    
    // When: Getting spread
    Optional<Money> spread = book.getSpread();
    
    // Then: Should be ask - bid
    assertTrue(spread.isPresent());
    assertEquals(Money.of("2.00"), spread.get());
}

@Test
public void should_return_empty_spread_when_missing_side() {
    // Given: Order book with only bids
    OrderBook book = new OrderBook(symbol);
    book.addOrder(createBuyOrder("99.00", "10"));
    
    // When: Getting spread
    Optional<Money> spread = book.getSpread();
    
    // Then: Should be empty
    assertFalse(spread.isPresent());
}
```

### **Market Depth**
```java
@Test
public void should_return_correct_market_depth() {
    // Given: Order book with multiple levels
    OrderBook book = setupOrderBookWithMultipleLevels();
    
    // When: Getting market depth
    MarketDepth depth = book.getMarketDepth(2);
    
    // Then: Should return top 2 levels each side
    assertEquals(2, depth.getBidLevels().size());
    assertEquals(2, depth.getAskLevels().size());
    assertEquals(symbol, depth.getSymbol());
}

@Test
public void should_limit_depth_to_available_levels() {
    // Given: Order book with 1 level each side
    OrderBook book = new OrderBook(symbol);
    book.addOrder(createBuyOrder("99.00", "10"));
    book.addOrder(createSellOrder("101.00", "5"));
    
    // When: Requesting 5 levels
    MarketDepth depth = book.getMarketDepth(5);
    
    // Then: Should return only available levels
    assertEquals(1, depth.getBidLevels().size());
    assertEquals(1, depth.getAskLevels().size());
}
```

### **Validation**
```java
@Test
public void should_reject_order_with_wrong_symbol() {
    // Given: Order book for AAPL
    OrderBook book = new OrderBook(Symbol.of("AAPL"));
    IBuyOrder msftOrder = createBuyOrder("MSFT", "100.00", "10");
    
    // When/Then: Should throw exception
    assertThrows(IllegalArgumentException.class, () -> 
        book.addOrder(msftOrder));
}

@Test
public void should_reject_inactive_order() {
    // Given: Inactive order
    // When/Then: Should throw exception
}

@Test
public void should_reject_duplicate_order() {
    // Given: Order already in book
    // When/Then: Should throw exception
}
```

---

## **Phase 3: Manager & Integration (Test Last)**

## 5. **OrderBookManager** 🏢
*High-level coordination - integration tests*

### **Order Book Management**
```java
@Test
public void should_create_order_book_for_new_symbol() {
    // Given: Empty manager
    OrderBookManager manager = new OrderBookManager();
    Symbol symbol = Symbol.of("AAPL");
    
    // When: Getting order book for new symbol
    OrderBook book = manager.getOrderBook(symbol);
    
    // Then: Should create and return new book
    assertNotNull(book);
    assertEquals(symbol, book.getSymbol());
    assertEquals(1, manager.getTotalOrderBooks());
}

@Test
public void should_return_existing_order_book() {
    // Given: Manager with existing book
    OrderBookManager manager = new OrderBookManager();
    Symbol symbol = Symbol.of("AAPL");
    OrderBook firstCall = manager.getOrderBook(symbol);
    
    // When: Getting same symbol again
    OrderBook secondCall = manager.getOrderBook(symbol);
    
    // Then: Should return same instance
    assertSame(firstCall, secondCall);
    assertEquals(1, manager.getTotalOrderBooks());
}

@Test
public void should_add_order_to_correct_book() {
    // Given: Manager and order
    OrderBookManager manager = new OrderBookManager();
    IBuyOrder order = createBuyOrder("AAPL", "100.00", "10");
    
    // When: Adding order
    manager.addOrderToBook(order);
    
    // Then: Should be in correct book
    OrderBook book = manager.getOrderBook(Symbol.of("AAPL"));
    assertEquals(1, book.getOrderCount());
    assertEquals(order.getId(), book.getBestBuyOrder().get().getId());
}
```

### **Cross-Book Operations**
```java
@Test
public void should_find_matches_across_all_books() {
    // Given: Multiple books with matchable orders
    OrderBookManager manager = new OrderBookManager();
    
    // Setup AAPL book with potential match
    manager.addOrderToBook(createBuyOrder("AAPL", "100.00", "10"));
    manager.addOrderToBook(createSellOrder("AAPL", "99.00", "5"));
    
    // Setup MSFT book with no match
    manager.addOrderToBook(createBuyOrder("MSFT", "50.00", "10"));
    manager.addOrderToBook(createSellOrder("MSFT", "51.00", "5"));
    
    // When: Finding all matches
    List<OrderMatch> matches = manager.findAllMatches();
    
    // Then: Should find matches from all books
    assertEquals(1, matches.size()); // Only AAPL should match
    assertEquals("AAPL", matches.get(0).getBuyOrder().getSymbol().getValue());
}

@Test
public void should_generate_market_overview() {
    // Given: Manager with multiple books
    OrderBookManager manager = setupManagerWithMultipleBooks();
    
    // When: Getting market overview
    MarketOverview overview = manager.getMarketOverview();
    
    // Then: Should aggregate all data
    assertEquals(2, overview.getTotalOrderBooks());
    assertTrue(overview.getActiveSymbols().contains(Symbol.of("AAPL")));
    assertTrue(overview.getActiveSymbols().contains(Symbol.of("MSFT")));
    assertTrue(overview.getTotalOrders() > 0);
}
```

---

## **Testing Utilities & Helpers**

### **Test Data Builders**
```java
public class OrderTestBuilder {
    public static IBuyOrder createBuyOrder(String price, String quantity) {
        return createBuyOrder("AAPL", price, quantity, LocalDateTime.now());
    }
    
    public static IBuyOrder createBuyOrder(String symbol, String price, 
                                          String quantity, LocalDateTime time) {
        // Implementation with proper mocking
    }
    
    public static ISellOrder createSellOrder(String price, String quantity) {
        // Similar implementation
    }
}

public class OrderBookTestFixtures {
    public static OrderBook createBookWithDepth() {
        OrderBook book = new OrderBook(Symbol.of("AAPL"));
        
        // Add multiple bid levels
        book.addOrder(createBuyOrder("101.00", "100"));
        book.addOrder(createBuyOrder("100.50", "200"));
        book.addOrder(createBuyOrder("100.00", "150"));
        
        // Add multiple ask levels
        book.addOrder(createSellOrder("102.00", "80"));
        book.addOrder(createSellOrder("102.50", "120"));
        book.addOrder(createSellOrder("103.00", "90"));
        
        return book;
    }
}
```

## **Test Execution Order Summary**

1. **Phase 1**: Priority Calculators → Price Levels → Match Finding
2. **Phase 2**: OrderBook (all scenarios)
3. **Phase 3**: OrderBookManager (integration)

**Critical Path**: Priority Calculators → Price Levels → OrderBook → Manager

Each phase builds on the previous, ensuring solid foundations before testing complex interactions.