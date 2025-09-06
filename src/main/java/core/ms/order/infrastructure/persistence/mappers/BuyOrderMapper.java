package core.ms.order.infrastructure.persistence.mappers;

import core.ms.order.domain.entities.BuyOrder;
import core.ms.order.domain.factories.OrderFactory;
import core.ms.order.infrastructure.persistence.entities.BuyOrderEntity;
import core.ms.shared.money.AssetType;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.springframework.stereotype.Component;

@Component
public class BuyOrderMapper {

    public BuyOrderEntity fromDomain(BuyOrder order) {
        return new BuyOrderEntity(
                order.getId(),
                order.getPortfolioId(),
                order.getReservationId(),
                order.getSymbol().getCode(),
                order.getSymbol().getName(),
                order.getPrice().getAmount(),
                order.getPrice().getCurrency(),
                order.getQuantity(),
                order.getStatus().getStatus(),
                order.getExecutedQuantity(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public BuyOrder toDomain(BuyOrderEntity entity) {
        try {
            Symbol symbol = reconstructSymbol(entity.getSymbolCode(), entity.getSymbolName(), entity.getCurrency());
            Money price = Money.of(entity.getPrice(), entity.getCurrency());

            String portfolioId = entity.getPortfolioId();
            String reservationId = entity.getReservationId();

            // Create order with factory
            BuyOrder order = OrderFactory.createBuyOrderWithId(
                    entity.getId(),
                    portfolioId,
                    reservationId,
                    symbol,
                    price,
                    entity.getQuantity()
            );

            // Set executed quantity WITHOUT changing status
            if (entity.getExecutedQuantity() != null && entity.getExecutedQuantity().signum() > 0) {
                order.setExecutedQuantity(entity.getExecutedQuantity());
            }

            // Only reconstruct state if it's different from default PENDING
            if (entity.getStatus() != null && entity.getStatus() != core.ms.order.domain.value_objects.OrderStatusEnum.PENDING) {
                reconstructOrderState(order, entity);
            }

            return order;

        } catch (OrderFactory.OrderCreationException e) {
            throw new IllegalStateException("Failed to reconstruct BuyOrder from persistence: " + e.getMessage(), e);
        }
    }

    private void reconstructOrderState(BuyOrder order, BuyOrderEntity entity) {
        // Only change state if necessary, avoid redundant state transitions
        switch (entity.getStatus()) {
            case PARTIAL -> {
                // Only transition if not already partial
                if (order.getStatus().getStatus() != core.ms.order.domain.value_objects.OrderStatusEnum.PARTIAL) {
                    order.fillPartial();
                }
            }
            case FILLED -> {
                // Only transition if not already filled
                if (order.getStatus().getStatus() != core.ms.order.domain.value_objects.OrderStatusEnum.FILLED) {
                    order.complete();
                }
            }
            case CANCELLED -> {
                // Only transition if not already cancelled
                if (order.getStatus().getStatus() != core.ms.order.domain.value_objects.OrderStatusEnum.CANCELLED) {
                    order.cancel();
                }
            }
            // PENDING is default state, no action needed
        }
    }

    private Symbol reconstructSymbol(String code, String name, Currency quoteCurrency) {
        return switch (code.toUpperCase()) {
            case "BTC" -> quoteCurrency == Currency.USD ? Symbol.btcUsd() : Symbol.btcEur();
            case "ETH" -> Symbol.ethUsd();
            case "EURUSD" -> Symbol.eurUsd();
            case "GBPUSD" -> Symbol.gbpUsd();
            default -> new Symbol(code, name, AssetType.STOCK, Currency.USD, quoteCurrency);
        };
    }
}