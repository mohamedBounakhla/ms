package core.ms.order.domain.entities;

import core.ms.shared.domain.Money;

public interface ISellOrder extends IOrder {
    Money getProceeds();
}