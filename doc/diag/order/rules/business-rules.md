# Order Management System - Business Rules

## Overview
This document outlines the business rules implemented in the Order Management System, derived from the domain model and comprehensive test suite.

## Core Domain Rules

### 1. Order Creation and Validation Rules

#### 1.1 Order Identity and Basic Properties
- **Rule**: Every order must have a unique, non-null identifier
- **Rule**: Orders must specify a valid symbol, price, and quantity
- **Rule**: Order quantity must be positive (> 0)
- **Rule**: Order price currency must match the symbol's quote currency
- **Implementation**: `AbstractOrder:26-40`, `validatePriceCurrency()`, `validateQuantity()`

#### 1.2 Order Status Lifecycle
- **Rule**: Orders start in `PENDING` status upon creation
- **Rule**: Order status transitions follow a strict state machine:
  - `PENDING` → `PARTIAL` (when partially filled)
  - `PENDING` → `FILLED` (when completely filled)
  - `PENDING` → `CANCELLED` (when cancelled)
  - `PARTIAL` → `FILLED` (when remaining quantity is filled)
  - `PARTIAL` → `CANCELLED` (when cancelled)
  - `FILLED` and `CANCELLED` are terminal states (no further transitions)
- **Implementation**: State pattern in `OrderStatus` class and state classes

### 2. Transaction Creation and Validation Rules

#### 2.1 Order Matching Prerequisites
- **Rule**: Orders can only be matched if buy price >= sell price
- **Rule**: Both buy and sell orders must be active (not FILLED or CANCELLED)
- **Rule**: Orders must have the same symbol
- **Rule**: Transaction symbol must match order symbols
- **Implementation**: `AbstractTransaction:validateOrderMatching()`, `ValidateOrderState` validator

