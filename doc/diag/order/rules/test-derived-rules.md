# Test-Derived Business Rules

## Overview
This document captures specific business rules and behaviors as evidenced by the comprehensive test suite. These rules complement the main business rules document with concrete examples and edge cases.

## 1. Transaction Order State Updates (TransactionOrderStateUpdatesTest)

### Rule 1.1: Full Quantity Execution
```java
// Test: shouldUpdateOrdersToFilledWhenTransactionQuantityEqualsOrderQuantity
Given: Order with quantity 1.0 BTC, status PENDING
When: Transaction executes exactly 1.0 BTC
Then: Order status becomes FILLED
```

### Rule 1.2: Partial Quantity Execution
```java
// Test: shouldUpdateOrdersToPartialWhenTransactionQuantityIsLessThanOrderQuantity
Given: Order with quantity 2.0 BTC, status PENDING
When: Transaction executes 0.5 BTC
Then: Order status becomes PARTIAL
```

### Rule 1.3: Progressive State Transitions
```java
// Test: shouldTrackOrderStateTransitionsThroughMultipleTransactions
Given: Order with quantity 3.0 BTC, status PENDING
When: Transaction 1 executes 1.0 BTC → Status becomes PARTIAL
When: Transaction 2 executes 1.0 BTC → Status remains PARTIAL
When: Transaction 3 executes 1.0 BTC → Status becomes FILLED
```

## 2. Quantity Tracking (QuantityTrackingTest)

### Rule 2.1: Executed Quantity Accumulation
```java
// Test: shouldAccumulateExecutedQuantityAcrossMultipleTransactions
Given: Order with quantity 3.0 BTC
When: Transaction 1 executes 1.0 BTC, Transaction 2 executes 0.5 BTC
Then: Executed quantity = 1.5 BTC, Remaining quantity = 1.5 BTC
```

### Rule 2.2: Remaining Quantity Calculation
```java
// Test: shouldTrackRemainingQuantityAfterTransaction
Given: Order with quantity 2.0 BTC
When: Transaction executes 0.5 BTC
Then: Remaining quantity = 1.5 BTC
```

### Rule 2.3: Zero Remaining Quantity
```java
// Test: shouldHaveZeroRemainingQuantityWhenOrderIsFullyExecuted
Given: Order with quantity 1.0 BTC
When: Transaction executes 1.0 BTC
Then: Remaining quantity = 0, Executed quantity = 1.0 BTC
```

## 3. Multiple Transaction Support (MultipleTransactionSupportTest)

### Rule 3.1: Sequential Transaction Processing
```java
// Test: shouldSupportMultiplePartialTransactionsOnSameOrders
Given: Orders with quantity 5.0 BTC each
When: Transaction 1 executes 1.0 BTC → Status: PARTIAL
When: Transaction 2 executes 2.0 BTC → Status: PARTIAL
When: Transaction 3 executes 2.0 BTC → Status: FILLED
```

### Rule 3.2: Transaction History Tracking
```java
// Test: shouldTrackAllTransactionsAssociatedWithAnOrder
Given: Order with quantity 3.0 BTC
When: 3 transactions execute
Then: Order.getTransactions() returns list of 3 transactions
```

### Rule 3.3: Insufficient Quantity Prevention
```java
// Test: shouldPreventTransactionWhenRemainingQuantityIsInsufficient
Given: Order with quantity 2.0 BTC, 1.5 BTC already executed
When: Attempt to execute 1.0 BTC (exceeds remaining 0.5 BTC)
Then: IllegalArgumentException is thrown
```

## 4. Order Matching Rules (OrderMatchingRulesTest)

### Rule 4.1: Price Overlap Requirement
```java
// Test: shouldAllowMatchingWhenBuyPriceIsGreaterThanOrEqualToSellPrice
Given: Buy order at $45,000, Sell order at $44,000
When: Creating transaction with execution price $44,500
Then: Transaction creation succeeds
```

### Rule 4.2: No Price Overlap Prevention
```java
// Test: shouldPreventMatchingWhenBuyPriceIsLessThanSellPrice
Given: Buy order at $44,000, Sell order at $45,000
When: Attempting to create transaction
Then: IllegalArgumentException is thrown
```

### Rule 4.3: Execution Price Range Validation
```java
// Test: shouldValidateExecutionPriceIsWithinBuySellPriceRange
Given: Buy order at $46,000, Sell order at $44,000
Valid execution prices: $44,000 (sell price), $45,000 (mid-point), $46,000 (buy price)
Invalid execution prices: $43,000 (below sell), $47,000 (above buy)
```

### Rule 4.4: Optimal Price Discovery
```java
// Test: shouldDetermineOptimalExecutionPriceForMatchingOrders
Given: Buy order at $46,000, Sell order at $44,000
When: Transaction.determineExecutionPrice() is called
Then: Returns $45,000 (mid-point)
```

