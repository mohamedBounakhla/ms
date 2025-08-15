package core.ms.portfolio.domain.ports.outbound;

import core.ms.portfolio.domain.cash.CashReservation;
import core.ms.portfolio.domain.positions.AssetReservation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    // Cash Reservations
    CashReservation saveCashReservation(CashReservation reservation);
    Optional<CashReservation> findCashReservationById(String reservationId);
    List<CashReservation> findCashReservationsByPortfolioId(String portfolioId);
    void deleteCashReservation(String reservationId);

    // Asset Reservations
    AssetReservation saveAssetReservation(AssetReservation reservation);
    Optional<AssetReservation> findAssetReservationById(String reservationId);
    List<AssetReservation> findAssetReservationsByPortfolioId(String portfolioId);
    void deleteAssetReservation(String reservationId);

    // Cleanup operations
    int deleteExpiredReservationsBefore(Instant cutoff);
    List<String> findExpiredReservationIds(Instant cutoff);
}