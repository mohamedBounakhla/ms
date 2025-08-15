package core.ms.portfolio.infrastructure.persistence.dao;

import core.ms.portfolio.infrastructure.persistence.entities.CashReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface CashReservationDAO extends JpaRepository<CashReservationEntity, String> {
    List<CashReservationEntity> findByPortfolioId(String portfolioId);
    List<CashReservationEntity> findByOrderId(String orderId);
    int deleteByExpirationTimeBefore(Instant cutoff);

    @Query("SELECT cr.reservationId FROM CashReservationEntity cr WHERE cr.expirationTime < :cutoff")
    List<String> findExpiredReservationIds(@Param("cutoff") Instant cutoff);
}