package core.ms.order.infrastructure.services;

import core.ms.order.domain.entities.*;
import core.ms.order.domain.ports.outbound.OrderRepository;
import core.ms.order.domain.ports.outbound.TransactionRepository;
import core.ms.order.infrastructure.persistence.dao.TransactionDAO;
import core.ms.order.infrastructure.persistence.entities.TransactionEntity;
import core.ms.order.infrastructure.persistence.mappers.TransactionMapper;
import core.ms.shared.domain.Symbol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TransactionRepositoryService implements TransactionRepository {

    @Autowired
    private TransactionDAO transactionDAO;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public ITransaction save(ITransaction transaction) {
        Transaction domainTransaction = (Transaction) transaction;
        TransactionEntity entity = transactionMapper.fromDomain(domainTransaction);
        TransactionEntity saved = transactionDAO.save(entity);

        // Get orders for reconstruction
        IBuyOrder buyOrder = getBuyOrderById(saved.getBuyOrderId());
        ISellOrder sellOrder = getSellOrderById(saved.getSellOrderId());

        return transactionMapper.toDomain(saved, buyOrder, sellOrder);
    }

    @Override
    public Optional<ITransaction> findById(String transactionId) {
        Optional<TransactionEntity> entityOpt = transactionDAO.findById(transactionId);

        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }

        TransactionEntity entity = entityOpt.get();
        IBuyOrder buyOrder = getBuyOrderById(entity.getBuyOrderId());
        ISellOrder sellOrder = getSellOrderById(entity.getSellOrderId());

        return Optional.of(transactionMapper.toDomain(entity, buyOrder, sellOrder));
    }

    @Override
    public void deleteById(String transactionId) {
        transactionDAO.deleteById(transactionId);
    }

    @Override
    public boolean existsById(String transactionId) {
        return transactionDAO.existsById(transactionId);
    }

    @Override
    public List<ITransaction> findByOrderId(String orderId) {
        List<TransactionEntity> entities = new ArrayList<>();
        entities.addAll(transactionDAO.findByBuyOrderId(orderId));
        entities.addAll(transactionDAO.findBySellOrderId(orderId));
        return mapToDomain(entities);
    }

    @Override
    public List<ITransaction> findBySymbol(Symbol symbol) {
        List<TransactionEntity> entities = transactionDAO.findBySymbolCode(symbol.getCode());
        return mapToDomain(entities);
    }

    @Override
    public List<ITransaction> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<TransactionEntity> entities = transactionDAO.findByCreatedAtBetween(startDate, endDate);
        return mapToDomain(entities);
    }

    @Override
    public List<ITransaction> findAll() {
        List<TransactionEntity> entities = transactionDAO.findAll();
        return mapToDomain(entities);
    }

    @Override
    public long count() {
        return transactionDAO.count();
    }

    // Helper methods
    private List<ITransaction> mapToDomain(List<TransactionEntity> entities) {
        return entities.stream()
                .map(entity -> {
                    IBuyOrder buyOrder = getBuyOrderById(entity.getBuyOrderId());
                    ISellOrder sellOrder = getSellOrderById(entity.getSellOrderId());
                    return transactionMapper.toDomain(entity, buyOrder, sellOrder);
                })
                .collect(Collectors.toList());
    }

    private IBuyOrder getBuyOrderById(String orderId) {
        Optional<IOrder> order = orderRepository.findById(orderId);
        if (order.isPresent() && order.get() instanceof IBuyOrder) {
            return (IBuyOrder) order.get();
        }
        throw new IllegalStateException("Buy order not found: " + orderId);
    }

    private ISellOrder getSellOrderById(String orderId) {
        Optional<IOrder> order = orderRepository.findById(orderId);
        if (order.isPresent() && order.get() instanceof ISellOrder) {
            return (ISellOrder) order.get();
        }
        throw new IllegalStateException("Sell order not found: " + orderId);
    }
}