package core.ms.order_book.domain.value_object;
import java.util.List;

public interface MatchingAlgorithm {
    /**
     * Finds all potential match candidates using specific traversal algorithm.
     * Delegates individual order pair validation to the injected strategy.
     *
     * @param bidSide BidSideManager with sorted bid levels
     * @param askSide AskSideManager with sorted ask levels
     * @param strategy MatchingStrategy for validating individual order pairs
     * @return List of valid match candidates
     */
    List<MatchCandidateExtractor> findMatchCandidates(
            BidSideManager bidSide,
            AskSideManager askSide,
            MatchingStrategy strategy
    );
}
