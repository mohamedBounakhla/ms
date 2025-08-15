package core.ms.portfolio.infrastructure.persistence;

import core.ms.portfolio.domain.Portfolio;
import core.ms.portfolio.domain.ports.outbound.PortfolioRepository;
import core.ms.portfolio.infrastructure.persistence.dao.PortfolioDAO;
import core.ms.portfolio.infrastructure.persistence.entities.PortfolioEntity;
import core.ms.portfolio.infrastructure.persistence.mappers.PortfolioMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public Portfolio save(Portfolio portfolio) {
        PortfolioEntity entity = portfolioMapper.toEntity(portfolio);
        PortfolioEntity saved = portfolioDAO.save(entity);
        return portfolioMapper.toDomain(saved);
    }

    @Override
    public Optional<Portfolio> findById(String portfolioId) {
        return portfolioDAO.findById(portfolioId)
                .map(portfolioMapper::toDomain);
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