package core.ms.order.domain.entities;

import core.ms.order.domain.value_objects.OrderStatus;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import core.ms.order.domain.value_objects.OrderStatus;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface IOrder {
    String getId();

    Symbol getSymbol();

    Money getPrice();

    BigDecimal getQuantity();

    OrderStatus getStatus();

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();

    // ===== STATUS OPERATIONS =====
    void cancel();

    void cancelPartial();

    void fillPartial();

    void complete();

    void updatePrice(Money price);

    // ===== BUSINESS LOGIC =====
    Money getTotalValue();

    boolean isActive();

    // ===== EXECUTION TRACKING (Internal Only) =====
    BigDecimal getExecutedQuantity();

    BigDecimal getRemainingQuantity();
    void updateExecution(BigDecimal executedAmount);

    /**
     * Sets the total executed quantity directly
     * Used for reconstruction from persistence
     */
    void setExecutedQuantity(BigDecimal executedQuantity);
}