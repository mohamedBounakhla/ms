package core.ms.portfolio.infrastructure.persistence.mappers;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.portfolio.domain.cash.CashReservation;
import core.ms.portfolio.domain.ports.outbound.OrderServiceAdapter;
import core.ms.portfolio.domain.positions.AssetReservation;
import core.ms.portfolio.infrastructure.persistence.entities.AssetReservationEntity;
import core.ms.portfolio.infrastructure.persistence.entities.CashReservationEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    @Autowired
    private OrderServiceAdapter orderServiceAdapter;

    // ===== CASH RESERVATION MAPPING =====

    public CashReservationEntity toEntity(CashReservation reservation) {
        CashReservationEntity entity = new CashReservationEntity();
        entity.setReservationId(reservation.getReservationId());
        entity.setOrderId(reservation.getOrder().getId());
        entity.setCurrency(reservation.getCurrency());
        entity.setAmount(reservation.getReservedAmount());
        entity.setCreatedAt(reservation.getCreatedAt());
        entity.setExpirationTime(reservation.getExpirationTime());
        entity.setStatus(reservation.isExpired() ? "EXPIRED" : "ACTIVE");

        // Portfolio ID would need to be tracked in the domain model
        // For now, this is a simplified implementation

        return entity;
    }

    public CashReservation toDomain(CashReservationEntity entity) {
        // Fetch the associated buy order
        IBuyOrder buyOrder = orderServiceAdapter.findBuyOrderById(entity.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Buy order not found: " + entity.getOrderId()));

        // Reconstruct the reservation
        return new CashReservation(entity.getReservationId(), buyOrder);
    }

    // ===== ASSET RESERVATION MAPPING =====

    public AssetReservationEntity toEntity(AssetReservation reservation) {
        AssetReservationEntity entity = new AssetReservationEntity();
        entity.setReservationId(reservation.getReservationId());
        entity.setOrderId(reservation.getOrder().getId());
        entity.setSymbolCode(reservation.getSymbol().getCode());
        entity.setQuantity(reservation.getReservedAmount());
        entity.setCreatedAt(reservation.getCreatedAt());
        entity.setExpirationTime(reservation.getExpirationTime());
        entity.setStatus(reservation.isExpired() ? "EXPIRED" : "ACTIVE");

        // Portfolio ID would need to be tracked in the domain model
        // For now, this is a simplified implementation

        return entity;
    }

    public AssetReservation toDomain(AssetReservationEntity entity) {
        // Fetch the associated sell order
        ISellOrder sellOrder = orderServiceAdapter.findSellOrderById(entity.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Sell order not found: " + entity.getOrderId()));

        // Reconstruct the reservation
        return new AssetReservation(entity.getReservationId(), sellOrder);
    }
}