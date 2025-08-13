package core.ms.portfolio.domain.value_object;

import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;


public class ResourceConsumption {
    private final Money cashToConsume;
    private final Symbol assetSymbol;
    private final BigDecimal assetQuantity;

    // For cash consumption
    public static ResourceConsumption forCash(Money amount) {
        return new ResourceConsumption(amount, null, null);
    }

    // For asset consumption
    public static ResourceConsumption forAsset(Symbol symbol, BigDecimal quantity) {
        return new ResourceConsumption(null, symbol, quantity);
    }

    private ResourceConsumption(Money cashToConsume, Symbol assetSymbol, BigDecimal assetQuantity) {
        this.cashToConsume = cashToConsume;
        this.assetSymbol = assetSymbol;
        this.assetQuantity = assetQuantity;
    }

    public boolean isCashConsumption() { return cashToConsume != null; }
    public boolean isAssetConsumption() { return assetSymbol != null; }

    public Money getCashToConsume() { return cashToConsume; }
    public Symbol getAssetSymbol() { return assetSymbol; }
    public BigDecimal getAssetQuantity() { return assetQuantity; }
}