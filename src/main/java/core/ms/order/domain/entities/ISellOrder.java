package core.ms.order.domain.entities;

import core.ms.shared.money.Money;

public interface ISellOrder extends IOrder {
    Money getProceeds();
}