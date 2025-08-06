package core.ms.order_book.domain.ports.outbound;

import core.ms.order_book.domain.value_object.OrderBookSnapshot;
import core.ms.shared.money.Symbol;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderBookSnapshotRepository {
    OrderBookSnapshot save(OrderBookSnapshot snapshot);
    Optional<OrderBookSnapshot> findLatestBySymbol(Symbol symbol);
    List<OrderBookSnapshot> findBySymbolAndTimestampBetween(Symbol symbol, Instant start, Instant end);
    void deleteByTimestampBefore(Instant cutoff);
    long countBySymbol(Symbol symbol);
}