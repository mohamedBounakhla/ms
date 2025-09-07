package core.ms.order.infrastructure.services;

import core.ms.order.domain.entities.*;
import core.ms.order.domain.ports.outbound.OrderRepository;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.order.infrastructure.persistence.dao.BuyOrderDAO;
import core.ms.order.infrastructure.persistence.dao.SellOrderDAO;
import core.ms.order.infrastructure.persistence.entities.BuyOrderEntity;
import core.ms.order.infrastructure.persistence.entities.SellOrderEntity;
import core.ms.order.infrastructure.persistence.mappers.BuyOrderMapper;
import core.ms.order.infrastructure.persistence.mappers.SellOrderMapper;
import core.ms.shared.money.Symbol;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderRepositoryService implements OrderRepository {

    @Autowired
    private BuyOrderDAO buyOrderDAO;

    @Autowired
    private SellOrderDAO sellOrderDAO;

    @Autowired
    private BuyOrderMapper buyOrderMapper;

    @Autowired
    private SellOrderMapper sellOrderMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public IOrder save(IOrder order) {
        if (order instanceof BuyOrder buyOrder) {
            BuyOrderEntity entity = buyOrderMapper.fromDomain(buyOrder);
            BuyOrderEntity saved = buyOrderDAO.save(entity);
            return buyOrderMapper.toDomain(saved);
        } else if (order instanceof SellOrder sellOrder) {
            SellOrderEntity entity = sellOrderMapper.fromDomain(sellOrder);
            SellOrderEntity saved = sellOrderDAO.save(entity);
            return sellOrderMapper.toDomain(saved);
        } else {
            throw new IllegalArgumentException("Unknown order type: " + order.getClass());
        }
    }

    @Override
    public void flush() {
        entityManager.flush();
    }

    @Override
    public Optional<IOrder> findById(String orderId) {
        // Try buy orders first
        Optional<BuyOrderEntity> buyEntity = buyOrderDAO.findById(orderId);
        if (buyEntity.isPresent()) {
            return Optional.of(buyOrderMapper.toDomain(buyEntity.get()));
        }

        // Try sell orders
        Optional<SellOrderEntity> sellEntity = sellOrderDAO.findById(orderId);
        return sellEntity.map(sellOrderEntity -> sellOrderMapper.toDomain(sellOrderEntity));
    }

    @Override
    public Optional<IOrder> findByIdWithLock(String orderId, LockModeType lockMode) {
        // Try buy orders first
        Optional<BuyOrderEntity> buyEntity = buyOrderDAO.findById(orderId);
        if (buyEntity.isPresent()) {
            BuyOrderEntity locked = entityManager.find(BuyOrderEntity.class, orderId, lockMode);
            if (locked != null) {
                return Optional.of(buyOrderMapper.toDomain(locked));
            }
        }

        // Try sell orders
        Optional<SellOrderEntity> sellEntity = sellOrderDAO.findById(orderId);
        if (sellEntity.isPresent()) {
            SellOrderEntity locked = entityManager.find(SellOrderEntity.class, orderId, lockMode);
            if (locked != null) {
                return Optional.of(sellOrderMapper.toDomain(locked));
            }
        }

        return Optional.empty();
    }

    @Override
    public void deleteById(String orderId) {
        buyOrderDAO.deleteById(orderId);
        sellOrderDAO.deleteById(orderId);
    }

    @Override
    public boolean existsById(String orderId) {
        return buyOrderDAO.existsById(orderId) || sellOrderDAO.existsById(orderId);
    }

    @Override
    public List<IOrder> findBySymbol(Symbol symbol) {
        List<IOrder> orders = new ArrayList<>();

        orders.addAll(buyOrderDAO.findBySymbolCode(symbol.getCode()).stream()
                .map(buyOrderMapper::toDomain)
                .toList());

        orders.addAll(sellOrderDAO.findBySymbolCode(symbol.getCode()).stream()
                .map(sellOrderMapper::toDomain)
                .toList());

        return orders;
    }

    @Override
    public List<IOrder> findByStatus(OrderStatusEnum status) {
        List<IOrder> orders = new ArrayList<>();

        orders.addAll(buyOrderDAO.findByStatus(status).stream()
                .map(buyOrderMapper::toDomain)
                .toList());

        orders.addAll(sellOrderDAO.findByStatus(status).stream()
                .map(sellOrderMapper::toDomain)
                .toList());

        return orders;
    }

    @Override
    public List<IOrder> findByPortfolioId(String portfolioId) {
        List<IOrder> orders = new ArrayList<>();

        orders.addAll(buyOrderDAO.findByPortfolioId(portfolioId).stream()
                .map(buyOrderMapper::toDomain)
                .toList());

        orders.addAll(sellOrderDAO.findByPortfolioId(portfolioId).stream()
                .map(sellOrderMapper::toDomain)
                .toList());

        return orders;
    }

    @Override
    public List<IBuyOrder> findBuyOrdersBySymbol(Symbol symbol) {
        return buyOrderDAO.findBySymbolCode(symbol.getCode()).stream()
                .map(buyOrderMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ISellOrder> findSellOrdersBySymbol(Symbol symbol) {
        return sellOrderDAO.findBySymbolCode(symbol.getCode()).stream()
                .map(sellOrderMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<IOrder> findAll() {
        List<IOrder> orders = new ArrayList<>();

        orders.addAll(buyOrderDAO.findAll().stream()
                .map(buyOrderMapper::toDomain)
                .toList());

        orders.addAll(sellOrderDAO.findAll().stream()
                .map(sellOrderMapper::toDomain)
                .toList());

        return orders;
    }

    @Override
    public long count() {
        return buyOrderDAO.count() + sellOrderDAO.count();
    }
}