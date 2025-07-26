package core.ms.order.infrastructure.persistence.mappers;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order.domain.entities.Transaction;
import core.ms.order.domain.factories.TransactionFactory;
import java.math.BigDecimal;
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
        try {
            // Reconstruct domain objects
            Money executionPrice = Money.of(entity.getPrice(), entity.getCurrency());

            // Use TransactionFactory for proper domain object creation
            Transaction transaction = TransactionFactory.create(
                    buyOrder,
                    sellOrder,
                    entity.getQuantity()
            );

            return transaction;

        } catch (TransactionFactory.TransactionCreationException e) {
            throw new IllegalStateException("Failed to reconstruct Transaction from persistence: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to reconstruct Transaction from persistence: " + e.getMessage(), e);
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