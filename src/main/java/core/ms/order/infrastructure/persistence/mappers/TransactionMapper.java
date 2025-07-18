package core.ms.order.infrastructure.persistence.mappers;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order.domain.entities.Transaction;
import core.ms.order.infrastructure.persistence.entities.TransactionEntity;
import core.ms.shared.domain.AssetType;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionEntity fromDomain(Transaction transaction) {
        return new TransactionEntity(
                transaction.getId(),
                transaction.getSymbol().getCode(),
                transaction.getSymbol().getName(),
                transaction.getBuyOrder().getId(),
                transaction.getSellOrder().getId(),
                transaction.getPrice().getAmount(),
                transaction.getPrice().getCurrency(),
                transaction.getQuantity(),
                transaction.getTotalValue().getAmount(),
                transaction.getCreatedAt()
        );
    }

    public Transaction toDomain(TransactionEntity entity, IBuyOrder buyOrder, ISellOrder sellOrder) {
        Symbol symbol = reconstructSymbol(entity.getSymbolCode(), entity.getSymbolName(), entity.getCurrency());
        Money price = Money.of(entity.getPrice(), entity.getCurrency());

        return new Transaction(
                entity.getId(),
                symbol,
                buyOrder,
                sellOrder,
                price,
                entity.getQuantity()
        );
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