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
            // Reconstruct domain objects
            Symbol symbol = reconstructSymbol(entity.getSymbolCode(), entity.getSymbolName(), entity.getCurrency());
            Money price = Money.of(entity.getPrice(), entity.getCurrency());

            // TODO: Get portfolioId and reservationId from entity (need to add these fields to entity)
            String portfolioId = entity.getPortfolioId(); // Assuming these fields are added to entity
            String reservationId = entity.getReservationId();

            // Use factory to create the order with proper validation - NOW WITH portfolioId and reservationId
            BuyOrder order = OrderFactory.createBuyOrderWithId(
                    entity.getId(),
                    portfolioId,
                    reservationId,
                    symbol,
                    price,
                    entity.getQuantity()
            );

            // Reconstruct the persisted state (executed quantity and status)
            order.setExecutedQuantity(entity.getExecutedQuantity());
            reconstructOrderState(order, entity);

            return order;

        } catch (OrderFactory.OrderCreationException e) {
            throw new IllegalStateException("Failed to reconstruct BuyOrder from persistence: " + e.getMessage(), e);
        }
    }

    private void reconstructOrderState(BuyOrder order, BuyOrderEntity entity) {
        switch (entity.getStatus()) {
            case PARTIAL -> order.fillPartial();
            case FILLED -> order.complete();
            case CANCELLED -> order.cancel();
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