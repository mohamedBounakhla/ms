package core.ms.order.domain.ports.outbound;

import java.util.Optional;


public interface UserRepository {
    boolean existsById(String userId);
    boolean canPlaceOrders(String userId);
    Optional<UserTradingLimits> getTradingLimits(String userId);
}