package core.ms.order.domain.events;

import core.ms.shared.events.DomainEvent;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionCreatedEvent implements DomainEvent {
    private final String transactionId;
    private final String buyOrderId;
    private final String sellOrderId;
    private final String buyerPortfolioId;
    private final String sellerPortfolioId;
    private final String buyerReservationId;
    private final String sellerReservationId;
    private final Symbol symbol;
    private final BigDecimal executedQuantity;
    private final Money executionPrice;
    private final LocalDateTime occurredAt;

    public TransactionCreatedEvent(String transactionId,
                                   String buyOrderId, String sellOrderId,
                                   String buyerPortfolioId, String sellerPortfolioId,
                                   String buyerReservationId, String sellerReservationId,
                                   Symbol symbol, BigDecimal executedQuantity,
                                   Money executionPrice) {
        this.transactionId = transactionId;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.buyerPortfolioId = buyerPortfolioId;
        this.sellerPortfolioId = sellerPortfolioId;
        this.buyerReservationId = buyerReservationId;
        this.sellerReservationId = sellerReservationId;
        this.symbol = symbol;
        this.executedQuantity = executedQuantity;
        this.executionPrice = executionPrice;
        this.occurredAt = LocalDateTime.now();
    }

    // All getters
    public String getTransactionId() { return transactionId; }
    public String getBuyOrderId() { return buyOrderId; }
    public String getSellOrderId() { return sellOrderId; }
    public String getBuyerPortfolioId() { return buyerPortfolioId; }
    public String getSellerPortfolioId() { return sellerPortfolioId; }
    public String getBuyerReservationId() { return buyerReservationId; }
    public String getSellerReservationId() { return sellerReservationId; }
    public Symbol getSymbol() { return symbol; }
    public BigDecimal getExecutedQuantity() { return executedQuantity; }
    public Money getExecutionPrice() { return executionPrice; }
    @Override
    public LocalDateTime getOccurredAt() { return occurredAt; }
}