package core.ms.portfolio.infrastructure.persistence.dao;

import core.ms.portfolio.infrastructure.persistence.entities.PortfolioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioDAO extends JpaRepository<PortfolioEntity, String> {
    Optional<PortfolioEntity> findByOwnerId(String ownerId);
    List<PortfolioEntity> findByOwnerIdIn(List<String> ownerIds);
}