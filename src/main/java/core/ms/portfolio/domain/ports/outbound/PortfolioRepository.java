package core.ms.portfolio.domain.ports.outbound;

import core.ms.portfolio.domain.Portfolio;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository {
    Portfolio save(Portfolio portfolio);
    Portfolio saveAndFlush(Portfolio portfolio);
    Optional<Portfolio> findById(String portfolioId);
    Optional<Portfolio> findByIdWithLock(String portfolioId, LockModeType lockMode);
    Optional<Portfolio> findByOwnerId(String ownerId);
    void deleteById(String portfolioId);
    boolean existsById(String portfolioId);
    List<Portfolio> findAll();
    List<Portfolio> findByOwnerIds(List<String> ownerIds);
    long count();
}