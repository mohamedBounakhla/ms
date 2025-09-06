package core.ms.order_book.application.services;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order_book.application.dto.query.OrderBookStatisticsDTO;
import core.ms.order_book.application.dto.query.OrderBookSummaryDTO;
import core.ms.order_book.application.dto.query.OrderBookTickerDTO;
import core.ms.order_book.domain.entities.OrderBook;
import core.ms.order_book.domain.events.publish.OrderMatchedEvent;
import core.ms.order_book.domain.ports.inbound.OrderBookOperationResult;
import core.ms.order_book.domain.ports.inbound.OrderBookService;
import core.ms.order_book.domain.ports.outbound.OrderBookRepository;
import core.ms.order_book.domain.ports.outbound.OrderMatchEventPublisher;
import core.ms.order_book.domain.value_object.MarketDepth;
import core.ms.order_book.domain.value_object.MarketOverview;
import core.ms.order_book.infrastructure.persistence.OrderBookRepositoryJpaImpl;
import core.ms.shared.events.EventContext;
import core.ms.shared.money.Symbol;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderBookApplicationService implements OrderBookService {

    private static final Logger logger = LoggerFactory.getLogger(OrderBookApplicationService.class);

    private final OrderBookRepository orderBookRepository;
    private final OrderMatchEventPublisher eventPublisher;

    @Autowired
    public OrderBookApplicationService(
            OrderBookRepository orderBookRepository,
            OrderMatchEventPublisher eventPublisher) {
        this.orderBookRepository = Objects.requireNonNull(orderBookRepository);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    @Override
    public OrderBookOperationResult addOrderToBook(IOrder order) {
        try {
            Objects.requireNonNull(order, "Order cannot be null");

            String correlationId = EventContext.getCurrentCorrelationId();

            logger.info("════════════════════════════════════════════════════════════════");
            logger.info("📚 ORDERBOOK SERVICE: Adding order to book");
            logger.info("   - Correlation ID: {}", correlationId);
            logger.info("   - Order ID: {}", order.getId());
            logger.info("   - Symbol: {}", order.getSymbol().getCode());
            logger.info("   - Type: {}", order.getClass().getSimpleName());
            logger.info("   - Price: {} {}", order.getPrice().getAmount(), order.getPrice().getCurrency());
            logger.info("   - Quantity: {}", order.getQuantity());
            logger.info("════════════════════════════════════════════════════════════════");

            // Get or create order book for the symbol
            OrderBook orderBook = getOrCreateOrderBook(order.getSymbol());

            // LOG CURRENT STATE
            logger.info("📊 ORDERBOOK STATE BEFORE ADD:");
            logger.info("   - Total Orders: {}", orderBook.getOrderCount());
            logger.info("   - Best Bid: {}", orderBook.getBestBid().map(m -> m.toString()).orElse("NONE"));
            logger.info("   - Best Ask: {}", orderBook.getBestAsk().map(m -> m.toString()).orElse("NONE"));
            logger.info("   - Spread Crossed: {}",
                    orderBook.getBestBid().isPresent() && orderBook.getBestAsk().isPresent() &&
                            orderBook.getBestBid().get().isGreaterThanOrEqual(orderBook.getBestAsk().get()));

            // Add order based on type
            if (order instanceof IBuyOrder) {
                logger.info("➕ Adding BUY order to book");
                orderBook.addOrder((IBuyOrder) order);
            } else if (order instanceof ISellOrder) {
                logger.info("➕ Adding SELL order to book");
                orderBook.addOrder((ISellOrder) order);
            } else {
                logger.error("❌ Unknown order type: {}", order.getClass().getSimpleName());
                return OrderBookOperationResult.builder()
                        .success(false)
                        .message("Unknown order type: " + order.getClass().getSimpleName())
                        .orderId(order.getId())
                        .build();
            }

            // LOG STATE AFTER ADD
            logger.info("📊 ORDERBOOK STATE AFTER ADD:");
            logger.info("   - Total Orders: {}", orderBook.getOrderCount());
            logger.info("   - Best Bid: {}", orderBook.getBestBid().map(m -> m.toString()).orElse("NONE"));
            logger.info("   - Best Ask: {}", orderBook.getBestAsk().map(m -> m.toString()).orElse("NONE"));

            // Save the updated order book
            orderBookRepository.save(orderBook);

            // CHECK FOR MATCHES
            logger.info("🔍 CHECKING FOR MATCHES...");

            if (orderBook.hasRecentMatches()) {
                List<OrderMatchedEvent> matchEvents = orderBook.consumeRecentMatchEvents();

                logger.info("🎯 MATCHES FOUND! Count: {}", matchEvents.size());

                for (OrderMatchedEvent matchEvent : matchEvents) {
                    logger.info("   📝 Match Details:");
                    logger.info("      - Buy Order: {}", matchEvent.getBuyOrderId());
                    logger.info("      - Sell Order: {}", matchEvent.getSellOrderId());
                    logger.info("      - Quantity: {}", matchEvent.getMatchedQuantity());
                    logger.info("      - Price: {} {}",
                            matchEvent.getExecutionPrice().getAmount(),
                            matchEvent.getExecutionPrice().getCurrency());
                }

                // Publish events to the saga
                logger.info("📤 Publishing {} match events to Order BC", matchEvents.size());
                eventPublisher.publishOrderMatchedEvents(matchEvents);

            } else {
                logger.info("❌ NO MATCHES FOUND");

                // DEBUG: Check why no matches
                if (orderBook.getBestBid().isPresent() && orderBook.getBestAsk().isPresent()) {
                    var bestBid = orderBook.getBestBid().get();
                    var bestAsk = orderBook.getBestAsk().get();
                    logger.info("   🔍 Match Debug:");
                    logger.info("      - Best Bid: {}", bestBid);
                    logger.info("      - Best Ask: {}", bestAsk);
                    logger.info("      - Bid >= Ask: {}", bestBid.isGreaterThanOrEqual(bestAsk));

                    if (!bestBid.isGreaterThanOrEqual(bestAsk)) {
                        logger.info("      - Spread not crossed - no match possible");
                        logger.info("      - Spread: {}", bestAsk.subtract(bestBid));
                    }
                } else {
                    logger.info("   - Missing bid or ask side for matching");
                }
            }

            logger.info("════════════════════════════════════════════════════════════════");
            logger.info("✅ ORDERBOOK SERVICE: Order {} added successfully", order.getId());
            logger.info("════════════════════════════════════════════════════════════════");

            return OrderBookOperationResult.builder()
                    .success(true)
                    .message("Order added to book")
                    .orderId(order.getId())
                    .build();

        } catch (Exception e) {
            logger.error("💥 ORDERBOOK SERVICE: Failed to add order to book", e);
            logger.error("   - Order ID: {}", order != null ? order.getId() : "null");
            logger.error("   - Error: {}", e.getMessage());

            return OrderBookOperationResult.builder()
                    .success(false)
                    .message("Failed to add order: " + e.getMessage())
                    .orderId(order != null ? order.getId() : null)
                    .build();
        }
    }

    @Override
    public OrderBookOperationResult removeOrderFromBook(String orderId, Symbol symbol) {
        logger.info("🗑️ ORDERBOOK SERVICE: Removing order {} from book", orderId);

        OrderBook orderBook = orderBookRepository.findBySymbol(symbol)
                .orElseThrow(() -> new IllegalArgumentException("Order book not found"));

        boolean removed = orderBook.removeOrderById(orderId);

        if (removed) {
            orderBookRepository.save(orderBook);
            logger.info("✅ Order {} removed from book", orderId);
            return OrderBookOperationResult.builder()
                    .success(true)
                    .message("Order removed")
                    .orderId(orderId)
                    .build();
        }

        logger.warn("⚠️ Order {} not found in book", orderId);
        return OrderBookOperationResult.builder()
                .success(false)
                .message("Order not found")
                .orderId(orderId)
                .build();
    }

    @Override
    public void processPendingMatches(Symbol symbol) {
        Objects.requireNonNull(symbol, "Symbol cannot be null");

        String correlationId = EventContext.getCurrentCorrelationId();
        logger.info("🔄 ORDERBOOK SERVICE: Processing pending matches for symbol {}", symbol.getCode());

        Optional<OrderBook> orderBookOpt = orderBookRepository.findBySymbol(symbol);

        if (orderBookOpt.isEmpty()) {
            logger.warn("⚠️ No order book found for symbol {}", symbol.getCode());
            return;
        }

        OrderBook orderBook = orderBookOpt.get();

        if (orderBook.hasRecentMatches()) {
            List<OrderMatchedEvent> events = orderBook.consumeRecentMatchEvents();

            logger.info("📤 Publishing {} pending matches for symbol {}", events.size(), symbol.getCode());

            for (OrderMatchedEvent event : events) {
                logger.info("   - Match: Buy {} vs Sell {}, Qty: {}",
                        event.getBuyOrderId(), event.getSellOrderId(), event.getMatchedQuantity());
            }

            eventPublisher.publishOrderMatchedEvents(events);
        } else {
            logger.info("❌ No pending matches for symbol {}", symbol.getCode());
        }
    }

    @Override
    public void processAllPendingMatches() {
        String correlationId = EventContext.getCurrentCorrelationId();
        logger.info("🔄 ORDERBOOK SERVICE: Processing ALL pending matches");

        int totalMatches = 0;

        for (OrderBook orderBook : orderBookRepository.findAll()) {
            if (orderBook.hasRecentMatches()) {
                List<OrderMatchedEvent> events = orderBook.consumeRecentMatchEvents();
                totalMatches += events.size();

                logger.info("📤 Publishing {} matches for symbol {}",
                        events.size(), orderBook.getSymbol().getCode());

                eventPublisher.publishOrderMatchedEvents(events);
            }
        }

        if (totalMatches > 0) {
            logger.info("✅ Published {} total pending matches", totalMatches);
        } else {
            logger.info("❌ No pending matches found across all books");
        }
    }

    // ... rest of the methods remain the same ...

    @Override
    public MarketDepth getMarketDepth(Symbol symbol, int levels) {
        Objects.requireNonNull(symbol, "Symbol cannot be null");
        if (levels <= 0) {
            throw new IllegalArgumentException("Levels must be positive");
        }
        OrderBook orderBook = getOrCreateOrderBook(symbol);
        return orderBook.getMarketDepth(levels);
    }

    @Override
    public MarketOverview getMarketOverview() {
        if (orderBookRepository instanceof OrderBookRepositoryJpaImpl repo) {
            return repo.getManager().getMarketOverview();
        }
        throw new UnsupportedOperationException("Market overview not available with current repository implementation");
    }

    @Override
    public OrderBookOperationResult createOrderBook(Symbol symbol) {
        try {
            Objects.requireNonNull(symbol, "Symbol cannot be null");

            if (orderBookRepository.existsBySymbol(symbol)) {
                return OrderBookOperationResult.builder()
                        .success(false)
                        .message("Order book already exists for symbol: " + symbol.getCode())
                        .build();
            }

            OrderBook orderBook = new OrderBook(symbol);
            orderBookRepository.save(orderBook);

            logger.info("Order book created for symbol: {}", symbol.getCode());

            return OrderBookOperationResult.builder()
                    .success(true)
                    .message("Order book created for symbol: " + symbol.getCode())
                    .build();

        } catch (Exception e) {
            logger.error("Failed to create order book for symbol: {}", symbol.getCode(), e);
            return OrderBookOperationResult.builder()
                    .success(false)
                    .message("Failed to create order book: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public OrderBookOperationResult removeOrderBook(Symbol symbol) {
        try {
            Objects.requireNonNull(symbol, "Symbol cannot be null");

            boolean removed = orderBookRepository.deleteBySymbol(symbol);

            if (removed) {
                logger.info("Order book removed for symbol: {}", symbol.getCode());
                return OrderBookOperationResult.builder()
                        .success(true)
                        .message("Order book removed for symbol: " + symbol.getCode())
                        .build();
            } else {
                return OrderBookOperationResult.builder()
                        .success(false)
                        .message("Order book not found for symbol: " + symbol.getCode())
                        .build();
            }

        } catch (Exception e) {
            logger.error("Failed to remove order book for symbol: {}", symbol.getCode(), e);
            return OrderBookOperationResult.builder()
                    .success(false)
                    .message("Failed to remove order book: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public int cleanupInactiveOrders() {
        int totalRemoved = 0;

        for (OrderBook orderBook : orderBookRepository.findAll()) {
            int beforeCount = orderBook.getOrderCount();
            orderBook.removeInactiveOrders();
            int afterCount = orderBook.getOrderCount();
            totalRemoved += (beforeCount - afterCount);
            orderBookRepository.save(orderBook);
        }

        if (totalRemoved > 0) {
            logger.info("Cleaned up {} inactive orders", totalRemoved);
        }

        return totalRemoved;
    }

    @Override
    public OrderBookTickerDTO getOrderBookTicker(Symbol symbol) {
        Objects.requireNonNull(symbol, "Symbol cannot be null");

        OrderBook orderBook = getOrCreateOrderBook(symbol);
        OrderBookTickerDTO ticker = new OrderBookTickerDTO();

        ticker.setSymbol(symbol.getCode());
        ticker.setCurrency(symbol.getQuoteCurrency());
        ticker.setTimestamp(LocalDateTime.now());

        orderBook.getBestBid().ifPresent(bid -> {
            ticker.setBidPrice(bid.getAmount());
            orderBook.getBestBuyOrder().ifPresent(order ->
                    ticker.setBidQuantity(order.getRemainingQuantity())
            );
        });

        orderBook.getBestAsk().ifPresent(ask -> {
            ticker.setAskPrice(ask.getAmount());
            orderBook.getBestSellOrder().ifPresent(order ->
                    ticker.setAskQuantity(order.getRemainingQuantity())
            );
        });

        orderBook.getSpread().ifPresent(spread ->
                ticker.setSpread(spread.getAmount())
        );

        return ticker;
    }

    @Override
    public OrderBookStatisticsDTO getOrderBookStatistics(Symbol symbol) {
        Objects.requireNonNull(symbol, "Symbol cannot be null");

        OrderBook orderBook = getOrCreateOrderBook(symbol);
        OrderBookStatisticsDTO stats = new OrderBookStatisticsDTO();

        stats.setTotalBuyOrders((int) orderBook.getBidLevels().stream()
                .mapToLong(level -> level.getOrderCount())
                .sum());

        stats.setTotalSellOrders((int) orderBook.getAskLevels().stream()
                .mapToLong(level -> level.getOrderCount())
                .sum());

        stats.setTotalBuyVolume(orderBook.getTotalBidVolume());
        stats.setTotalSellVolume(orderBook.getTotalAskVolume());

        orderBook.getBestBid().ifPresent(bid -> {
            stats.setBestBidPrice(bid.getAmount());
            stats.setPriceCurrency(bid.getCurrency());
        });

        orderBook.getBestAsk().ifPresent(ask -> {
            stats.setBestAskPrice(ask.getAmount());
        });

        orderBook.getSpread().ifPresent(spread ->
                stats.setSpread(spread.getAmount())
        );

        return stats;
    }

    @Override
    public OrderBookSummaryDTO getOrderBookSummary(Symbol symbol) {
        Objects.requireNonNull(symbol, "Symbol cannot be null");

        OrderBook orderBook = getOrCreateOrderBook(symbol);
        OrderBookSummaryDTO summary = new OrderBookSummaryDTO();

        summary.setSymbol(symbol.getCode());
        summary.setBidLevels(orderBook.getBidLevels().size());
        summary.setAskLevels(orderBook.getAskLevels().size());
        summary.setTotalBidVolume(orderBook.getTotalBidVolume());
        summary.setTotalAskVolume(orderBook.getTotalAskVolume());
        summary.setTimestamp(LocalDateTime.now());

        BigDecimal totalVolume = summary.getTotalBidVolume().add(summary.getTotalAskVolume());
        if (totalVolume.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal volumeDiff = summary.getTotalBidVolume().subtract(summary.getTotalAskVolume());
            BigDecimal imbalance = volumeDiff.divide(totalVolume, 4, RoundingMode.HALF_UP);
            summary.setImbalance(imbalance);
        } else {
            summary.setImbalance(BigDecimal.ZERO);
        }

        return summary;
    }

    @Override
    public Set<Symbol> getActiveSymbols() {
        Collection<OrderBook> allOrderBooks = orderBookRepository.findAll();
        return allOrderBooks.stream()
                .map(OrderBook::getSymbol)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isOrderBookActive(Symbol symbol) {
        return orderBookRepository.existsBySymbol(symbol);
    }

    @Override
    public int getTotalOrderCount() {
        return orderBookRepository.findAll().stream()
                .mapToInt(OrderBook::getOrderCount)
                .sum();
    }

    @Override
    public Map<String, OrderBookStatisticsDTO> getAllMarketStatistics() {
        Map<String, OrderBookStatisticsDTO> allStats = new HashMap<>();

        for (OrderBook orderBook : orderBookRepository.findAll()) {
            OrderBookStatisticsDTO stats = getOrderBookStatistics(orderBook.getSymbol());
            allStats.put(orderBook.getSymbol().getCode(), stats);
        }

        return allStats;
    }

    private OrderBook getOrCreateOrderBook(Symbol symbol) {
        return orderBookRepository.findBySymbol(symbol)
                .orElseGet(() -> {
                    OrderBook newOrderBook = new OrderBook(symbol);
                    return orderBookRepository.save(newOrderBook);
                });
    }
}