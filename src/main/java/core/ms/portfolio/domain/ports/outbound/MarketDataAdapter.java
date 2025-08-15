package core.ms.portfolio.domain.ports.outbound;

import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.util.Optional;

public interface MarketDataAdapter {
    Optional<Money> getCurrentPrice(Symbol symbol);
    Optional<Money> getLastTradedPrice(Symbol symbol);
}