#### 2.2 Execution Price Validation
- **Rule**: Transaction execution price must be within the buy-sell price range
- **Rule**: Execution price must be >= sell order price (seller's minimum)
- **Rule**: Execution price must be <= buy order price (buyer's maximum)
- **Rule**: System provides optimal price discovery using mid-point calculation: `(buy_price + sell_price) / 2`
- **Implementation**: `AbstractTransaction:validateOrderMatching()`, `determineExecutionPrice()`

#### 2.3 Quantity Constraints
- **Rule**: Transaction quantity must be positive (> 0)
- **Rule**: Transaction quantity cannot exceed buy order's remaining quantity
- **Rule**: Transaction quantity cannot exceed sell order's remaining quantity
- **Rule**: Multiple transactions are allowed on the same order until fully executed
- **Implementation**: `AbstractTransaction:validateQuantityConstraints()`

### 3. Order Execution and State Management Rules

#### 3.1 Quantity Tracking
- **Rule**: Orders track executed quantity (cumulative across all transactions)
- **Rule**: Orders track remaining quantity (original - executed)
- **Rule**: Remaining quantity must always be >= 0
- **Rule**: When remaining quantity = 0, order is considered fully executed
- **Implementation**: `AbstractOrder:addTransaction()`, quantity tracking methods

#### 3.2 Automatic Status Updates
- **Rule**: Orders automatically update status after each transaction:
  - If remaining quantity = 0 → status becomes `FILLED`
  - If remaining quantity > 0 and status = `PENDING` → status becomes `PARTIAL`
  - If remaining quantity > 0 and status = `PARTIAL` → status remains `PARTIAL`
- **Implementation**: `AbstractOrder:updateStatusAfterExecution()`

#### 3.3 Transaction History
- **Rule**: Orders maintain an ordered list of all associated transactions
- **Rule**: Transaction sequence numbers are assigned based on order of execution
- **Rule**: Transaction history is immutable (returned as defensive copy)
- **Implementation**: `AbstractOrder:getTransactions()`, `getTransactionSequence()`

### 4. Order Modification Rules

#### 4.1 Price Updates
- **Rule**: Order price can only be updated if order is not in terminal state
- **Rule**: New price must match symbol's quote currency
- **Rule**: Price updates automatically update the order's `updatedAt` timestamp
- **Implementation**: `AbstractOrder:updatePrice()`

#### 4.2 Order Cancellation
- **Rule**: Active orders can be cancelled (transitions to `CANCELLED` state)
- **Rule**: Partial orders can be cancelled (transitions to `CANCELLED` state)
- **Rule**: Filled orders cannot be cancelled (terminal state)
- **Rule**: Already cancelled orders cannot be cancelled again
- **Implementation**: `AbstractOrder:cancel()`, `cancelPartial()`

### 5. Domain-Specific Business Rules

#### 5.1 Buy Order Rules
- **Rule**: Buy orders represent willingness to purchase at or below specified price
- **Rule**: Cost basis equals total value (price × quantity)
- **Rule**: Buy orders can match with sell orders when buy price >= sell price
- **Implementation**: `BuyOrder:getCostBasis()`

#### 5.2 Sell Order Rules
- **Rule**: Sell orders represent willingness to sell at or above specified price
- **Rule**: Proceeds equal total value (price × quantity)
- **Rule**: Sell orders can match with buy orders when sell price <= buy price
- **Implementation**: `SellOrder:getProceeds()`

#### 5.3 Transaction Rules
- **Rule**: Transactions represent executed trades between buy and sell orders
- **Rule**: Transactions are immutable once created
- **Rule**: Transaction creation automatically updates both participating orders
- **Rule**: Transaction total value = execution price × quantity
- **Implementation**: `Transaction` class, `AbstractTransaction:updateOrdersAfterTransaction()`

### 6. Data Integrity Rules

#### 6.1 Null Safety
- **Rule**: All critical domain objects must be non-null
- **Rule**: Null values are rejected with appropriate error messages
- **Implementation**: Comprehensive null checks throughout domain classes

#### 6.2 Immutability
- **Rule**: Core value objects (Money, Symbol, Currency) are immutable
- **Rule**: Defensive copying is used for collections and mutable objects
- **Rule**: Transaction objects are immutable after creation
- **Implementation**: Value object design, defensive copying patterns

#### 6.3 Validation Chain
- **Rule**: Domain objects validate their state upon creation
- **Rule**: Business rule violations result in `IllegalArgumentException`
- **Rule**: State transition violations result in `IllegalStateException`
- **Implementation**: Constructor validation, state validation methods

### 7. Temporal Rules

#### 7.1 Timestamps
- **Rule**: All domain objects have creation timestamps
- **Rule**: Orders have both creation and last-updated timestamps
- **Rule**: Transactions have creation timestamps (immutable)
- **Rule**: Timestamps are automatically managed by the system
- **Implementation**: Automatic timestamp management in constructors

#### 7.2 Transaction Ordering
- **Rule**: Transactions are ordered by creation timestamp
- **Rule**: Earlier transactions have lower sequence numbers
- **Rule**: Transaction sequence is maintained within each order
- **Implementation**: Transaction list ordering, sequence numbering

### 8. Currency and Monetary Rules

#### 8.1 Currency Compatibility
- **Rule**: Monetary operations require same currency
- **Rule**: Order price currency must match symbol's quote currency
- **Rule**: Different currencies cannot be mixed in operations
- **Implementation**: `Money` class currency validation, `validatePriceCurrency()`

#### 8.2 Precision Handling
- **Rule**: Monetary amounts preserve appropriate precision based on currency
- **Rule**: Arithmetic operations maintain currency-specific precision
- **Rule**: Display formatting respects currency decimal places
- **Implementation**: `Currency` enum with arithmetic strategies, `Money` class operations

## Error Handling Rules

### 9.1 Business Rule Violations
- **Rule**: Business rule violations throw `IllegalArgumentException`
- **Rule**: State transition violations throw `IllegalStateException`
- **Rule**: Null parameter violations throw `NullPointerException`
- **Implementation**: Comprehensive validation and error handling

### 9.2 Error Messages
- **Rule**: Error messages provide clear, actionable information
- **Rule**: Error messages include relevant context (values, states, etc.)
- **Rule**: Error messages are consistent across the domain
- **Implementation**: Descriptive error messages in validation methods

## Testing Strategy

### 10.1 Test Coverage
- **Rule**: All business rules are verified through comprehensive tests
- **Rule**: Edge cases and error conditions are thoroughly tested
- **Rule**: Integration tests verify complete order lifecycle scenarios
- **Implementation**: Test classes covering all domain rules

### 10.2 Test Organization
- **Rule**: Tests are organized by functional area:
  - Order lifecycle and state transitions
  - Quantity tracking and execution
  - Multiple transaction support
  - Order matching and validation
  - Transaction ordering and sequencing
- **Implementation**: Structured test classes with clear naming

## Summary

This Order Management System implements a robust domain model with comprehensive business rules covering:
- Order creation, validation, and lifecycle management
- Transaction matching and execution
- Quantity tracking and state management
- Currency and monetary operations
- Data integrity and validation
- Error handling and temporal consistency

The rules are enforced through domain-driven design principles, comprehensive validation, and extensive test coverage, ensuring system reliability and business requirement compliance.