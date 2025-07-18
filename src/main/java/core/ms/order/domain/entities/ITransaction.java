package core.ms.order.domain.entities;

import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ITransaction {
    String getId();
    Symbol getSymbol();
    IBuyOrder getBuyOrder();
    ISellOrder getSellOrder();
    Money getPrice();
    BigDecimal getQuantity();
    LocalDateTime getCreatedAt();
    Money getTotalValue();

}