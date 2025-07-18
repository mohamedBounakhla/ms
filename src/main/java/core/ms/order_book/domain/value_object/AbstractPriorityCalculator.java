package core.ms.order_book.domain.value_object;

import core.ms.order.domain.entities.IOrder;
import core.ms.shared.domain.Money;

import java.time.LocalDateTime;
import java.util.Objects;

public abstract class AbstractPriorityCalculator<T extends IOrder> implements IPriorityCalculator<T> {

    @Override
    public boolean isHigherPriority(T order1, T order2) {
        Objects.requireNonNull(order1, "First order cannot be null");
        Objects.requireNonNull(order2, "Second order cannot be null");

        Money price1 = order1.getPrice();
        Money price2 = order2.getPrice();

        if (isPriceBetter(price1, price2)) {
            return true;
        }

        if (isPriceBetter(price2, price1)) {
            return false;
        }

        return isTimeBetter(order1.getCreatedAt(), order2.getCreatedAt());
    }

    @Override
    public boolean isTimeBetter(LocalDateTime time1, LocalDateTime time2) {
        return time1.isBefore(time2);
    }

    @Override
    public Money calculatePriceDifference(Money price1, Money price2) {
        Money difference = price1.subtract(price2);
        return difference.isNegative() ? difference.negate() : difference;
    }

    @Override
    public boolean hasSamePrice(T order1, T order2) {
        Objects.requireNonNull(order1, "First order cannot be null");
        Objects.requireNonNull(order2, "Second order cannot be null");

        Money price1 = order1.getPrice();
        Money price2 = order2.getPrice();

        return price1.equals(price2);
    }
}
