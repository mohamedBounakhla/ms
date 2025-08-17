package core.ms.order.infrastructure.persistence.entities;

import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.shared.money.Currency;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sell_orders")
public class SellOrderEntity extends AbstractOrderEntity {

    public SellOrderEntity() { super(); }

    public SellOrderEntity(String id, String portfolioId, String reservationId,
                           String symbolCode, String symbolName,
                           BigDecimal price, Currency currency, BigDecimal quantity,
                           OrderStatusEnum status, BigDecimal executedQuantity,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, portfolioId, reservationId, symbolCode, symbolName, price, currency, quantity,
                status, executedQuantity, createdAt, updatedAt);
    }
}