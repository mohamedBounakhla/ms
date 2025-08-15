package core.ms.portfolio.infrastructure.persistence;

import core.ms.portfolio.domain.cash.CashReservation;
import core.ms.portfolio.domain.ports.outbound.ReservationRepository;
import core.ms.portfolio.domain.positions.AssetReservation;
import core.ms.portfolio.infrastructure.persistence.dao.AssetReservationDAO;
import core.ms.portfolio.infrastructure.persistence.dao.CashReservationDAO;
import core.ms.portfolio.infrastructure.persistence.entities.AssetReservationEntity;
import core.ms.portfolio.infrastructure.persistence.entities.CashReservationEntity;
import core.ms.portfolio.infrastructure.persistence.mappers.ReservationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Transactional
public class ReservationRepositoryImpl implements ReservationRepository {

    @Autowired
    private CashReservationDAO cashReservationDAO;

    @Autowired
    private AssetReservationDAO assetReservationDAO;

    @Autowired
    private ReservationMapper reservationMapper;

    // ===== CASH RESERVATIONS =====

    @Override
    public CashReservation saveCashReservation(CashReservation reservation) {
        CashReservationEntity entity = reservationMapper.toEntity(reservation);
        CashReservationEntity saved = cashReservationDAO.save(entity);
        return reservationMapper.toDomain(saved);
    }

    @Override
    public Optional<CashReservation> findCashReservationById(String reservationId) {
        return cashReservationDAO.findById(reservationId)
                .map(reservationMapper::toDomain);
    }

    @Override
    public List<CashReservation> findCashReservationsByPortfolioId(String portfolioId) {
        return cashReservationDAO.findByPortfolioId(portfolioId).stream()
                .map(reservationMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteCashReservation(String reservationId) {
        cashReservationDAO.deleteById(reservationId);
    }

    // ===== ASSET RESERVATIONS =====

    @Override
    public AssetReservation saveAssetReservation(AssetReservation reservation) {
        AssetReservationEntity entity = reservationMapper.toEntity(reservation);
        AssetReservationEntity saved = assetReservationDAO.save(entity);
        return reservationMapper.toDomain(saved);
    }

    @Override
    public Optional<AssetReservation> findAssetReservationById(String reservationId) {
        return assetReservationDAO.findById(reservationId)
                .map(reservationMapper::toDomain);
    }

    @Override
    public List<AssetReservation> findAssetReservationsByPortfolioId(String portfolioId) {
        return assetReservationDAO.findByPortfolioId(portfolioId).stream()
                .map(reservationMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAssetReservation(String reservationId) {
        assetReservationDAO.deleteById(reservationId);
    }

    // ===== CLEANUP OPERATIONS =====

    @Override
    public int deleteExpiredReservationsBefore(Instant cutoff) {
        int cashDeleted = cashReservationDAO.deleteByExpirationTimeBefore(cutoff);
        int assetDeleted = assetReservationDAO.deleteByExpirationTimeBefore(cutoff);
        return cashDeleted + assetDeleted;
    }

    @Override
    public List<String> findExpiredReservationIds(Instant cutoff) {
        List<String> expiredCash = cashReservationDAO.findExpiredReservationIds(cutoff);
        List<String> expiredAsset = assetReservationDAO.findExpiredReservationIds(cutoff);
        expiredCash.addAll(expiredAsset);
        return expiredCash;
    }
}