package core.ms.order.domain;

import core.ms.shared.domain.Money;

public interface IBuyOrder extends IOrder{
    Money getCostBasis();

}
