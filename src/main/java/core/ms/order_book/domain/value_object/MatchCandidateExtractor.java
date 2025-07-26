package core.ms.order_book.domain.value_object;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;

public interface MatchCandidateExtractor {
    IBuyOrder getBuyOrder();
    ISellOrder getSellOrder();
    boolean isValid();
}