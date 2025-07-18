package core.ms.order.domain.entities;

import core.ms.shared.domain.Money;

public interface IBuyOrder extends IOrder {
    Money getCostBasis();

}
