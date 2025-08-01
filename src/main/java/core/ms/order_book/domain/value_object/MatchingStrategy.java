package core.ms.order_book.domain.value_object;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;

import java.util.List;

public interface MatchingStrategy {
    /**
     * Finds match candidates between two specific orders.
     */
    List<? extends MatchCandidateExtractor> findMatchCandidates(IBuyOrder buyOrder, ISellOrder sellOrder);

    /**
     * Validates if two orders can be matched.
     */
    boolean canMatch(IBuyOrder buyOrder, ISellOrder sellOrder);


}