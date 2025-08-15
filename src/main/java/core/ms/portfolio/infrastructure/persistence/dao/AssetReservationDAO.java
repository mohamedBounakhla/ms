package core.ms.portfolio.infrastructure.persistence.dao;

import core.ms.portfolio.infrastructure.persistence.entities.AssetReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AssetReservationDAO extends JpaRepository<AssetReservationEntity, String> {
    List<AssetReservationEntity> findByPortfolioId(String portfolioId);
    List<AssetReservationEntity> findByOrderId(String orderId);
    List<AssetReservationEntity> findBySymbolCode(String symbolCode);
    int deleteByExpirationTimeBefore(Instant cutoff);

    @Query("SELECT ar.reservationId FROM AssetReservationEntity ar WHERE ar.expirationTime < :cutoff")
    List<String> findExpiredReservationIds(@Param("cutoff") Instant cutoff);
}