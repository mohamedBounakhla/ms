package core.ms.order_book.infrastructure.persistence.mappers;

import core.ms.order.domain.entities.IOrder;
import core.ms.order_book.domain.entities.OrderBook;
import core.ms.order_book.domain.value_object.AskPriceLevel;
import core.ms.order_book.domain.value_object.BidPriceLevel;
import core.ms.order_book.domain.value_object.OrderBookSnapshot;
import core.ms.order_book.infrastructure.persistence.entities.OrderBookSnapshotEntity;
import core.ms.order_book.infrastructure.persistence.entities.OrderBookStatisticsEntity;
import core.ms.order_book.infrastructure.persistence.entities.OrderSnapshotEntity;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderBookSnapshotMapper {

    public OrderBookSnapshotEntity toEntity(OrderBook orderBook) {
        String snapshotId = generateSnapshotId(orderBook.getSymbol());
        OrderBookSnapshotEntity entity = new OrderBookSnapshotEntity(
                snapshotId,
                orderBook.getSymbol().getCode(),
                orderBook.getLastUpdate()
        );

        // Add buy orders
        for (BidPriceLevel level : orderBook.getBidLevels()) {
            for (IOrder order : level.getActiveOrders()) {
                OrderSnapshotEntity orderSnapshot = createOrderSnapshot(order, "BUY");
                entity.addOrder(orderSnapshot);
            }
        }

        // Add sell orders
        for (AskPriceLevel level : orderBook.getAskLevels()) {
            for (IOrder order : level.getActiveOrders()) {
                OrderSnapshotEntity orderSnapshot = createOrderSnapshot(order, "SELL");
                entity.addOrder(orderSnapshot);
            }
        }

        // Add statistics
        entity.setStatistics(createStatistics(orderBook));

        return entity;
    }

    public OrderBookSnapshot toDomain(OrderBookSnapshotEntity entity) {
        Symbol symbol = createSymbol(entity.getSymbolCode());

        List<OrderBookSnapshot.OrderSnapshot> buyOrders = entity.getOrders().stream()
                .filter(o -> "BUY".equals(o.getOrderType()))
                .map(this::toOrderSnapshot)
                .collect(Collectors.toList());

        List<OrderBookSnapshot.OrderSnapshot> sellOrders = entity.getOrders().stream()
                .filter(o -> "SELL".equals(o.getOrderType()))
                .map(this::toOrderSnapshot)
                .collect(Collectors.toList());

        OrderBookSnapshot.OrderBookStatistics stats = toStatistics(entity.getStatistics());

        return new OrderBookSnapshot(
                entity.getId(),
                symbol,
                buyOrders,
                sellOrders,
                stats
        );
    }

    private OrderSnapshotEntity createOrderSnapshot(IOrder order, String orderType) {
        OrderSnapshotEntity entity = new OrderSnapshotEntity();
        entity.setOrderId(order.getId());
        entity.setOrderType(orderType);
        entity.setPrice(order.getPrice().getAmount());
        entity.setCurrency(order.getPrice().getCurrency());
        entity.setQuantity(order.getQuantity());
        entity.setRemainingQuantity(order.getRemainingQuantity());
        entity.setCreatedAt(order.getCreatedAt());
        return entity;
    }

    private OrderBookSnapshot.OrderSnapshot toOrderSnapshot(OrderSnapshotEntity entity) {
        Money price = Money.of(entity.getPrice(), entity.getCurrency());
        return new OrderBookSnapshot.OrderSnapshot(
                entity.getOrderId(),
                price,
                entity.getQuantity(),
                entity.getRemainingQuantity(),
                entity.getCreatedAt()
        );
    }

    private OrderBookStatisticsEntity createStatistics(OrderBook orderBook) {
        OrderBookStatisticsEntity stats = new OrderBookStatisticsEntity();

        stats.setTotalBuyOrders(orderBook.getBidLevels().stream()
                .mapToInt(level -> level.getOrderCount())
                .sum());

        stats.setTotalSellOrders(orderBook.getAskLevels().stream()
                .mapToInt(level -> level.getOrderCount())
                .sum());

        stats.setTotalBuyVolume(orderBook.getTotalBidVolume());
        stats.setTotalSellVolume(orderBook.getTotalAskVolume());

        orderBook.getBestBid().ifPresent(bid -> {
            stats.setBestBidPrice(bid.getAmount());
            stats.setPriceCurrency(bid.getCurrency());
        });

        orderBook.getBestAsk().ifPresent(ask -> {
            stats.setBestAskPrice(ask.getAmount());
        });

        orderBook.getSpread().ifPresent(spread -> {
            stats.setSpread(spread.getAmount());
        });

        return stats;
    }

    private OrderBookSnapshot.OrderBookStatistics toStatistics(OrderBookStatisticsEntity entity) {
        Money bestBid = entity.getBestBidPrice() != null ?
                Money.of(entity.getBestBidPrice(), entity.getPriceCurrency()) : null;
        Money bestAsk = entity.getBestAskPrice() != null ?
                Money.of(entity.getBestAskPrice(), entity.getPriceCurrency()) : null;
        Money spread = entity.getSpread() != null ?
                Money.of(entity.getSpread(), entity.getPriceCurrency()) : null;

        return new OrderBookSnapshot.OrderBookStatistics(
                entity.getTotalBuyOrders(),
                entity.getTotalSellOrders(),
                entity.getTotalBuyVolume(),
                entity.getTotalSellVolume(),
                bestBid,
                bestAsk,
                spread
        );
    }

    private String generateSnapshotId(Symbol symbol) {
        return String.format("SNAPSHOT-%s-%d",
                symbol.getCode(),
                System.currentTimeMillis());
    }

    private Symbol createSymbol(String symbolCode) {
        return switch (symbolCode.toUpperCase()) {
            case "BTC" -> Symbol.btcUsd();
            case "ETH" -> Symbol.ethUsd();
            case "EURUSD" -> Symbol.eurUsd();
            case "GBPUSD" -> Symbol.gbpUsd();
            default -> throw new IllegalArgumentException("Unsupported symbol: " + symbolCode);
        };
    }
}
