package core.ms.portfolio.domain.entities;

import core.ms.shared.money.Money;

import java.math.BigDecimal;
import java.time.Instant;
import core.ms.shared.money.Symbol;

public class Position {
    private String positionId;
    private Symbol symbol;
    private BigDecimal quantity;
    private Money averageCost;
    private Instant lastUpdated;

    public Position(String positionId, Symbol symbol, BigDecimal quantity, Money averageCost) {
        this.positionId = positionId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.averageCost = averageCost;
        this.lastUpdated = Instant.now();
    }

    public void increase(BigDecimal quantity, Money price) {
        Money currentTotal = averageCost.multiply(this.quantity);
        Money additionalCost = price.multiply(quantity);
        BigDecimal newQuantity = this.quantity.add(quantity);

        this.averageCost = currentTotal.add(additionalCost).divide(newQuantity);
        this.quantity = newQuantity;
        this.lastUpdated = Instant.now();
    }

    public void decrease(BigDecimal quantityToReduce) {
        if (quantityToReduce == null || quantityToReduce.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity to reduce must be positive");
        }

        if (quantityToReduce.compareTo(this.quantity) > 0) {
            throw new IllegalArgumentException(
                    String.format("Cannot reduce position by %s. Current quantity: %s",
                            quantityToReduce, this.quantity)
            );
        }

        this.quantity = this.quantity.subtract(quantityToReduce);
        this.lastUpdated = Instant.now();
    }

    public Money getCurrentValue(Money marketPrice) {
        return marketPrice.multiply(quantity);
    }

    public Money getUnrealizedPnL(Money marketPrice) {
        return getCurrentValue(marketPrice).subtract(averageCost.multiply(quantity));
    }
    public BigDecimal getQuantity() { return this.quantity; }
    public Symbol getSymbol() { return this.symbol; }
}
