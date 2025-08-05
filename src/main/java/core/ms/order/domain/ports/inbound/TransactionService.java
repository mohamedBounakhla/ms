package core.ms.order.domain.ports.inbound;

import core.ms.order.application.dto.query.TransactionResultDTO;
import core.ms.order.application.dto.query.TransactionStatisticsDTO;
import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order.domain.entities.ITransaction;
import core.ms.shared.money.Symbol;
import core.ms.shared.money.Money;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for Transaction domain operations.
 * Handles all transaction-related use cases.
 */
public interface TransactionService {

    // ===== TRANSACTION CREATION =====
    /**
     * Creates a transaction from matching orders
     */
    TransactionResultDTO createTransaction(IBuyOrder buyOrder, ISellOrder sellOrder,
                                           Money executionPrice, BigDecimal quantity);

    /**
     * Creates a transaction using order IDs
     */
    TransactionResultDTO createTransactionByOrderIds(String buyOrderId, String sellOrderId,
                                                     Money executionPrice, BigDecimal quantity);

    // ===== TRANSACTION QUERIES =====
    /**
     * Finds a transaction by its ID
     */
    Optional<ITransaction> findTransactionById(String transactionId);

    /**
     * Finds all transactions for a specific order
     */
    List<ITransaction> findTransactionsByOrderId(String orderId);

    /**
     * Finds all transactions for a specific symbol
     */
    List<ITransaction> findTransactionsBySymbol(Symbol symbol);

    /**
     * Finds all transactions for a specific user
     */
    List<ITransaction> findTransactionsByUserId(String userId);

    /**
     * Finds transactions within a date range
     */
    List<ITransaction> findTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Finds transactions by price range
     */
    List<ITransaction> findTransactionsByPriceRange(Money minPrice, Money maxPrice);

    // ===== TRANSACTION ANALYTICS =====
    /**
     * Gets transaction statistics for a symbol
     */
    TransactionStatisticsDTO getTransactionStatistics(Symbol symbol);
}