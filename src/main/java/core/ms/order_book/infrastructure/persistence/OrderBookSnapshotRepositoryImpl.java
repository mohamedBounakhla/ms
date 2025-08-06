package core.ms.order_book.infrastructure.persistence;

import core.ms.order_book.domain.entities.OrderBook;
import core.ms.order_book.domain.ports.outbound.OrderBookSnapshotRepository;
import core.ms.order_book.domain.value_object.OrderBookSnapshot;
import core.ms.order_book.infrastructure.persistence.DAO.OrderBookSnapshotDAO;
import core.ms.order_book.infrastructure.persistence.entities.OrderBookSnapshotEntity;
import core.ms.order_book.infrastructure.persistence.mappers.OrderBookSnapshotMapper;
import core.ms.shared.money.Symbol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class OrderBookSnapshotRepositoryImpl implements OrderBookSnapshotRepository {

    @Autowired
    private OrderBookSnapshotDAO dao;

    @Autowired
    private OrderBookSnapshotMapper mapper;

    @Override
    public OrderBookSnapshot save(OrderBookSnapshot snapshot) {
        // This would need to be implemented differently since we're creating
        // snapshots from OrderBook, not saving domain snapshots
        throw new UnsupportedOperationException("Use createSnapshot method instead");
    }

    @Override
    public Optional<OrderBookSnapshot> findLatestBySymbol(Symbol symbol) {
        return dao.findLatestBySymbol(symbol.getCode())
                .map(mapper::toDomain);
    }

    @Override
    public List<OrderBookSnapshot> findBySymbolAndTimestampBetween(
            Symbol symbol, Instant start, Instant end) {
        return dao.findBySymbolCodeAndSnapshotTimeBetween(symbol.getCode(), start, end)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByTimestampBefore(Instant cutoff) {
        dao.deleteBySnapshotTimeBefore(cutoff);
    }

    @Override
    public long countBySymbol(Symbol symbol) {
        return dao.countBySymbolCode(symbol.getCode());
    }

    // Helper method to create snapshot from OrderBook
    public void createSnapshot(OrderBook orderBook) {
        OrderBookSnapshotEntity entity = mapper.toEntity(orderBook);
        dao.save(entity);
    }
}