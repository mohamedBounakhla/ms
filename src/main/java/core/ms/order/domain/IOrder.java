package core.ms.order.domain;

import core.ms.order.domain.value.OrderStatus;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface IOrder {
    String getId();
    Symbol getSymbol();
    Money getPrice();
    BigDecimal getQuantity();
    OrderStatus getStatus();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();

    void cancel();
    void cancelPartial();
    void fillPartial();
    void complete();
    void updatePrice(Money price);
    Money getTotalValue();
    boolean isActive();

    // ===== NEW: QUANTITY TRACKING METHODS =====
    BigDecimal getExecutedQuantity();
    BigDecimal getRemainingQuantity();
    List<ITransaction> getTransactions();
    int getTransactionSequence(ITransaction transaction);

    // ===== NEW: INTERNAL METHOD FOR TRANSACTION UPDATES =====
    void addTransaction(ITransaction transaction, BigDecimal executedQuantity);
}