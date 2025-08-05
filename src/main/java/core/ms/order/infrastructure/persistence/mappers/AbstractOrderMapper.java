package core.ms.order.infrastructure.persistence.mappers;

import core.ms.order.domain.entities.IOrder;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.order.infrastructure.persistence.entities.AbstractOrderEntity;
import core.ms.shared.money.AssetType;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;

public abstract class AbstractOrderMapper <T extends IOrder, E extends AbstractOrderEntity>{
    /**
     * Common mapping from domain to entity
     */
    protected void mapCommonFields(T order, E entity) {
        entity.setId(order.getId());
        entity.setSymbolCode(order.getSymbol().getCode());
        entity.setSymbolName(order.getSymbol().getName());
        entity.setPrice(order.getPrice().getAmount());
        entity.setCurrency(order.getPrice().getCurrency());
        entity.setQuantity(order.getQuantity());
        entity.setStatus(order.getStatus().getStatus()); // Extract enum from state pattern
        entity.setExecutedQuantity(order.getExecutedQuantity());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setUpdatedAt(order.getUpdatedAt());
    }

    /**
     * Common mapping from entity to domain fields
     */
    protected void reconstructCommonState(T order, E entity) {
        // Reconstruct the order state from persisted data
        reconstructOrderState(order, entity.getStatus(), entity.getExecutedQuantity());
    }

    /**
     * Reconstructs the domain order state from persisted data
     * This handles the state pattern complexity
     */
    protected void reconstructOrderState(T order, OrderStatusEnum status, BigDecimal executedQuantity) {
        // If there's executed quantity, we need to simulate the partial execution
        if (executedQuantity.compareTo(BigDecimal.ZERO) > 0) {
            if (status == OrderStatusEnum.PARTIAL) {
                order.fillPartial();
            } else if (status == OrderStatusEnum.FILLED) {
                order.complete();
            }
        } else {
            // No execution, just set the final status
            switch (status) {
                case CANCELLED -> order.cancel();
                case FILLED -> order.complete();
                // PENDING is default state
            }
        }
    }

    /**
     * Reconstructs Symbol from persisted data
     * Common logic for all order types
     */
    protected Symbol reconstructSymbol(String code, String name, Currency quoteCurrency) {
        // Enhanced symbol reconstruction based on your Symbol factory methods
        return switch (code.toUpperCase()) {
            case "BTC" -> quoteCurrency == Currency.USD ? Symbol.btcUsd() : Symbol.btcEur();
            case "ETH" -> Symbol.ethUsd();
            case "EURUSD" -> Symbol.eurUsd();
            case "GBPUSD" -> Symbol.gbpUsd();
            default -> {
                // Fallback - determine asset type based on currency
                AssetType assetType = quoteCurrency.isCrypto() ? AssetType.CRYPTO :
                        quoteCurrency.isFiat() ? AssetType.FOREX : AssetType.STOCK;
                Currency baseCurrency = determineBasseCurrency(code, quoteCurrency);
                yield new Symbol(code, name, assetType, baseCurrency, quoteCurrency);
            }
        };
    }

    /**
     * Helper method to determine base currency for unknown symbols
     */
    private Currency determineBasseCurrency(String code, Currency quoteCurrency) {
        // Simple heuristic - you might want to enhance this
        if (code.contains("BTC")) return Currency.BTC;
        if (code.contains("ETH")) return Currency.ETH;
        if (code.contains("USD")) return Currency.USD;
        if (code.contains("EUR")) return Currency.EUR;
        if (code.contains("GBP")) return Currency.GBP;

        // Default fallback
        return quoteCurrency == Currency.USD ? Currency.EUR : Currency.USD;
    }

    public abstract E fromDomain(T order);

    public abstract T toDomain(E entity);
}
