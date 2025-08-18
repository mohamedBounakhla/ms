package core.ms.order_book.application.dto.query;


import core.ms.shared.money.Currency;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderBookTickerDTO {
    private String symbol;
    private BigDecimal bidPrice;
    private BigDecimal bidQuantity;
    private BigDecimal askPrice;
    private BigDecimal askQuantity;
    private Currency currency;
    private BigDecimal spread;
    private LocalDateTime timestamp;


    public OrderBookTickerDTO() {}

    public OrderBookTickerDTO(String symbol, BigDecimal bidPrice, BigDecimal bidQuantity, BigDecimal askPrice, BigDecimal askQuantity, Currency currency, BigDecimal spread) {
        this.symbol = symbol;
        this.bidPrice = bidPrice;
        this.bidQuantity = bidQuantity;
        this.askPrice = askPrice;
        this.askQuantity = askQuantity;
        this.currency = currency;
        this.spread = spread;
        this.timestamp = LocalDateTime.now();
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getBidPrice() {
        return bidPrice;
    }

    public BigDecimal getBidQuantity() {
        return bidQuantity;
    }

    public BigDecimal getAskPrice() {
        return askPrice;
    }

    public BigDecimal getAskQuantity() {
        return askQuantity;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getSpread() {
        return spread;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setBidPrice(BigDecimal bidPrice) {
        this.bidPrice = bidPrice;
    }

    public void setBidQuantity(BigDecimal bidQuantity) {
        this.bidQuantity = bidQuantity;
    }

    public void setAskPrice(BigDecimal askPrice) {
        this.askPrice = askPrice;
    }

    public void setAskQuantity(BigDecimal askQuantity) {
        this.askQuantity = askQuantity;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public void setSpread(BigDecimal spread) {
        this.spread = spread;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}