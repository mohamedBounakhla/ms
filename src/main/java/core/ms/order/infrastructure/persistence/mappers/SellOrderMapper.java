package core.ms.order.infrastructure.persistence.mappers;

import core.ms.order.domain.entities.SellOrder;
import core.ms.order.infrastructure.persistence.entities.SellOrderEntity;
import core.ms.shared.domain.AssetType;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.springframework.stereotype.Component;


@Component
public class SellOrderMapper {

    public SellOrderEntity fromDomain(SellOrder order) {
        return new SellOrderEntity(
                order.getId(),
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

    public SellOrder toDomain(SellOrderEntity entity) {
        Symbol symbol = reconstructSymbol(entity.getSymbolCode(), entity.getSymbolName(), entity.getCurrency());
        Money price = Money.of(entity.getPrice(), entity.getCurrency());

        SellOrder order = new SellOrder(entity.getId(), symbol, price, entity.getQuantity());

        // Reconstruct state
        order.setExecutedQuantity(entity.getExecutedQuantity());
        reconstructOrderState(order, entity);

        return order;
    }

    private void reconstructOrderState(SellOrder order, SellOrderEntity entity) {
        switch (entity.getStatus()) {
            case PARTIAL -> order.fillPartial();
            case FILLED -> order.complete();
            case CANCELLED -> order.cancel();
            // PENDING is default
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
