package core.ms.order.domain.events.publish;

import core.ms.shared.events.BaseEvent;
import core.ms.shared.events.DomainEvent;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionCreatedEvent extends BaseEvent {
    private final String transactionId;
    private final String buyOrderId;
    private final String sellOrderId;
    private final String buyerPortfolioId;
    private final String sellerPortfolioId;
    private final String buyerReservationId;
    private final String sellerReservationId;
    private final String symbolCode;
    private final BigDecimal executedQuantity;
    private final BigDecimal executionPrice;
    private final Currency currency;
    private final BigDecimal buyOrderRemainingQuantity;
    private final BigDecimal sellOrderRemainingQuantity;

    public TransactionCreatedEvent(String correlationId, String transactionId,
                                   String buyOrderId, String sellOrderId,
                                   String buyerPortfolioId, String sellerPortfolioId,
                                   String buyerReservationId, String sellerReservationId,
                                   String symbolCode, BigDecimal executedQuantity,
                                   BigDecimal executionPrice, Currency currency,
                                   BigDecimal buyOrderRemainingQuantity,
                                   BigDecimal sellOrderRemainingQuantity) {
        super(correlationId, "ORDER_BC");
        this.transactionId = transactionId;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.buyerPortfolioId = buyerPortfolioId;
        this.sellerPortfolioId = sellerPortfolioId;
        this.buyerReservationId = buyerReservationId;
        this.sellerReservationId = sellerReservationId;
        this.symbolCode = symbolCode;
        this.executedQuantity = executedQuantity;
        this.executionPrice = executionPrice;
        this.currency = currency;
        this.buyOrderRemainingQuantity = buyOrderRemainingQuantity;
        this.sellOrderRemainingQuantity = sellOrderRemainingQuantity;
    }

    // Getters
    public String getTransactionId() { return transactionId; }
    public String getBuyOrderId() { return buyOrderId; }
    public String getSellOrderId() { return sellOrderId; }
    public String getBuyerPortfolioId() { return buyerPortfolioId; }
    public String getSellerPortfolioId() { return sellerPortfolioId; }
    public String getBuyerReservationId() { return buyerReservationId; }
    public String getSellerReservationId() { return sellerReservationId; }
    public String getSymbolCode() { return symbolCode; }
    public BigDecimal getExecutedQuantity() { return executedQuantity; }
    public BigDecimal getExecutionPrice() { return executionPrice; }
    public Currency getCurrency() { return currency; }
    public BigDecimal getBuyOrderRemainingQuantity() { return buyOrderRemainingQuantity; }
    public BigDecimal getSellOrderRemainingQuantity() { return sellOrderRemainingQuantity; }
}