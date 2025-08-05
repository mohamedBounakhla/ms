package core.ms.order_book.domain.value_object;

import core.ms.order.domain.entities.IOrder;
import core.ms.shared.money.Money;

import java.time.LocalDateTime;

public interface IPriorityCalculator<T extends IOrder> {
    boolean isHigherPriority(T order1, T order2);
    boolean isPriceBetter(Money price1, Money price2);
    boolean isTimeBetter(LocalDateTime time1, LocalDateTime time2);
    Money calculatePriceDifference(Money price1, Money price2);
    boolean hasSamePrice(T order1, T order2);
}