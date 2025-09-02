package core.ms.order.domain.ports.inbound;

import core.ms.order.domain.entities.ITransaction;
import core.ms.shared.money.Symbol;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for Transaction domain operations.
 * Handles all transaction-related use cases.
 */
public interface TransactionService {

    // ===== QUERY OPERATIONS (For Internal Use/Monitoring) =====

    /**
     * Finds a transaction by its ID
     */
    Optional<ITransaction> findTransactionById(String transactionId);

    /**
     * Finds all transactions for a specific order
     */
    List<ITransaction> findTransactionsByOrderId(String orderId);

    /**
     * Finds all transactions for a specific portfolio
     */
    List<ITransaction> findTransactionsByPortfolioId(String portfolioId);

    /**
     * Finds all transactions for a specific symbol
     */
    List<ITransaction> findTransactionsBySymbol(Symbol symbol);

    /**
     * Finds transactions within a date range
     */
    List<ITransaction> findTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get total count of transactions
     */
    long getTotalTransactionCount();

    /**
     * Get transaction volume for a symbol
     */
    java.math.BigDecimal getTransactionVolume(Symbol symbol);
}