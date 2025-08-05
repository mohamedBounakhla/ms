package core.ms.order_book.infrastructure.events.dto;

import core.ms.shared.money.Currency;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderMatchedEventDTO {
    private String buyOrderId;
    private String sellOrderId;
    private Symbol symbol;
    private BigDecimal quantity;
    private BigDecimal executionPrice;
    private Currency currency;
    private BigDecimal totalValue;
    private LocalDateTime occurredAt;

    public OrderMatchedEventDTO() {
        // Default constructor for serialization
    }

    public OrderMatchedEventDTO(String buyOrderId, String sellOrderId, Symbol symbol,
                                BigDecimal quantity, BigDecimal executionPrice, Currency currency,
                                BigDecimal totalValue, LocalDateTime occurredAt) {
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.executionPrice = executionPrice;
        this.currency = currency;
        this.totalValue = totalValue;
        this.occurredAt = occurredAt;
    }

    // Getters and setters
    public String getBuyOrderId() { return buyOrderId; }
    public void setBuyOrderId(String buyOrderId) { this.buyOrderId = buyOrderId; }

    public String getSellOrderId() { return sellOrderId; }
    public void setSellOrderId(String sellOrderId) { this.sellOrderId = sellOrderId; }

    public Symbol getSymbol() { return symbol; }
    public void setSymbol(Symbol symbol) { this.symbol = symbol; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getExecutionPrice() { return executionPrice; }
    public void setExecutionPrice(BigDecimal executionPrice) { this.executionPrice = executionPrice; }

    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }

    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
}
