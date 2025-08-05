package core.ms.order.domain.ports.outbound;

import core.ms.order.domain.entities.ITransaction;
import core.ms.shared.money.Symbol;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    // Basic CRUD
    ITransaction save(ITransaction transaction);
    Optional<ITransaction> findById(String transactionId);
    void deleteById(String transactionId);
    boolean existsById(String transactionId);

    // Simple queries
    List<ITransaction> findByOrderId(String orderId);
    List<ITransaction> findBySymbol(Symbol symbol);
    List<ITransaction> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    // Bulk operations
    List<ITransaction> findAll();
    long count();
}