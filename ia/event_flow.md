Empirical Implementation Plan - Ordered
1. Order BC (Build First)
   Publishes:

OrderCreatedEvent (orderId, portfolioId, reservationId, symbol, price, quantity, status)
OrderCreationFailedEvent (reservationId, reason)
TransactionCreatedEvent (transactionId, buyOrderId, sellOrderId, executedQuantity, price)

Listens to:

OrderRequestedEvent → Create order with reservation reference
OrderMatchedEvent → Create transaction, update both orders' executed quantities

Can test with: Mocked OrderRequestedEvent and OrderMatchedEvent

2. OrderBook BC (Build Second)
   Publishes:

OrderMatchedEvent (buyOrderId, sellOrderId, matchedQuantity, executionPrice)

Listens to:

OrderCreatedEvent → Add to book, run matching algorithm
TransactionCreatedEvent → Remove/update orders based on remaining quantity

Can test with: Real OrderCreatedEvent from Order BC, mocked TransactionCreatedEvent

3. Portfolio BC (Build Last)
   Publishes:

OrderRequestedEvent (reservationId, portfolioId, symbol, price, quantity, orderType)

Listens to:

OrderCreatedEvent → Mark reservation as CONFIRMED
OrderCreationFailedEvent → Release reservation
TransactionCreatedEvent → Execute reservation, update balances

Additional requirement: Timeout mechanism for reservations (auto-release after X minutes)
Can test with: Real events from Order BC