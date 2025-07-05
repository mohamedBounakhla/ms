package core.ms.order.domain;

import core.ms.order.domain.value.OrderStatus;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface IOrder {
    String getId();
    Symbol getSymbol();
    Money getPrice();
    BigDecimal getQuantity();
    OrderStatus getStatus(); // This is now our State Pattern OrderStatus
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();

    void cancel();
    void cancelPartial();
    void fillPartial();
    void complete();
    void updatePrice(Money price);
    Money getTotalValue();
    boolean isActive();
}