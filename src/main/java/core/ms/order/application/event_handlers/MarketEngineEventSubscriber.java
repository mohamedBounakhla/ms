package core.ms.order.application.event_handlers;

import core.ms.order.application.services.OrderApplicationService;
import core.ms.order.application.services.TransactionApplicationService;
import core.ms.order.domain.events.subscribe.*;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class MarketEngineEventSubscriber {

    private static final Logger logger = LoggerFactory.getLogger(MarketEngineEventSubscriber.class);

    @Autowired
    private OrderApplicationService orderService;

    @Autowired
    private TransactionApplicationService transactionService;

    // ===== ORDER CREATION EVENTS =====

    @EventListener
    @Async
    public void handleCreateOrderCommand(CreateOrderEvent command) {
        logger.info("📥 Received CreateOrderCommand from Market Engine - Command ID: {}, Portfolio: {}, Type: {}, Symbol: {}",
                command.getCommandId(), command.getPortfolioId(), command.getOrderType(), command.getSymbolCode());

        try {
            // Validate event source (could add authentication/authorization here)
            validateEventSource(command);

            // Create domain objects
            Symbol symbol = createSymbol(command.getSymbolCode());
            Money price = Money.of(command.getPrice(), command.getCurrency());

            // Route to appropriate order type
            if ("BUY".equalsIgnoreCase(command.getOrderType())) {
                var result = orderService.createBuyOrder(
                        command.getPortfolioId(),
                        command.getReservationId(),
                        symbol,
                        price,
                        command.getQuantity()
                );

                if (result.isSuccess()) {
                    logger.info("✅ Successfully created BUY order - Command ID: {}, Order ID: {}",
                            command.getCommandId(), result.getOrderId());
                } else {
                    logger.error("❌ Failed to create BUY order - Command ID: {}, Errors: {}",
                            command.getCommandId(), result.getErrors());
                }

            } else if ("SELL".equalsIgnoreCase(command.getOrderType())) {
                var result = orderService.createSellOrder(
                        command.getPortfolioId(),
                        command.getReservationId(),
                        symbol,
                        price,
                        command.getQuantity()
                );

                if (result.isSuccess()) {
                    logger.info("✅ Successfully created SELL order - Command ID: {}, Order ID: {}",
                            command.getCommandId(), result.getOrderId());
                } else {
                    logger.error("❌ Failed to create SELL order - Command ID: {}, Errors: {}",
                            command.getCommandId(), result.getErrors());
                }

            } else {
                logger.error("❌ Invalid order type - Command ID: {}, Type: {}",
                        command.getCommandId(), command.getOrderType());
            }

        } catch (Exception e) {
            logger.error("💥 Failed to process CreateOrderCommand - Command ID: {}, Error: {}",
                    command.getCommandId(), e.getMessage(), e);
            // Could publish a failure event back to Market Engine here
        }
    }

    // ===== ORDER CANCELLATION EVENTS =====

    @EventListener
    @Async
    public void handleCancelOrderCommand(CancelOrderEvent command) {
        logger.info("📥 Received CancelOrderCommand from Market Engine - Command ID: {}, Order ID: {}, Reason: {}",
                command.getCommandId(), command.getOrderId(), command.getReason());

        try {
            validateEventSource(command);

            var result = orderService.cancelOrder(command.getOrderId());

            if (result.isSuccess()) {
                logger.info("✅ Successfully cancelled order - Command ID: {}, Order ID: {}",
                        command.getCommandId(), command.getOrderId());
            } else {
                logger.error("❌ Failed to cancel order - Command ID: {}, Order ID: {}, Errors: {}",
                        command.getCommandId(), command.getOrderId(), result.getErrors());
            }

        } catch (Exception e) {
            logger.error("💥 Failed to process CancelOrderCommand - Command ID: {}, Error: {}",
                    command.getCommandId(), e.getMessage(), e);
        }
    }

    @EventListener
    @Async
    public void handleCancelPartialOrderCommand(CancelPartialOrderEvent command) {
        logger.info("📥 Received CancelPartialOrderCommand from Market Engine - Command ID: {}, Order ID: {}, Quantity: {}, Reason: {}",
                command.getCommandId(), command.getOrderId(), command.getQuantityToCancel(), command.getReason());

        try {
            validateEventSource(command);

            var result = orderService.cancelPartialOrder(
                    command.getOrderId(),
                    command.getQuantityToCancel()
            );

            if (result.isSuccess()) {
                logger.info("✅ Successfully partially cancelled order - Command ID: {}, Order ID: {}",
                        command.getCommandId(), command.getOrderId());
            } else {
                logger.error("❌ Failed to partially cancel order - Command ID: {}, Order ID: {}, Errors: {}",
                        command.getCommandId(), command.getOrderId(), result.getErrors());
            }

        } catch (Exception e) {
            logger.error("💥 Failed to process CancelPartialOrderCommand - Command ID: {}, Error: {}",
                    command.getCommandId(), e.getMessage(), e);
        }
    }

    // ===== ORDER UPDATE EVENTS =====

    @EventListener
    @Async
    public void handleUpdateOrderPriceCommand(UpdateOrderPriceEvent command) {
        logger.info("📥 Received UpdateOrderPriceCommand from Market Engine - Command ID: {}, Order ID: {}, New Price: {} {}",
                command.getCommandId(), command.getOrderId(), command.getNewPrice(), command.getCurrency());

        try {
            validateEventSource(command);

            Money newPrice = Money.of(command.getNewPrice(), command.getCurrency());

            var result = orderService.updateOrderPrice(
                    command.getOrderId(),
                    newPrice
            );

            if (result.isSuccess()) {
                logger.info("✅ Successfully updated order price - Command ID: {}, Order ID: {}",
                        command.getCommandId(), command.getOrderId());
            } else {
                logger.error("❌ Failed to update order price - Command ID: {}, Order ID: {}, Errors: {}",
                        command.getCommandId(), command.getOrderId(), result.getErrors());
            }

        } catch (Exception e) {
            logger.error("💥 Failed to process UpdateOrderPriceCommand - Command ID: {}, Error: {}",
                    command.getCommandId(), e.getMessage(), e);
        }
    }

    // ===== TRANSACTION EVENTS =====

    @EventListener
    @Async
    public void handleCreateTransactionCommand(CreateTransactionEvent command) {
        logger.info("📥 Received CreateTransactionCommand from Market Engine - Command ID: {}, Buy Order: {}, Sell Order: {}, Quantity: {}",
                command.getCommandId(), command.getBuyOrderId(), command.getSellOrderId(), command.getQuantity());

        try {
            validateEventSource(command);

            Money executionPrice = Money.of(command.getExecutionPrice(), command.getCurrency());

            var result = transactionService.createTransactionByOrderIds(
                    command.getBuyOrderId(),
                    command.getSellOrderId(),
                    executionPrice,
                    command.getQuantity()
            );

            if (result.isSuccess()) {
                logger.info("✅ Successfully created transaction - Command ID: {}, Transaction ID: {}",
                        command.getCommandId(), result.getTransactionId());
            } else {
                logger.error("❌ Failed to create transaction - Command ID: {}, Errors: {}",
                        command.getCommandId(), result.getErrors());
            }

        } catch (Exception e) {
            logger.error("💥 Failed to process CreateTransactionCommand - Command ID: {}, Error: {}",
                    command.getCommandId(), e.getMessage(), e);
        }
    }

    // ===== HELPER METHODS =====

    /**
     * Validates that the event is from an authorized source (Market Engine).
     * This is where you would add authentication/authorization logic.
     */
    private void validateEventSource(Object event) {
        // TODO: Implement actual validation logic
        // Examples:
        // - Check event signature
        // - Verify source bounded context
        // - Validate timestamp is recent
        // - Check event hasn't been processed already (idempotency)

        // For now, we trust all events (development mode)
        logger.debug("Event source validation passed for: {}", event.getClass().getSimpleName());
    }

    /**
     * Creates a Symbol from a symbol code.
     * This could be enhanced to support more symbols dynamically.
     */
    private Symbol createSymbol(String symbolCode) {
        try {
            return switch (symbolCode.toUpperCase()) {
                case "BTC" -> Symbol.btcUsd();
                case "ETH" -> Symbol.ethUsd();
                case "EURUSD" -> Symbol.eurUsd();
                case "GBPUSD" -> Symbol.gbpUsd();
                default -> {
                    logger.warn("Unknown symbol code: {}, creating generic symbol", symbolCode);
                    throw new IllegalArgumentException("Unsupported symbol: " + symbolCode);
                }
            };
        } catch (Exception e) {
            logger.error("Failed to create symbol for code: {}", symbolCode, e);
            throw new IllegalArgumentException("Invalid symbol: " + symbolCode, e);
        }
    }
}