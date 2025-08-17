package core.ms.market_engine.application.handlers;

import core.ms.order.application.services.OrderApplicationService;
import core.ms.order_book.application.services.OrderBookApplicationService;
import core.ms.portfolio.domain.events.publish.BuyOrderRequestedEvent;
import core.ms.shared.events.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PortfolioEventHandler {
    private static final Logger log = LoggerFactory.getLogger(PortfolioEventHandler.class);

    @Autowired
    private OrderApplicationService orderService;

    @Autowired
    private OrderBookApplicationService orderBookService;

    @Autowired
    private EventBus eventBus;

    @EventListener
    public void handleBuyOrderRequested(BuyOrderRequestedEvent event) {
        log.info("Market Engine: Processing buy order request from portfolio: {}",
                event.getPortfolioId());

        try {
            // Step 1: Create order in Order BC
            var result = orderService.createBuyOrderWithReservation(
                    event.getPortfolioId(),
                    event.getReservationId(),
                    event.getSymbol(),
                    event.getPrice(),
                    event.getQuantity()
            );

            if (result.isSuccess()) {
                // Step 2: Get the created order
                var orderOpt = orderService.findOrderById(result.getOrderId());

                if (orderOpt.isPresent()) {
                    // Step 3: Add to OrderBook for matching
                    var orderBookResult = orderBookService.addOrderToBook(orderOpt.get());

                    if (orderBookResult.hasMatches()) {
                        log.info("Matches found for order: {}", result.getOrderId());
                        // OrderBook will emit OrderMatchedEvent
                    } else {
                        log.info("Order added to book, no matches: {}", result.getOrderId());
                    }
                }
            } else {
                log.error("Failed to create buy order: {}", result.getMessage());
                // Release reservation on failure
                releaseReservation(event.getPortfolioId(), event.getReservationId(), "CASH");
            }
        } catch (Exception e) {
            log.error("Error processing buy order request", e);
            releaseReservation(event.getPortfolioId(), event.getReservationId(), "CASH");
        }
    }

    @EventListener
    public void handleSellOrderRequested(SellOrderRequestedEvent event) {
        log.info("Market Engine: Processing sell order request from portfolio: {}",
                event.getPortfolioId());

        try {
            // Step 1: Create order in Order BC
            var result = orderService.createSellOrderWithReservation(
                    event.getPortfolioId(),
                    event.getReservationId(),
                    event.getSymbol(),
                    event.getPrice(),
                    event.getQuantity()
            );

            if (result.isSuccess()) {
                // Step 2: Get the created order
                var orderOpt = orderService.findOrderById(result.getOrderId());

                if (orderOpt.isPresent()) {
                    // Step 3: Add to OrderBook for matching
                    var orderBookResult = orderBookService.addOrderToBook(orderOpt.get());

                    if (orderBookResult.hasMatches()) {
                        log.info("Matches found for order: {}", result.getOrderId());
                        // OrderBook will emit OrderMatchedEvent
                    } else {
                        log.info("Order added to book, no matches: {}", result.getOrderId());
                    }
                }
            } else {
                log.error("Failed to create sell order: {}", result.getMessage());
                // Release reservation on failure
                releaseReservation(event.getPortfolioId(), event.getReservationId(), "ASSET");
            }
        } catch (Exception e) {
            log.error("Error processing sell order request", e);
            releaseReservation(event.getPortfolioId(), event.getReservationId(), "ASSET");
        }
    }

    private void releaseReservation(String portfolioId, String reservationId, String type) {
        // Emit event to release reservation
        eventBus.publish(new ReservationReleaseRequestedEvent(portfolioId, reservationId, type));
    }
}