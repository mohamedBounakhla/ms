🎯 Order Domain Rules (What Order Domain DOES)
Individual Order Validation:

✅ Order ID: Cannot be null/empty, must be unique
✅ Symbol: Cannot be null, must be valid symbol
✅ Price: Cannot be null, must be positive, currency compatibility with symbol
✅ Quantity: Cannot be null, must be positive
✅ Order Status: Valid state transitions (PENDING → PARTIAL → FILLED/CANCELLED)
✅ Timestamps: Valid creation/update times
✅ Execution Tracking: Executed quantity vs remaining quantity consistency

Transaction Creation Validation:

✅ Order Compatibility: Both orders have same symbol
✅ Price Matching: Buy price >= Sell price (can match)
✅ Order Status: Both orders must be active (not FILLED/CANCELLED)
✅ Quantity Constraints: Transaction quantity <= remaining quantity of both orders
✅ Business Logic: Update order execution after transaction

What Order Domain Should NOT Do:

❌ Market Structure: Order placement in specific price levels
❌ Book Organization: Bid/ask side placement logic
❌ Price-Time Priority: Order sequencing within price levels
❌ Market State: Cross-market validation, spread management
❌ Book Consistency: Ensuring orders don't duplicate across books

