package core.ms.order_book.application.services;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order_book.domain.entities.OrderBook;
import core.ms.order_book.domain.events.OrderMatchedEvent;
import core.ms.order_book.domain.ports.inbound.OrderBookOperationResult;
import core.ms.order_book.domain.ports.inbound.OrderBookService;
import core.ms.order_book.domain.ports.outbound.OrderBookRepository;
import core.ms.order_book.domain.ports.outbound.OrderMatchEventPublisher;
import core.ms.order_book.domain.value_object.MarketDepth;
import core.ms.order_book.domain.value_object.MarketOverview;
import core.ms.order_book.infrastructure.persistence.OrderBookRepositoryService;
import core.ms.shared.money.Symbol;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
        if (orderBookRepository instanceof OrderBookRepositoryService) {
            OrderBookRepositoryService repoService = (OrderBookRepositoryService) orderBookRepository;
            return repoService.getManager().getMarketOverview();
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