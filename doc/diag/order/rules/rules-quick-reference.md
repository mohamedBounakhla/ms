# Business Rules Quick Reference

## Order Creation Rules ✅
- ✅ Orders must have unique, non-null ID
- ✅ Quantity must be positive (> 0)
- ✅ Price currency must match symbol quote currency
- ✅ Orders start in PENDING status

## Order State Transitions 🔄
```
PENDING → PARTIAL → FILLED ✅
PENDING → FILLED ✅
PENDING → CANCELLED ✅
PARTIAL → CANCELLED ✅
PARTIAL → FILLED ✅

❌ FILLED → * (terminal)
❌ CANCELLED → * (terminal)
```

## Transaction Matching Rules 🔗
- ✅ Buy price >= Sell price (must overlap)
- ✅ Both orders must be active (not FILLED/CANCELLED)
- ✅ Orders must have same symbol
- ✅ Execution price: sell_price ≤ exec_price ≤ buy_price
- ✅ Optimal pricing: (buy_price + sell_price) / 2

## Quantity Management 📊
- ✅ Track executed quantity (cumulative)
- ✅ Track remaining quantity (original - executed)
- ✅ Transaction quantity ≤ min(buy_remaining, sell_remaining)
- ✅ Multiple transactions allowed until fully executed
- ✅ Auto-transition: remaining = 0 → FILLED

## Order Modifications 📝
- ✅ Price updates only if not terminal state
- ✅ Cancellation only if not already terminal
- ✅ Automatic timestamp updates
- ❌ No modifications to FILLED/CANCELLED orders

## Data Integrity 🛡️
- ✅ Null safety with clear error messages
- ✅ Immutable value objects (Money, Symbol, Currency)
- ✅ Defensive copying for collections
- ✅ Comprehensive validation at creation

## Error Handling ⚠️
- 🚫 `IllegalArgumentException` for business rule violations
- 🚫 `IllegalStateException` for invalid state transitions
- 🚫 `NullPointerException` for null parameters
- 📝 Descriptive error messages with context

## Currency Rules 💰
- ✅ Same currency required for monetary operations
- ✅ Currency-specific precision handling
- ✅ Appropriate rounding strategies per currency type
- ✅ Display formatting respects decimal places

## Transaction History 📚
- ✅ Orders maintain chronological transaction list
- ✅ Sequential numbering (1, 2, 3, ...)
- ✅ Immutable history (defensive copies)
- ✅ Automatic timestamp management

## Key Validation Points 🔍

### Before Transaction Creation
1. ✅ Orders are active (not FILLED/CANCELLED)
2. ✅ Orders have same symbol
3. ✅ Buy price >= Sell price
4. ✅ Execution price within valid range
5. ✅ Transaction quantity within limits
6. ✅ All parameters non-null and valid

### After Transaction Creation
1. ✅ Orders updated with transaction reference
2. ✅ Executed quantities incremented
3. ✅ Order statuses updated based on remaining quantity
4. ✅ Timestamps updated automatically

## Common Error Scenarios ❌

| Scenario | Exception | Message Pattern |
|----------|-----------|-----------------|
| Null order ID | `NullPointerException` | "ID cannot be null" |
| Negative quantity | `IllegalArgumentException` | "Quantity must be positive" |
| Wrong currency | `IllegalArgumentException` | "Price currency ... does not match ..." |
| No price overlap | `IllegalArgumentException` | "Orders cannot match: buy price is less than sell price" |
| Insufficient quantity | `IllegalArgumentException` | "Transaction quantity cannot exceed ... remaining quantity" |
| Inactive order | `IllegalArgumentException` | "... order must be active (not FILLED or CANCELLED)" |
| Terminal state modification | `IllegalStateException` | "Cannot update ... of terminal order" |

## Test Coverage Summary 📋

| Rule Category | Test Class | Key Tests |
|---------------|------------|-----------|
| State Updates | `TransactionOrderStateUpdatesTest` | 6 tests |
| Quantity Tracking | `QuantityTrackingTest` | 4 tests |
| Multiple Transactions | `MultipleTransactionSupportTest` | 3 tests |
| Order Matching | `OrderMatchingRulesTest` | 4 tests |
| Transaction Ordering | `TransactionOrderingTest` | 5 tests |
| Domain Rules | `TransactionTest` | 20+ tests |
| Integration | `OrderDomainIntegrationTest` | 2 tests |

**Total: 45+ comprehensive test scenarios**

## Architecture Patterns Used 🏗️

- **Domain-Driven Design (DDD)**: Rich domain model with business logic
- **State Pattern**: Order status management with state transitions
- **Value Objects**: Immutable Money, Symbol, Currency classes
- **Factory Methods**: Static creation methods for common objects
- **Defensive Programming**: Comprehensive validation and null safety
- **Test-Driven Development**: Extensive test coverage for all rules