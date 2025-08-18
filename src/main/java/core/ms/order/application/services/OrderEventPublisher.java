package core.ms.order.application.services;

import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.entities.ITransaction;
import core.ms.order.domain.events.publish.*;
import core.ms.shared.events.EventBus;
import core.ms.shared.money.Money;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class OrderEventPublisher {

    @Autowired
    private EventBus eventBus;

    public void publishOrderCreated(IOrder order, String orderType) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                order.getPortfolioId(),
                order.getReservationId(),
                orderType,
                order.getSymbol(),
                order.getPrice(),
                order.getQuantity(),
                order.getStatus().getStatus().name()
        );
        eventBus.publish(event);
    }

    public void publishOrderCancelled(IOrder order, String orderType, String reason) {
        OrderCancelledEvent event = new OrderCancelledEvent(
                order.getId(),
                order.getPortfolioId(),
                order.getReservationId(),
                orderType,
                order.getSymbol(),
                order.getQuantity().subtract(order.getExecutedQuantity()),
                BigDecimal.ZERO,
                reason
        );
        eventBus.publish(event);
    }

    public void publishOrderPartialCancelled(IOrder order, String orderType,
                                             BigDecimal cancelledQuantity, String reason) {
        OrderCancelledEvent event = new OrderCancelledEvent(
                order.getId(),
                order.getPortfolioId(),
                order.getReservationId(),
                orderType,
                order.getSymbol(),
                cancelledQuantity,
                order.getRemainingQuantity(),
                reason
        );
        eventBus.publish(event);
    }

    public void publishOrderUpdated(IOrder order, Money oldPrice, Money newPrice) {
        OrderUpdatedEvent event = new OrderUpdatedEvent(
                order.getId(),
                order.getPortfolioId(),
                "PRICE_UPDATED",
                order.getSymbol(),
                oldPrice,
                newPrice
        );
        eventBus.publish(event);
    }

    public void publishOrderPartiallyFilled(IOrder order, String orderType,
                                            BigDecimal filledQuantity, Money executionPrice) {
        OrderPartiallyFilledEvent event = new OrderPartiallyFilledEvent(
                order.getId(),
                order.getPortfolioId(),
                orderType,
                order.getSymbol(),
                filledQuantity,
                order.getExecutedQuantity(),
                order.getRemainingQuantity(),
                executionPrice
        );
        eventBus.publish(event);
    }

    public void publishOrderFilled(IOrder order, String orderType, Money averagePrice) {
        OrderFilledEvent event = new OrderFilledEvent(
                order.getId(),
                order.getPortfolioId(),
                orderType,
                order.getSymbol(),
                order.getExecutedQuantity(),
                averagePrice
        );
        eventBus.publish(event);
    }

    public void publishTransactionCreated(ITransaction transaction) {
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                transaction.getId(),
                transaction.getBuyOrder().getId(),
                transaction.getSellOrder().getId(),
                transaction.getBuyOrder().getPortfolioId(),
                transaction.getSellOrder().getPortfolioId(),
                transaction.getBuyOrder().getReservationId(),
                transaction.getSellOrder().getReservationId(),
                transaction.getSymbol(),
                transaction.getQuantity(),
                transaction.getPrice()
        );
        eventBus.publish(event);
    }
}