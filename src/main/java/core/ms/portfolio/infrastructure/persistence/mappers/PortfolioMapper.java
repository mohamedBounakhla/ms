package core.ms.portfolio.infrastructure.persistence.mappers;

import core.ms.portfolio.domain.Portfolio;
import core.ms.portfolio.domain.cash.CashManager;
import core.ms.portfolio.domain.positions.PositionManager;
import core.ms.portfolio.infrastructure.persistence.entities.CashBalanceEntity;
import core.ms.portfolio.infrastructure.persistence.entities.PortfolioEntity;
import core.ms.portfolio.infrastructure.persistence.entities.PositionEntity;
import core.ms.shared.money.Money;
import org.springframework.stereotype.Component;

@Component
public class PortfolioMapper {

    public PortfolioEntity toEntity(Portfolio portfolio) {
        PortfolioEntity entity = new PortfolioEntity(
                portfolio.getPortfolioId(),
                portfolio.getOwnerId()
        );

        // Map cash balances
        for (var currency : core.ms.shared.money.Currency.values()) {
            Money total = portfolio.getTotalCash(currency);
            if (total.isPositive()) {
                CashBalanceEntity cashBalance = new CashBalanceEntity(currency, total.getAmount());
                Money reserved = portfolio.getReservedCash(currency);
                cashBalance.setReservedAmount(reserved.getAmount());
                entity.addCashBalance(cashBalance);
            }
        }

        // Map positions (simplified - would need to track all symbols)
        // This would need enhancement to properly track all positions

        return entity;
    }

    public Portfolio toDomain(PortfolioEntity entity) {
        CashManager cashManager = new CashManager();
        PositionManager positionManager = new PositionManager();

        // Reconstruct cash balances
        for (CashBalanceEntity cashBalanceEntity : entity.getCashBalances()) {
            Money amount = Money.of(cashBalanceEntity.getBalance(), cashBalanceEntity.getCurrency());
            cashManager.deposit(amount);
            // Note: Reserved amounts would need to be reconstructed from reservation entities
        }

        // Reconstruct positions
        for (PositionEntity positionEntity : entity.getPositions()) {
            // This would need proper symbol reconstruction and position management
            // Simplified for demonstration
        }

        return new Portfolio(
                entity.getPortfolioId(),
                entity.getOwnerId(),
                cashManager,
                positionManager
        );
    }
}