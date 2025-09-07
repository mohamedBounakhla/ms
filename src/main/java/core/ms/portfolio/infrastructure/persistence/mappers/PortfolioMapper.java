package core.ms.portfolio.infrastructure.persistence.mappers;

import core.ms.portfolio.domain.Portfolio;
import core.ms.portfolio.domain.cash.CashManager;
import core.ms.portfolio.domain.positions.PositionManager;
import core.ms.portfolio.infrastructure.persistence.entities.CashBalanceEntity;
import core.ms.portfolio.infrastructure.persistence.entities.PortfolioEntity;
import core.ms.portfolio.infrastructure.persistence.entities.PositionEntity;
import core.ms.portfolio.infrastructure.persistence.entities.ReservationEntity;
import core.ms.shared.OrderType;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class PortfolioMapper {

    public PortfolioEntity toEntity(Portfolio portfolio, PortfolioEntity existingEntity) {
        PortfolioEntity entity = existingEntity != null ? existingEntity :
                new PortfolioEntity(portfolio.getPortfolioId(), portfolio.getOwnerId());

        // Update cash balances
        for (var currency : core.ms.shared.money.Currency.values()) {
            Money total = portfolio.getTotalCash(currency);
            Money reserved = portfolio.getReservedCash(currency);

            // Process if there's any cash (total or reserved) for this currency
            if (total.isPositive() || reserved.isPositive()) {
                CashBalanceEntity cashBalance = entity.getCashBalances().stream()
                        .filter(cb -> cb.getCurrency() == currency)
                        .findFirst()
                        .orElseGet(() -> {
                            CashBalanceEntity newCb = new CashBalanceEntity(currency, total.getAmount());
                            entity.addCashBalance(newCb);
                            return newCb;
                        });

                cashBalance.setBalance(total.getAmount());
                cashBalance.setReservedAmount(reserved.getAmount());
                cashBalance.setUpdatedAt(LocalDateTime.now());
            }
        }

        // Update positions - NEW SECTION
        Set<Symbol> symbols = portfolio.getPositionSymbols();
        for (Symbol symbol : symbols) {
            BigDecimal quantity = portfolio.getTotalAssets(symbol);
            System.out.println("DEDOUG PortfolioMapper - Symbol: " + symbol.getCode() + ", Quantity to save: " + quantity);

            if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                PositionEntity position = entity.getPositions().stream()
                        .filter(p -> p.getSymbolCode().equals(symbol.getCode()))
                        .findFirst()
                        .orElseGet(() -> {
                            PositionEntity newPos = new PositionEntity(
                                    symbol.getCode(),
                                    quantity,
                                    BigDecimal.ZERO,  // average cost
                                    symbol.getQuoteCurrency().name()
                            );
                            entity.addPosition(newPos);
                            return newPos;
                        });

                position.setQuantity(quantity);
                position.setReservedQuantity(portfolio.getReservedAssets(symbol));
                position.setUpdatedAt(LocalDateTime.now());
            }
        }

        // Update reservations
        for (ReservationEntity reservation : portfolio.getReservations()) {
            if (reservation.getPortfolio() == null) {
                reservation.setPortfolio(entity);
            }
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
            if (amount.isPositive()) {
                cashManager.deposit(amount);
            }
        }

        // Reconstruct positions
        for (PositionEntity positionEntity : entity.getPositions()) {
            Symbol symbol = Symbol.createFromCode(positionEntity.getSymbolCode());
            positionManager.setPosition(symbol, positionEntity.getQuantity());
        }

        // FIX: Ensure reservations are properly loaded
        List<ReservationEntity> reservations = new ArrayList<>(entity.getReservations());

        // Create a map for quick lookup in Portfolio
        Map<String, ReservationEntity> reservationMap = new HashMap<>();
        for (ReservationEntity reservation : reservations) {
            reservationMap.put(reservation.getReservationId(), reservation);
        }

        // Reconstruct internal reservations in managers
        for (ReservationEntity reservation : reservations) {
            if (reservation.isActive()) {
                if (reservation.getOrderType() == OrderType.BUY &&
                        reservation.getAmount() != null && reservation.getCurrency() != null) {
                    Money amount = Money.of(reservation.getAmount(), reservation.getCurrency());
                    if (reservation.getStatus() != ReservationEntity.ReservationStatus.EXECUTED) {
                        try {
                            cashManager.createInternalReservation(reservation.getReservationId(), amount);
                        } catch (Exception e) {
                            // Ignore if can't recreate
                        }
                    }
                } else if (reservation.getOrderType() == OrderType.SELL &&
                        reservation.getQuantity() != null && reservation.getSymbolCode() != null) {
                    Symbol symbol = Symbol.createFromCode(reservation.getSymbolCode());
                    if (reservation.getStatus() != ReservationEntity.ReservationStatus.EXECUTED) {
                        try {
                            positionManager.createInternalReservation(reservation.getReservationId(),
                                    symbol, reservation.getQuantity());
                        } catch (Exception e) {
                            // Ignore if can't recreate
                        }
                    }
                }
            }
        }

        // FIX: Pass the reservation map to Portfolio constructor
        return new Portfolio(
                entity.getPortfolioId(),
                entity.getOwnerId(),
                cashManager,
                positionManager,
                reservationMap  // Pass map instead of list
        );
    }
}