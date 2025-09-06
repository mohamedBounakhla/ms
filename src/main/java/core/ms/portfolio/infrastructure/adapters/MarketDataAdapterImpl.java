package core.ms.portfolio.infrastructure.adapters;

import core.ms.order_book.application.services.OrderBookApplicationService;
import core.ms.portfolio.domain.ports.outbound.MarketDataAdapter;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class MarketDataAdapterImpl implements MarketDataAdapter {

    @Autowired
    private OrderBookApplicationService orderBookService;

    @Override
    public Optional<Money> getCurrentPrice(Symbol symbol) {
        // Just get the ticker from the order book service!
        var ticker = orderBookService.getOrderBookTicker(symbol);

        if (ticker.getBidPrice() != null && ticker.getAskPrice() != null) {
            // Calculate mid-price
            BigDecimal midPrice = ticker.getBidPrice()
                    .add(ticker.getAskPrice())
                    .divide(new BigDecimal("2"), 8, BigDecimal.ROUND_HALF_UP);

            return Optional.of(Money.of(midPrice, ticker.getCurrency()));
        }

        // No market yet
        return Optional.empty();
    }

    @Override
    public Optional<Money> getLastTradedPrice(Symbol symbol) {
        return getCurrentPrice(symbol);
    }
}