package core.ms.portfolio.domain.events.publish;

import core.ms.shared.OrderType;
import core.ms.shared.events.BaseEvent;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;

public class OrderRequestedEvent extends BaseEvent {
    private final String reservationId;
    private final String portfolioId;
    private final Symbol symbol;
    private final Money price;
    private final BigDecimal quantity;
    private final OrderType orderType;

    public OrderRequestedEvent(String correlationId, String sourceBC,
                               String reservationId, String portfolioId,
                               Symbol symbol, Money price, BigDecimal quantity,
                               OrderType orderType) {
        super(correlationId, sourceBC);
        this.reservationId = reservationId;
        this.portfolioId = portfolioId;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.orderType = orderType;
    }

    // Getters
    public String getReservationId() { return reservationId; }
    public String getPortfolioId() { return portfolioId; }
    public Symbol getSymbol() { return symbol; }
    public Money getPrice() { return price; }
    public BigDecimal getQuantity() { return quantity; }
    public OrderType getOrderType() { return orderType; }
}