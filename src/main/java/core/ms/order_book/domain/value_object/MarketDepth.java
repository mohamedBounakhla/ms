package core.ms.order_book.domain.value_object;

import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class MarketDepth {
    private final Symbol symbol;
    private final List<BidPriceLevel> bidLevels;
    private final List<AskPriceLevel> askLevels;
    private final Money spread;
    private final BigDecimal totalBidVolume;
    private final BigDecimal totalAskVolume;
    private final LocalDateTime timestamp;

    public MarketDepth(Symbol symbol, List<BidPriceLevel> bidLevels, List<AskPriceLevel> askLevels) {
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");
        this.bidLevels = new ArrayList<>(Objects.requireNonNull(bidLevels, "Bid levels cannot be null"));
        this.askLevels = new ArrayList<>(Objects.requireNonNull(askLevels, "Ask levels cannot be null"));
        this.timestamp = LocalDateTime.now();

        this.totalBidVolume = calculateBidVolume();
        this.totalAskVolume = calculateAskVolume();
        this.spread = calculateSpread();
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public List<BidPriceLevel> getBidLevels() {
        return new ArrayList<>(bidLevels);
    }

    public List<AskPriceLevel> getAskLevels() {
        return new ArrayList<>(askLevels);
    }

    public Money getSpread() {
        return spread;
    }

    public BigDecimal getTotalBidVolume() {
        return totalBidVolume;
    }

    public BigDecimal getTotalAskVolume() {
        return totalAskVolume;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Optional<BidPriceLevel> getBestBid() {
        return bidLevels.isEmpty() ? Optional.empty() : Optional.of(bidLevels.get(0));
    }

    public Optional<AskPriceLevel> getBestAsk() {
        return askLevels.isEmpty() ? Optional.empty() : Optional.of(askLevels.get(0));
    }

    public boolean isEmpty() {
        return bidLevels.isEmpty() && askLevels.isEmpty();
    }

    public int getLevelCount() {
        return bidLevels.size() + askLevels.size();
    }

    private BigDecimal calculateBidVolume() {
        return bidLevels.stream()
                .map(BidPriceLevel::getTotalQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateAskVolume() {
        return askLevels.stream()
                .map(AskPriceLevel::getTotalQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Money calculateSpread() {
        Optional<BidPriceLevel> bestBid = getBestBid();
        Optional<AskPriceLevel> bestAsk = getBestAsk();

        if (bestBid.isPresent() && bestAsk.isPresent()) {
            return bestAsk.get().getPrice().subtract(bestBid.get().getPrice());
        }
        return null; // No spread when missing bid or ask
    }
}