## 5. Transaction Ordering & Timestamps (TransactionOrderingTest)

### Rule 5.1: Timestamp Assignment
```java
// Test: shouldCreateTransactionsWithProperTimestamps
Given: Transaction creation time window
When: Transaction is created
Then: Transaction.getCreatedAt() is within the time window
```

### Rule 5.2: Chronological Ordering
```java
// Test: shouldMaintainTransactionSequenceOrder
Given: Multiple transactions created sequentially
When: Transactions are created with delays
Then: Transaction timestamps are in chronological order
```

### Rule 5.3: Sequence Tracking
```java
// Test: shouldTrackTransactionExecutionSequenceWithinOrder
Given: Order with 3 transactions
When: Transactions execute in order
Then: getTransactionSequence() returns 1, 2, 3 respectively
```

### Rule 5.4: Concurrent Transaction Prevention
```java
// Test: shouldPreventConcurrentTransactionCreationOnSameOrder
Given: Order with 1.0 BTC, first transaction executes 0.8 BTC
When: Second transaction attempts to execute 0.5 BTC (exceeds remaining 0.2 BTC)
Then: IllegalArgumentException is thrown
```

## 6. Transaction Domain Rules (TransactionTest)

### Rule 6.1: Active Orders Requirement
```java
// Test: shouldNotCreateTransactionWithCancelledBuyOrder
Given: Cancelled buy order
When: Attempting to create transaction
Then: IllegalArgumentException is thrown
```

### Rule 6.2: Symbol Consistency
```java
// Test: shouldValidateBuyAndSellOrdersHaveSameSymbol
Given: Buy order with BTC/USD, Sell order with ETH/USD
When: Attempting to create transaction
Then: IllegalArgumentException is thrown
```

### Rule 6.3: Currency Validation
```java
// Test: shouldValidatePriceCurrencyMatchesSymbolQuoteCurrency
Given: BTC/EUR symbol, USD-denominated price
When: Attempting to create transaction
Then: IllegalArgumentException is thrown
```

### Rule 6.4: Quantity Constraints
```java
// Test: shouldValidateMinimumTransactionQuantity
Given: Zero or negative quantity
When: Attempting to create transaction
Then: IllegalArgumentException is thrown
```

### Rule 6.5: Order State Transitions
```java
// Test: shouldTransitionPendingOrdersToFilledWhenFullyExecuted
Given: PENDING orders with exact quantity match
When: Transaction executes full quantity
Then: Both orders transition to FILLED status
```

### Rule 6.6: Mixed Execution Scenarios
```java
// Test: shouldHandleOneOrderFullyExecutedOtherPartiallyExecuted
Given: Buy order 1.0 BTC, Sell order 2.0 BTC
When: Transaction executes 1.0 BTC
Then: Buy order → FILLED, Sell order → PARTIAL
```

## 7. Order Domain Integration (OrderDomainIntegrationTest)

### Rule 7.1: Complete Order Lifecycle
```java
// Test: shouldHandleCompleteOrderLifecycle
Given: Orders with matching prices and quantities
When: Transaction is created
Then: Orders automatically transition to FILLED status
```

### Rule 7.2: Order Modification Rules
```java
// Test: shouldHandleOrderCancellationScenario
Given: Active order
When: Price update is performed → Succeeds
When: Order is cancelled → Status becomes CANCELLED
When: Price update is attempted → IllegalStateException is thrown
```

## 8. Edge Cases and Error Conditions

### Rule 8.1: Null Parameter Validation
- All domain objects reject null parameters with NullPointerException
- Error messages provide clear indication of which parameter was null

### Rule 8.2: Business Rule Violation Errors
- Price mismatches throw IllegalArgumentException
- Quantity constraint violations throw IllegalArgumentException
- State transition violations throw IllegalStateException

### Rule 8.3: Precision Handling
- Monetary calculations maintain appropriate precision
- BigDecimal arithmetic is used for exact calculations
- Currency-specific rounding rules are applied

## 9. State Machine Behavior

### Rule 9.1: Valid State Transitions
```
PENDING → PARTIAL → FILLED
PENDING → FILLED
PENDING → CANCELLED
PARTIAL → CANCELLED
PARTIAL → FILLED
```

### Rule 9.2: Invalid State Transitions
```
FILLED → * (terminal state)
CANCELLED → * (terminal state)
```

### Rule 9.3: Automatic State Management
- Orders automatically transition states based on transaction execution
- No manual state transitions are required
- State consistency is maintained across all operations

## Summary

The test suite demonstrates comprehensive coverage of business rules with:
- 45+ test scenarios covering all major business rules
- Edge case handling for error conditions
- State transition validation
- Quantity tracking and accumulation
- Multi-transaction support
- Order matching validation
- Temporal consistency and ordering

These test-derived rules provide concrete examples of expected system behavior and serve as executable specifications for the business requirements.