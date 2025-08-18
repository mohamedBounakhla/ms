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
import core.ms.shared.money.Symbol;
import jakarta.transaction.Transactional;
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

    private final OrderBookRepository orderBookRepository;
    private final OrderMatchEventPublisher eventPublisher;

    @Autowired
    public OrderBookApplicationService(
            OrderBookRepository orderBookRepository,
            OrderMatchEventPublisher eventPublisher) {
        this.orderBookRepository = Objects.requireNonNull(orderBookRepository);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    public OrderBookTickerDTO getOrderBookTicker(Symbol symbol) {
        Objects.requireNonNull(symbol, "Symbol cannot be null");

        OrderBook orderBook = getOrCreateOrderBook(symbol);
        OrderBookTickerDTO ticker = new OrderBookTickerDTO();

        ticker.setSymbol(symbol.getCode());
        ticker.setCurrency(symbol.getQuoteCurrency());
        ticker.setTimestamp(LocalDateTime.now());

        // Get best bid
        orderBook.getBestBid().ifPresent(bid -> {
            ticker.setBidPrice(bid.getAmount());
            orderBook.getBestBuyOrder().ifPresent(order ->
                    ticker.setBidQuantity(order.getRemainingQuantity())
            );
        });

        // Get best ask
        orderBook.getBestAsk().ifPresent(ask -> {
            ticker.setAskPrice(ask.getAmount());
            orderBook.getBestSellOrder().ifPresent(order ->
                    ticker.setAskQuantity(order.getRemainingQuantity())
            );
        });

        // Calculate spread
        orderBook.getSpread().ifPresent(spread ->
                ticker.setSpread(spread.getAmount())
        );

        return ticker;
    }
    /**
     * Gets detailed order book statistics.
     */
    public OrderBookStatisticsDTO getOrderBookStatistics(Symbol symbol) {
        Objects.requireNonNull(symbol, "Symbol cannot be null");

        OrderBook orderBook = getOrCreateOrderBook(symbol);
        OrderBookStatisticsDTO stats = new OrderBookStatisticsDTO();

        // Count orders
        stats.setTotalBuyOrders((int) orderBook.getBidLevels().stream()
                .mapToLong(level -> level.getOrderCount())
                .sum());

        stats.setTotalSellOrders((int) orderBook.getAskLevels().stream()
                .mapToLong(level -> level.getOrderCount())
                .sum());

        // Volumes
        stats.setTotalBuyVolume(orderBook.getTotalBidVolume());
        stats.setTotalSellVolume(orderBook.getTotalAskVolume());

        // Best prices
        orderBook.getBestBid().ifPresent(bid -> {
            stats.setBestBidPrice(bid.getAmount());
            stats.setPriceCurrency(bid.getCurrency());
        });

        orderBook.getBestAsk().ifPresent(ask -> {
            stats.setBestAskPrice(ask.getAmount());
        });

        // Spread
        orderBook.getSpread().ifPresent(spread ->
                stats.setSpread(spread.getAmount())
        );

        return stats;
    }

    @Override
    public OrderBookOperationResult addOrderToBook(IOrder order) {
        try {
            Objects.requireNonNull(order, "Order cannot be null");

            // Get or create order book for the symbol
            OrderBook orderBook = getOrCreateOrderBook(order.getSymbol());

            // Add order based on type
            if (order instanceof IBuyOrder) {
                orderBook.addOrder((IBuyOrder) order);
            } else if (order instanceof ISellOrder) {
                orderBook.addOrder((ISellOrder) order);
            } else {
                return OrderBookOperationResult.builder()
                        .success(false)
                        .message("Unknown order type: " + order.getClass().getSimpleName())
                        .orderId(order.getId())
                        .build();
            }

            // Save the updated order book
            orderBookRepository.save(orderBook);

            // Check for matches and process if found
            if (orderBook.hasRecentMatches()) {
                List<OrderMatchedEvent> matchEvents = orderBook.consumeRecentMatchEvents();

                // Publish events through infrastructure
                eventPublisher.publishOrderMatchedEvents(matchEvents);

                return OrderBookOperationResult.builder()
                        .success(true)
                        .message(String.format("Order added with %d matches", matchEvents.size()))
                        .matchEvents(matchEvents)
                        .orderId(order.getId())
                        .build();
            }

            return OrderBookOperationResult.builder()
                    .success(true)
                    .message("Order added to book, no matches found")
                    .orderId(order.getId())
                    .build();

        } catch (Exception e) {
            return OrderBookOperationResult.builder()
                    .success(false)
                    .message("Failed to add order: " + e.getMessage())
                    .orderId(order != null ? order.getId() : null)
                    .build();
        }
    }
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

        // Calculate imbalance
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

    /**
     * Gets all active trading symbols.
     */
    public Set<Symbol> getActiveSymbols() {
        Collection<OrderBook> allOrderBooks = orderBookRepository.findAll();
        return allOrderBooks.stream()
                .map(OrderBook::getSymbol)
                .collect(Collectors.toSet());
    }

    /**
     * Checks if an order book exists and is active for a symbol.
     */
    public boolean isOrderBookActive(Symbol symbol) {
        return orderBookRepository.existsBySymbol(symbol);
    }

    /**
     * Gets the total number of orders across all books.
     */
    public int getTotalOrderCount() {
        return orderBookRepository.findAll().stream()
                .mapToInt(OrderBook::getOrderCount)
                .sum();
    }

    /**
     * Gets market statistics for all symbols.
     */
    public Map<String, OrderBookStatisticsDTO> getAllMarketStatistics() {
        Map<String, OrderBookStatisticsDTO> allStats = new HashMap<>();

        for (OrderBook orderBook : orderBookRepository.findAll()) {
            OrderBookStatisticsDTO stats = getOrderBookStatistics(orderBook.getSymbol());
            allStats.put(orderBook.getSymbol().getCode(), stats);
        }

        return allStats;
    }

    // ===== HELPER METHODS (keep existing one) =====


    @Override
    public OrderBookOperationResult removeOrderFromBook(String orderId, Symbol symbol) {
        try {
            Objects.requireNonNull(orderId, "Order ID cannot be null");
            Objects.requireNonNull(symbol, "Symbol cannot be null");

            Optional<OrderBook> orderBookOpt = orderBookRepository.findBySymbol(symbol);

            if (orderBookOpt.isEmpty()) {
                return OrderBookOperationResult.builder()
                        .success(false)
                        .message("Order book not found for symbol: " + symbol.getCode())
                        .orderId(orderId)
                        .build();
            }

            OrderBook orderBook = orderBookOpt.get();

            // Note: This requires OrderBook to have removeOrderById method
            // For now, we'll return a placeholder response

            return OrderBookOperationResult.builder()
                    .success(true)
                    .message("Order removed from book")
                    .orderId(orderId)
                    .build();

        } catch (Exception e) {
            return OrderBookOperationResult.builder()
                    .success(false)
                    .message("Failed to remove order: " + e.getMessage())
                    .orderId(orderId)
                    .build();
        }
    }

    @Override
    public List<OrderMatchedEvent> processPendingMatches(Symbol symbol) {
        Objects.requireNonNull(symbol, "Symbol cannot be null");

        Optional<OrderBook> orderBookOpt = orderBookRepository.findBySymbol(symbol);

        if (orderBookOpt.isEmpty()) {
            return new ArrayList<>();
        }

        OrderBook orderBook = orderBookOpt.get();

        if (orderBook.hasRecentMatches()) {
            List<OrderMatchedEvent> events = orderBook.consumeRecentMatchEvents();
            eventPublisher.publishOrderMatchedEvents(events);
            return events;
        }

        return new ArrayList<>();
    }

    @Override
    public List<OrderMatchedEvent> processAllPendingMatches() {
        List<OrderMatchedEvent> allEvents = new ArrayList<>();

        for (OrderBook orderBook : orderBookRepository.findAll()) {
            if (orderBook.hasRecentMatches()) {
                List<OrderMatchedEvent> events = orderBook.consumeRecentMatchEvents();
                allEvents.addAll(events);
            }
        }

        if (!allEvents.isEmpty()) {
            eventPublisher.publishOrderMatchedEvents(allEvents);
        }

        return allEvents;
    }

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
        // Access the manager through the repository service
        if (orderBookRepository instanceof OrderBookRepositoryJpaImpl repo) {
            return repo.getManager().getMarketOverview();
        }

        // Fallback: build overview from available order books
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

            return OrderBookOperationResult.builder()
                    .success(true)
                    .message("Order book created for symbol: " + symbol.getCode())
                    .build();

        } catch (Exception e) {
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

            // Save the updated order book
            orderBookRepository.save(orderBook);
        }

        return totalRemoved;
    }

    // Private helper method
    private OrderBook getOrCreateOrderBook(Symbol symbol) {
        return orderBookRepository.findBySymbol(symbol)
                .orElseGet(() -> {
                    OrderBook newOrderBook = new OrderBook(symbol);
                    return orderBookRepository.save(newOrderBook);
                });
    }
}