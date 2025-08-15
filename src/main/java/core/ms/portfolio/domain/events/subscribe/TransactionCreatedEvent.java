package core.ms.portfolio.domain.events.subscribe;

import core.ms.shared.OrderType;
import core.ms.shared.events.DomainEvent;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionCreatedEvent implements DomainEvent {
    private final String transactionId;
    private final String orderId;
    private final String reservationId;
    private final String portfolioId;
    private final Symbol symbol;



    public TransactionCreatedEvent(String transactionId, String orderId, String reservationId, String portfolioId, Symbol symbol, BigDecimal executedQuantity, Money executedPrice, Money totalValue, OrderType type) {
        this.transactionId = transactionId;
        this.orderId = orderId;
        this.reservationId = reservationId;
        this.portfolioId = portfolioId;
        this.symbol = symbol;
        this.executedQuantity = executedQuantity;
        this.executedPrice = executedPrice;
        this.totalValue = totalValue;
        this.type = type;
        this.occurredAt = LocalDateTime.now();

    }
    public String getTransactionId() {
        return transactionId;
    }
    public String getOrderId() {
        return orderId;
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public BigDecimal getExecutedQuantity() {
        return executedQuantity;
    }

    public Money getExecutedPrice() {
        return executedPrice;
    }

    public Money getTotalValue() {
        return totalValue;
    }

    public OrderType getType() {
        return type;
    }

    private final BigDecimal executedQuantity;
    private final Money executedPrice;
    private final Money totalValue;
    private final LocalDateTime occurredAt;
    private final OrderType type;


    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }





}