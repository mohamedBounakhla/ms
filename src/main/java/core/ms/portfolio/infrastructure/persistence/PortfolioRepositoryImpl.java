package core.ms.portfolio.infrastructure.persistence;

import core.ms.portfolio.domain.Portfolio;
import core.ms.portfolio.domain.ports.outbound.PortfolioRepository;
import core.ms.portfolio.infrastructure.persistence.dao.PortfolioDAO;
import core.ms.portfolio.infrastructure.persistence.entities.CashBalanceEntity;
import core.ms.portfolio.infrastructure.persistence.entities.PortfolioEntity;
import core.ms.portfolio.infrastructure.persistence.mappers.PortfolioMapper;
import core.ms.shared.money.Money;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Transactional
public class PortfolioRepositoryImpl implements PortfolioRepository {

    @Autowired
    private PortfolioDAO portfolioDAO;

    @Autowired
    private PortfolioMapper portfolioMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Portfolio save(Portfolio portfolio) {
        // Check if entity already exists
        Optional<PortfolioEntity> existingEntity = portfolioDAO.findById(portfolio.getPortfolioId());

        if (existingEntity.isPresent()) {
            // Update existing entity
            PortfolioEntity entity = existingEntity.get();
            entity.setUpdatedAt(LocalDateTime.now());

            // Update cash balances
            for (var currency : core.ms.shared.money.Currency.values()) {
                Money total = portfolio.getTotalCash(currency);
                if (total.isPositive()) {
                    // Find existing cash balance or create new one
                    CashBalanceEntity cashBalance = entity.getCashBalances().stream()
                            .filter(cb -> cb.getCurrency() == currency)
                            .findFirst()
                            .orElseGet(() -> {
                                CashBalanceEntity newCb = new CashBalanceEntity(currency, total.getAmount());
                                entity.addCashBalance(newCb);
                                return newCb;
                            });

                    // Update values
                    cashBalance.setBalance(total.getAmount());
                    Money reserved = portfolio.getReservedCash(currency);
                    cashBalance.setReservedAmount(reserved.getAmount());
                    cashBalance.setUpdatedAt(LocalDateTime.now());
                }
            }

            PortfolioEntity saved = portfolioDAO.save(entity);
            return portfolioMapper.toDomain(saved);
        } else {
            // Create new entity
            PortfolioEntity entity = portfolioMapper.toEntity(portfolio);
            PortfolioEntity saved = portfolioDAO.save(entity);
            return portfolioMapper.toDomain(saved);
        }
    }

    @Override
    public Portfolio saveAndFlush(Portfolio portfolio) {
        Portfolio saved = save(portfolio);
        entityManager.flush();
        return saved;
    }

    @Override
    public Optional<Portfolio> findById(String portfolioId) {
        return portfolioDAO.findById(portfolioId)
                .map(portfolioMapper::toDomain);
    }

    @Override
    public Optional<Portfolio> findByIdWithLock(String portfolioId, LockModeType lockMode) {
        PortfolioEntity entity = entityManager.find(PortfolioEntity.class, portfolioId, lockMode);
        if (entity != null) {
            return Optional.of(portfolioMapper.toDomain(entity));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Portfolio> findByOwnerId(String ownerId) {
        return portfolioDAO.findByOwnerId(ownerId)
                .map(portfolioMapper::toDomain);
    }

    @Override
    public void deleteById(String portfolioId) {
        portfolioDAO.deleteById(portfolioId);
    }

    @Override
    public boolean existsById(String portfolioId) {
        return portfolioDAO.existsById(portfolioId);
    }

    @Override
    public List<Portfolio> findAll() {
        return portfolioDAO.findAll().stream()
                .map(portfolioMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Portfolio> findByOwnerIds(List<String> ownerIds) {
        return portfolioDAO.findByOwnerIdIn(ownerIds).stream()
                .map(portfolioMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return portfolioDAO.count();
    }
}