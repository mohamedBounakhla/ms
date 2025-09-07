package core.ms.portfolio.infrastructure.persistence.dao;

import core.ms.portfolio.infrastructure.persistence.entities.ReservationEntity;
import core.ms.portfolio.infrastructure.persistence.entities.ReservationEntity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationDAO extends JpaRepository<ReservationEntity, String> {

    List<ReservationEntity> findByPortfolioPortfolioId(String portfolioId);

    List<ReservationEntity> findByPortfolioPortfolioIdAndStatus(String portfolioId, ReservationStatus status);

    Optional<ReservationEntity> findByReservationIdAndPortfolioPortfolioId(String reservationId, String portfolioId);

    @Query("SELECT r FROM ReservationEntity r WHERE r.status IN :statuses AND r.createdAt < :cutoff")
    List<ReservationEntity> findExpiredReservations(
            @Param("statuses") List<ReservationStatus> statuses,
            @Param("cutoff") LocalDateTime cutoff
    );

    @Query("SELECT COUNT(r) FROM ReservationEntity r WHERE r.portfolio.portfolioId = :portfolioId AND r.status IN :statuses")
    int countActiveReservations(
            @Param("portfolioId") String portfolioId,
            @Param("statuses") List<ReservationStatus> statuses
    );

    void deleteByPortfolioPortfolioId(String portfolioId);
}