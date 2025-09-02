📊 Order Book BC - Clarified Responsibilities
What Order Book BC DOES:
1. Core Order Book Management (Internal)

Maintains bid/ask price levels for each symbol
Adds/removes orders when instructed by Market Engine
Detects when spreads cross and generates match events
Manages efficient data structures for price-time priority

2. Market Data Provider (External - READ ONLY)

Provides real-time market data via WebSocket ✅
Exposes REST endpoints for market queries ✅
Streams order book updates to frontend
Provides market depth, best bid/ask, spreads
This is PUBLIC DATA not business operations

What Order Book BC DOESN'T DO:

Doesn't accept direct order placement from users
Doesn't modify orders on its own
Doesn't create transactions
Doesn't validate user permissions/funds
Doesn't make matching decisions (only detects opportunities)


┌─────────────────────────────────────────────────────────────┐
│                      FRONTEND APP                           │
│                   (Trading Interface)                       │
└─────────────┬────────────────────┬─────────────────────────┘
              │                    │
     WebSocket│ (Market Data)      │ REST (Order Placement)
              │                    │
              ↓                    ↓
┌─────────────────────┐    ┌──────────────────┐
│   ORDER BOOK BC     │    │   PORTFOLIO BC    │
│                     │    │                   │
│ ✅ GET /depth       │    │ ✅ POST /orders   │
│ ✅ WS /stream       │    │    (creates order │
│ ✅ GET /spread      │    │     request)      │
│                     │    └─────────┬─────────┘
│ ❌ NO POST/PUT/DEL  │              │ Event
└──────────┬──────────┘              ↓
           │              ┌──────────────────┐
           │              │  MARKET ENGINE   │
           ←──────────────┤                  │
        Commands as       │ Orchestrates all │
        Events Only       │ business flows   │
                         └──────────────────┘



