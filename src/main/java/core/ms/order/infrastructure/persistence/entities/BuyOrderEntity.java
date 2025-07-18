package core.ms.order.infrastructure.persistence.entities;

import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.shared.domain.Currency;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "buy_orders")
public class BuyOrderEntity extends AbstractOrderEntity {

    public BuyOrderEntity() { super(); }

    public BuyOrderEntity(String id, String symbolCode, String symbolName,
                          BigDecimal price, Currency currency, BigDecimal quantity,
                          OrderStatusEnum status, BigDecimal executedQuantity,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, symbolCode, symbolName, price, currency, quantity,
                status, executedQuantity, createdAt, updatedAt);
    }
}