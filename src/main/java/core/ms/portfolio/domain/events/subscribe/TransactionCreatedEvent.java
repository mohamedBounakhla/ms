package core.ms.portfolio.domain.events.subscribe;

import core.ms.shared.OrderType;
import core.ms.shared.events.BaseEvent;
import core.ms.shared.events.DomainEvent;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionCreatedEvent extends BaseEvent {
    private final String transactionId;
    private final String buyOrderId;
    private final String sellOrderId;
    private final String buyPortfolioId;
    private final String sellPortfolioId;
    private final String buyReservationId;
    private final String sellReservationId;
    private final Symbol symbol;
    private final BigDecimal executedQuantity;
    private final Money executedPrice;

    public TransactionCreatedEvent(String correlationId, String sourceBC,
                                   String transactionId, String buyOrderId, String sellOrderId,
                                   String buyPortfolioId, String sellPortfolioId,
                                   String buyReservationId, String sellReservationId,
                                   Symbol symbol, BigDecimal executedQuantity, Money executedPrice) {
        super(correlationId, sourceBC);
        this.transactionId = transactionId;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.buyPortfolioId = buyPortfolioId;
        this.sellPortfolioId = sellPortfolioId;
        this.buyReservationId = buyReservationId;
        this.sellReservationId = sellReservationId;
        this.symbol = symbol;
        this.executedQuantity = executedQuantity;
        this.executedPrice = executedPrice;
    }

    // Getters
    public String getTransactionId() { return transactionId; }
    public String getBuyOrderId() { return buyOrderId; }
    public String getSellOrderId() { return sellOrderId; }
    public String getBuyPortfolioId() { return buyPortfolioId; }
    public String getSellPortfolioId() { return sellPortfolioId; }
    public String getBuyReservationId() { return buyReservationId; }
    public String getSellReservationId() { return sellReservationId; }
    public Symbol getSymbol() { return symbol; }
    public BigDecimal getExecutedQuantity() { return executedQuantity; }
    public Money getExecutedPrice() { return executedPrice; }

    public Money getTotalValue() {
        return executedPrice.multiply(executedQuantity);
    }
}