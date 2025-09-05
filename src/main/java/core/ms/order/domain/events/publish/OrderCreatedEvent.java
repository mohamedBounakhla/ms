package core.ms.order.domain.events.publish;

import core.ms.shared.OrderType;
import core.ms.shared.events.BaseEvent;
import core.ms.shared.events.DomainEvent;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderCreatedEvent extends BaseEvent {
    private final String orderId;
    private final String portfolioId;
    private final String reservationId;
    private final Symbol symbol;
    private final Money price;
    private final BigDecimal quantity;
    private final OrderType orderType;
    private final String status;

    public OrderCreatedEvent(String correlationId, String orderId, String portfolioId,
                             String reservationId, Symbol symbol, Money price,
                             BigDecimal quantity, OrderType orderType, String status) {
        super(correlationId, "ORDER_BC");
        this.orderId = orderId;
        this.portfolioId = portfolioId;
        this.reservationId = reservationId;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.orderType = orderType;
        this.status = status;
    }

    // Getters
    public String getOrderId() { return orderId; }
    public String getPortfolioId() { return portfolioId; }
    public String getReservationId() { return reservationId; }
    public Symbol getSymbol() { return symbol; }
    public Money getPrice() { return price; }
    public BigDecimal getQuantity() { return quantity; }
    public OrderType getOrderType() { return orderType; }
    public String getStatus() { return status; }
}