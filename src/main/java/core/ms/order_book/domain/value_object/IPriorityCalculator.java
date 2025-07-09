package core.ms.order_book.domain.value_object;

import core.ms.order.domain.IOrder;
import core.ms.shared.domain.Money;

import java.time.LocalDateTime;

public interface IPriorityCalculator<T extends IOrder> {
    boolean isHigherPriority(T order1, T order2);           // NOT compareOrders!
    boolean isPriceBetter(Money price1, Money price2);      // ✅ Same
    boolean isTimeBetter(LocalDateTime time1, LocalDateTime time2); // ✅ Same
    Money calculatePriceDifference(Money price1, Money price2);     // NOT comparePrices!
    boolean hasSamePrice(T order1, T order2);               // NEW method
}