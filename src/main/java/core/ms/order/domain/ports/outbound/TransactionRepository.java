package core.ms.order.domain.ports.outbound;

import core.ms.order.domain.entities.ITransaction;
import core.ms.shared.money.Symbol;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    // Basic CRUD with locking support
    ITransaction save(ITransaction transaction);
    ITransaction saveAndFlush(ITransaction transaction);
    Optional<ITransaction> findById(String transactionId);
    Optional<ITransaction> findByIdWithLock(String transactionId, LockModeType lockMode);
    void deleteById(String transactionId);
    boolean existsById(String transactionId);
    void flush();

    // Query methods
    List<ITransaction> findByOrderId(String orderId);
    List<ITransaction> findBySymbol(Symbol symbol);
    List<ITransaction> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    // Check for duplicate transactions (idempotency)
    boolean existsByBuyOrderIdAndSellOrderId(String buyOrderId, String sellOrderId);
    Optional<ITransaction> findByBuyOrderIdAndSellOrderId(String buyOrderId, String sellOrderId);

    // Bulk operations
    List<ITransaction> findAll();
    long count();
}