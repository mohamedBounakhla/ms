package core.ms.portfolio.infrastructure.persistence.mappers;

import core.ms.portfolio.domain.Portfolio;
import core.ms.portfolio.domain.cash.CashManager;
import core.ms.portfolio.domain.positions.PositionManager;
import core.ms.portfolio.infrastructure.persistence.entities.CashBalanceEntity;
import core.ms.portfolio.infrastructure.persistence.entities.PortfolioEntity;
import core.ms.portfolio.infrastructure.persistence.entities.PositionEntity;
import core.ms.portfolio.infrastructure.persistence.entities.ReservationEntity;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class PortfolioMapper {

    public PortfolioEntity toEntity(Portfolio portfolio, PortfolioEntity existingEntity) {
        PortfolioEntity entity = existingEntity != null ? existingEntity :
                new PortfolioEntity(portfolio.getPortfolioId(), portfolio.getOwnerId());

        // Update cash balances
        for (var currency : core.ms.shared.money.Currency.values()) {
            Money total = portfolio.getTotalCash(currency);
            if (total.isPositive()) {
                CashBalanceEntity cashBalance = entity.getCashBalances().stream()
                        .filter(cb -> cb.getCurrency() == currency)
                        .findFirst()
                        .orElseGet(() -> {
                            CashBalanceEntity newCb = new CashBalanceEntity(currency, total.getAmount());
                            entity.addCashBalance(newCb);
                            return newCb;
                        });

                Money reserved = portfolio.getReservedCash(currency);
                cashBalance.setBalance(total.getAmount());
                cashBalance.setReservedAmount(reserved.getAmount());
                cashBalance.setUpdatedAt(LocalDateTime.now());
            }
        }

        // Update reservations
        for (ReservationEntity reservation : portfolio.getReservations()) {
            if (!entity.getReservations().contains(reservation)) {
                entity.addReservation(reservation);
            }
        }

        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    public Portfolio toDomain(PortfolioEntity entity) {
        CashManager cashManager = new CashManager();
        PositionManager positionManager = new PositionManager();

        // Reconstruct cash balances
        for (CashBalanceEntity cashBalanceEntity : entity.getCashBalances()) {
            Money amount = Money.of(cashBalanceEntity.getBalance(), cashBalanceEntity.getCurrency());
            cashManager.deposit(amount);

            // Reconstruct cash reservations
            Money reserved = Money.of(cashBalanceEntity.getReservedAmount(), cashBalanceEntity.getCurrency());
            if (reserved.isPositive()) {
                // Note: We need to track which reservations are for cash
                // This is handled by the reservation entities
            }
        }

        // Reconstruct positions
        for (PositionEntity positionEntity : entity.getPositions()) {
            Symbol symbol = Symbol.createFromCode(positionEntity.getSymbolCode());
            positionManager.deposit(symbol, positionEntity.getQuantity());
        }

        // Load reservations
        List<ReservationEntity> reservations = new ArrayList<>(entity.getReservations());

        // Reconstruct internal reservations in managers
        for (ReservationEntity reservation : reservations) {
            if (reservation.isActive()) {
                if (reservation.getOrderType() == core.ms.shared.OrderType.BUY &&
                        reservation.getAmount() != null && reservation.getCurrency() != null) {
                    Money amount = Money.of(reservation.getAmount(), reservation.getCurrency());
                    cashManager.createInternalReservation(reservation.getReservationId(), amount);
                } else if (reservation.getOrderType() == core.ms.shared.OrderType.SELL &&
                        reservation.getQuantity() != null && reservation.getSymbolCode() != null) {
                    Symbol symbol = Symbol.createFromCode(reservation.getSymbolCode());
                    positionManager.createInternalReservation(reservation.getReservationId(),
                            symbol, reservation.getQuantity());
                }
            }
        }

        return new Portfolio(
                entity.getPortfolioId(),
                entity.getOwnerId(),
                cashManager,
                positionManager,
                reservations
        );
    }
}