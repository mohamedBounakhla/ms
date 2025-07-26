package core.ms.order_book.domain.value_object;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order_book.domain.entities.OrderBook;

import java.util.List;

public interface MatchingStrategy<T extends MatchCandidateExtractor> {
    /**
     * Finds all possible match candidates in the given order book.
     * Returns intermediate objects containing the validated order pairs.
     * The factory is responsible for converting candidates to OrderMatches.
     *
     * @param orderBook the order book to analyze
     * @return list of match candidates found
     */
    List<T> findMatchCandidates(OrderBook orderBook);

    /**
     * Determines if two orders can be matched together.
     * Pure validation logic - no object creation.
     *
     * @param buyOrder the buy order
     * @param sellOrder the sell order
     * @return true if orders can be matched
     */
    boolean canMatch(IBuyOrder buyOrder, ISellOrder sellOrder);
}