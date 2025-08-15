package core.ms.portfolio.infrastructure.adapters;

import core.ms.order_book.domain.entities.OrderBook;
import core.ms.order_book.domain.ports.outbound.OrderBookRepository;
import core.ms.portfolio.domain.ports.outbound.MarketDataAdapter;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Random;

@Component
public class MarketDataAdapterImpl implements MarketDataAdapter {

    @Autowired(required = false)
    private OrderBookRepository orderBookRepository;

    // Cache for 30 seconds to reduce load
    @Override
    @Cacheable(value = "marketPrices", key = "#symbol.code")
    public Optional<Money> getCurrentPrice(Symbol symbol) {
        // Try to get price from order book first
        if (orderBookRepository != null) {
            Optional<OrderBook> orderBook = orderBookRepository.findBySymbol(symbol);
            if (orderBook.isPresent()) {
                // Use mid price (average of best bid and best ask)
                Optional<Money> bestBid = orderBook.get().getBestBid();
                Optional<Money> bestAsk = orderBook.get().getBestAsk();

                if (bestBid.isPresent() && bestAsk.isPresent()) {
                    Money midPrice = bestBid.get().add(bestAsk.get()).divide(new BigDecimal("2"));
                    return Optional.of(midPrice);
                } else if (bestBid.isPresent()) {
                    return bestBid;
                } else if (bestAsk.isPresent()) {
                    return bestAsk;
                }
            }
        }

        // Fallback to mock prices for demo purposes
        return getMockPrice(symbol);
    }

    @Override
    @Cacheable(value = "lastTradedPrices", key = "#symbol.code")
    public Optional<Money> getLastTradedPrice(Symbol symbol) {
        // In a real implementation, this would fetch from transaction history
        // For now, return the same as current price
        return getCurrentPrice(symbol);
    }

    // Mock price generation for demo purposes
    private Optional<Money> getMockPrice(Symbol symbol) {
        Random random = new Random();

        return switch (symbol.getCode()) {
            case "BTC" -> Optional.of(Money.of(
                    new BigDecimal("45000").add(
                            new BigDecimal(random.nextInt(10000))
                    ),
                    symbol.getQuoteCurrency()
            ));
            case "ETH" -> Optional.of(Money.of(
                    new BigDecimal("2500").add(
                            new BigDecimal(random.nextInt(500))
                    ),
                    symbol.getQuoteCurrency()
            ));
            case "EURUSD" -> Optional.of(Money.of(
                    new BigDecimal("1.08").add(
                            new BigDecimal(random.nextDouble() * 0.02)
                    ),
                    symbol.getQuoteCurrency()
            ));
            case "GBPUSD" -> Optional.of(Money.of(
                    new BigDecimal("1.25").add(
                            new BigDecimal(random.nextDouble() * 0.02)
                    ),
                    symbol.getQuoteCurrency()
            ));
            default -> Optional.empty();
        };
    }
}