package core.ms.order.application.event_handlers;

import core.ms.order.application.services.OrderApplicationService;
import core.ms.order.application.services.TransactionApplicationService;
import core.ms.order.domain.events.subscribe.CancelOrderEvent;
import core.ms.order.domain.events.subscribe.CreateOrderEvent;
import core.ms.order.domain.events.subscribe.CreateTransactionEvent;
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

    @EventListener
    @Async
    public void handleCreateOrderCommand(CreateOrderEvent command) {
        logger.info("Received CreateOrderCommand from Market Engine - ID: {}, Portfolio: {}, Type: {}",
                command.getCommandId(), command.getPortfolioId(), command.getOrderType());

        try {
            Symbol symbol = createSymbol(command.getSymbolCode());
            Money price = Money.of(command.getPrice(), command.getCurrency());

            if ("BUY".equals(command.getOrderType())) {
                orderService.createBuyOrder(
                        command.getPortfolioId(),
                        command.getReservationId(),
                        symbol,
                        price,
                        command.getQuantity()
                );
            } else if ("SELL".equals(command.getOrderType())) {
                orderService.createSellOrder(
                        command.getPortfolioId(),
                        command.getReservationId(),
                        symbol,
                        price,
                        command.getQuantity()
                );
            }

            logger.info("Successfully processed CreateOrderCommand: {}", command.getCommandId());

        } catch (Exception e) {
            logger.error("Failed to process CreateOrderCommand {}: {}", command.getCommandId(), e.getMessage(), e);
        }
    }

    @EventListener
    @Async
    public void handleCreateTransactionCommand(CreateTransactionEvent command) {
        logger.info("Received CreateTransactionCommand from Market Engine - ID: {}, Buy: {}, Sell: {}",
                command.getCommandId(), command.getBuyOrderId(), command.getSellOrderId());

        try {
            Money executionPrice = Money.of(command.getExecutionPrice(), command.getCurrency());

            transactionService.createTransactionByOrderIds(
                    command.getBuyOrderId(),
                    command.getSellOrderId(),
                    executionPrice,
                    command.getQuantity()
            );

            logger.info("Successfully processed CreateTransactionCommand: {}", command.getCommandId());

        } catch (Exception e) {
            logger.error("Failed to process CreateTransactionCommand {}: {}", command.getCommandId(), e.getMessage(), e);
        }
    }

    @EventListener
    @Async
    public void handleCancelOrderCommand(CancelOrderEvent command) {
        logger.info("Received CancelOrderCommand from Market Engine - ID: {}, Order: {}",
                command.getCommandId(), command.getOrderId());

        try {
            orderService.cancelOrder(command.getOrderId());

            logger.info("Successfully processed CancelOrderCommand: {}", command.getCommandId());

        } catch (Exception e) {
            logger.error("Failed to process CancelOrderCommand {}: {}", command.getCommandId(), e.getMessage(), e);
        }
    }

    private Symbol createSymbol(String symbolCode) {
        return switch (symbolCode.toUpperCase()) {
            case "BTC" -> Symbol.btcUsd();
            case "ETH" -> Symbol.ethUsd();
            case "EURUSD" -> Symbol.eurUsd();
            case "GBPUSD" -> Symbol.gbpUsd();
            default -> throw new IllegalArgumentException("Unsupported symbol: " + symbolCode);
        };
    }
}