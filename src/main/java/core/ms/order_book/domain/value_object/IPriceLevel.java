package core.ms.order_book.domain.value_object;

import core.ms.shared.domain.Money;

import java.math.BigDecimal;

public interface IPriceLevel {
    Money getPrice();
    BigDecimal getTotalQuantity();
    int getOrderCount();
    boolean isEmpty();
    boolean hasQuantity(BigDecimal quantity);
}