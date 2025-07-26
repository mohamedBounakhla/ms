📚 OrderBook Domain Rules (What OrderBook Domain SHOULD DO)
1. Order Book Structural Integrity:

✅ Symbol Consistency: All orders in book must match book's symbol
✅ Order Uniqueness: Same order cannot exist twice in same book
✅ Type Placement: Buy orders only in bid levels, sell orders only in ask levels
✅ Price Level Organization: Proper sorting (highest bid first, lowest ask first)
✅ Active Orders Only: Only active orders can be added to book

2. Price Level Management:

✅ Price Matching: Orders added to level must match level's exact price
✅ Time Priority: First-in-first-out within same price level
✅ Level Creation/Removal: Create levels when first order added, remove when empty
✅ Volume Aggregation: Correct total quantity calculation per level

3. Market Structure Rules:

✅ Spread Management: Ensure proper bid-ask spread structure
✅ Cross Prevention: Prevent invalid market states (bid > ask permanently)
✅ Book State Consistency: Maintain valid market depth structure
✅ Order Sequencing: Proper order queue management within levels

4. Matching Logic:

✅ Match Eligibility: Orders can match based on book state and priority
✅ Price-Time Priority: Respect market rules for order matching sequence
✅ Match Validation: Ensure matches respect book's current state
✅ Cross-Market Logic: Prevent invalid cross-market situations

5. Book Operations:

✅ Order Addition: Can this order be placed in this book at this time?
✅ Order Removal: Maintain book integrity after order removal
✅ Book Cleanup: Remove inactive orders, maintain clean state
✅ Volume Tracking: Accurate bid/ask volume calculations

What OrderBook Domain Should NOT Do:

❌ Order Creation: Don't create or validate individual order objects
❌ Order Status Management: Don't change order status (PENDING→FILLED, etc.)
❌ Transaction Creation: Don't create transaction objects
❌ Individual Order Rules: Don't validate price positivity, quantity rules, etc.
❌ Order Execution: Don't update order execution quantities


🔄 OrderBookManager Domain Rules
Multi-Book Management:

✅ Book Creation: Ensure unique books per symbol
✅ Symbol Routing: Route orders to correct book based on symbol
✅ Cross-Book Operations: Manage operations across multiple books
✅ Market Overview: Aggregate statistics across all books

What OrderBookManager Should NOT Do:

❌ Individual Book Logic: Don't duplicate OrderBook internal rules
❌ Order Validation: Don't validate individual orders
❌ Price Level Management: Don't manage internal book structure


📋 Validation Builder Focus Areas
OrderBook Validation Builders Should Focus On:
1. OrderBookCreationBuilder:
   javaOrderBookBuilder.builder()
   .withSymbol(symbol)                    // Symbol cannot be null
   .withInitialState()                    // Must start empty and consistent
   .build()
2. OrderAdditionBuilder:
   javaOrderAdditionBuilder.builder()
   .withOrderBook(orderBook)              // Book cannot be null
   .withOrder(order)                      // Order cannot be null (but don't validate order content)
   .validateSymbolCompatibility()         // Order symbol == Book symbol
   .validateOrderUniqueness()             // Order not already in book
   .validateOrderActivity()               // Order must be active
   .validateTypePlacement()               // Buy→Bid, Sell→Ask
   .build()
3. OrderMatchBuilder:
   javaOrderMatchBuilder.builder()
   .withBuyOrder(buyOrder)                // Must be IBuyOrder
   .withSellOrder(sellOrder)              // Must be ISellOrder
   .validateBookCompatibility()           // Both from same book context
   .validateMatchEligibility()            // Can match based on book state
   .validatePriceTimePriority()           // Respect market priority rules
   .build()
4. PriceLevelBuilder:
   javaPriceLevelBuilder.builder()
   .withPrice(price)                      // Price cannot be null
   .withOrderType(orderType)              // Buy or Sell
   .validatePriceValidity()               // Price valid for this level type
   .build()
   Key Principle:
   OrderBook domain assumes orders are already valid business objects and focuses on market structure and trading system coherence.