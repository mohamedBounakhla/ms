package core.ms.order.domain.entities;

import core.ms.shared.money.Money;

public interface IBuyOrder extends IOrder {
    Money getCostBasis();

}
