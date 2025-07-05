package core.ms.order.domain;

import core.ms.shared.domain.Money;

public interface ISellOrder extends IOrder {
    Money getProceeds();
}