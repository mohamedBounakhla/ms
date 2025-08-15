package core.ms.portfolio.domain.ports.inbound;

import core.ms.portfolio.domain.Portfolio;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.util.Optional;

public interface PortfolioService {

    // ===== PORTFOLIO MANAGEMENT =====
    Portfolio createPortfolio(String portfolioId, String ownerId);
    Optional<Portfolio> findPortfolioById(String portfolioId);
    Optional<Portfolio> findPortfolioByOwnerId(String ownerId);
    void deletePortfolio(String portfolioId);

    // ===== CASH OPERATIONS =====
    PortfolioOperationResult depositCash(String portfolioId, Money amount);
    PortfolioOperationResult withdrawCash(String portfolioId, Money amount);
    Money getAvailableCash(String portfolioId, Currency currency);
    Money getTotalCash(String portfolioId, Currency currency);

    // ===== POSITION OPERATIONS =====
    BigDecimal getAvailableAssets(String portfolioId, Symbol symbol);
    BigDecimal getTotalAssets(String portfolioId, Symbol symbol);
    PortfolioSnapshot getPortfolioSnapshot(String portfolioId);

    // ===== ORDER OPERATIONS =====
    OrderReservationResult placeBuyOrder(String portfolioId, String orderId,
                                         Symbol symbol, Money price, BigDecimal quantity);
    OrderReservationResult placeSellOrder(String portfolioId, String orderId,
                                          Symbol symbol, Money price, BigDecimal quantity);

    // ===== MAINTENANCE =====
    void cleanupExpiredReservations();
    int getActiveReservationsCount(String portfolioId);
}