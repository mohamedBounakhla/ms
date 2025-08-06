package core.ms.order_book.application.services;

import core.ms.order_book.application.dto.command.CreateSnapshotCommand;
import core.ms.order_book.application.dto.query.OrderBookSnapshotDTO;
import core.ms.order_book.application.dto.query.OrderBookStatisticsDTO;
import core.ms.order_book.application.dto.query.OrderSnapshotDTO;
import core.ms.order_book.domain.entities.OrderBook;
import core.ms.order_book.domain.ports.outbound.OrderBookSnapshotRepository;
import core.ms.order_book.domain.value_object.OrderBookSnapshot;
import core.ms.order_book.infrastructure.persistence.OrderBookSnapshotRepositoryImpl;
import core.ms.shared.money.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderBookSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(OrderBookSnapshotService.class);

    @Autowired
    private OrderBookRepository orderBookRepository;

    @Autowired
    private OrderBookSnapshotRepository snapshotRepository;

    @Autowired
    private OrderBookSnapshotRepositoryImpl snapshotRepositoryImpl;

    @Value("${orderbook.snapshot.enabled:true}")
    private boolean snapshotEnabled;

    @Value("${orderbook.snapshot.retention-days:7}")
    private int retentionDays;

    @Value("${orderbook.snapshot.max-snapshots-per-symbol:100}")
    private int maxSnapshotsPerSymbol;

    // ===== SCHEDULED OPERATIONS =====

    @Scheduled(fixedRateString = "${orderbook.snapshot.interval:300000}") // Default 5 minutes
    @Async
    public void scheduledSnapshot() {
        if (!snapshotEnabled) {
            return;
        }

        log.info("Starting scheduled OrderBook snapshot...");
        performSnapshot();
    }

    @Scheduled(cron = "${orderbook.snapshot.cleanup.cron:0 0 2 * * *}") // Default 2 AM daily
    public void cleanupOldSnapshots() {
        if (!snapshotEnabled) {
            return;
        }

        log.info("Starting snapshot cleanup...");
        Instant cutoffTime = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

        try {
            snapshotRepository.deleteByTimestampBefore(cutoffTime);
            log.info("Cleaned up snapshots older than {}", cutoffTime);
        } catch (Exception e) {
            log.error("Failed to cleanup old snapshots", e);
        }
    }

    // ===== PUBLIC OPERATIONS =====

    public void performSnapshot() {
        Collection<OrderBook> orderBooks = orderBookRepository.findAll();
        int successCount = 0;
        int failureCount = 0;

        for (OrderBook orderBook : orderBooks) {
            try {
                if (shouldSnapshot(orderBook)) {
                    createSnapshot(orderBook);
                    successCount++;
                }
            } catch (Exception e) {
                log.error("Failed to snapshot OrderBook for symbol: {}",
                        orderBook.getSymbol().getCode(), e);
                failureCount++;
            }
        }

        log.info("Snapshot completed. Success: {}, Failures: {}", successCount, failureCount);
    }

    public void createSnapshot(String symbolCode) {
        Symbol symbol = createSymbol(symbolCode);
        Optional<OrderBook> orderBook = orderBookRepository.findBySymbol(symbol);

        if (orderBook.isPresent()) {
            createSnapshot(orderBook.get());
            log.info("Snapshot created for symbol: {}", symbolCode);
        } else {
            throw new IllegalArgumentException("OrderBook not found for symbol: " + symbolCode);
        }
    }

    public void createSnapshot(CreateSnapshotCommand command) {
        createSnapshot(command.getSymbolCode());
    }

    // ===== QUERY OPERATIONS =====

    public Optional<OrderBookSnapshotDTO> getLatestSnapshot(String symbolCode) {
        Symbol symbol = createSymbol(symbolCode);
        return snapshotRepository.findLatestBySymbol(symbol)
                .map(this::toDTO);
    }

    public List<OrderBookSnapshotDTO> getSnapshotHistory(String symbolCode,
                                                         LocalDateTime from,
                                                         LocalDateTime to) {
        Symbol symbol = createSymbol(symbolCode);
        Instant startTime = from.toInstant(java.time.ZoneOffset.UTC);
        Instant endTime = to.toInstant(java.time.ZoneOffset.UTC);

        return snapshotRepository.findBySymbolAndTimestampBetween(symbol, startTime, endTime)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ===== RESTORE OPERATIONS =====

    public void restoreFromLatestSnapshot(String symbolCode) {
        Symbol symbol = createSymbol(symbolCode);
        Optional<OrderBookSnapshot> snapshot = snapshotRepository.findLatestBySymbol(symbol);

        if (snapshot.isPresent()) {
            // This would require additional logic to restore OrderBook state
            // For now, just log
            log.info("Found snapshot for {} from {}", symbolCode, snapshot.get().getTimestamp());
        } else {
            log.warn("No snapshot found for symbol: {}", symbolCode);
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    private void createSnapshot(OrderBook orderBook) {
        // Check if we need to cleanup old snapshots for this symbol
        long currentCount = snapshotRepository.countBySymbol(orderBook.getSymbol());
        if (currentCount >= maxSnapshotsPerSymbol) {
            // Would need to implement cleanup of oldest snapshots
            log.warn("Maximum snapshots reached for symbol: {}", orderBook.getSymbol().getCode());
        }

        snapshotRepositoryImpl.createSnapshot(orderBook);
    }

    private boolean shouldSnapshot(OrderBook orderBook) {
        // Only snapshot if there's meaningful data
        return orderBook.getOrderCount() > 0 ||
                orderBook.getLastUpdate().isAfter(Instant.now().minus(30, ChronoUnit.MINUTES));
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

    // ===== DTO MAPPING =====

    private OrderBookSnapshotDTO toDTO(OrderBookSnapshot snapshot) {
        OrderBookSnapshotDTO dto = new OrderBookSnapshotDTO();
        dto.setId(snapshot.getId());
        dto.setSymbolCode(snapshot.getSymbol().getCode());
        dto.setTimestamp(LocalDateTime.ofInstant(snapshot.getTimestamp(),
                java.time.ZoneOffset.UTC));

        // Map buy orders
        dto.setBuyOrders(snapshot.getBuyOrders().stream()
                .map(this::toOrderSnapshotDTO)
                .collect(Collectors.toList()));

        // Map sell orders
        dto.setSellOrders(snapshot.getSellOrders().stream()
                .map(this::toOrderSnapshotDTO)
                .collect(Collectors.toList()));

        // Map statistics
        dto.setStatistics(toStatisticsDTO(snapshot.getStatistics()));

        return dto;
    }

    private OrderSnapshotDTO toOrderSnapshotDTO(OrderBookSnapshot.OrderSnapshot order) {
        OrderSnapshotDTO dto = new OrderSnapshotDTO();
        dto.setOrderId(order.getOrderId());
        dto.setPrice(order.getPrice().getAmount());
        dto.setCurrency(order.getPrice().getCurrency());
        dto.setQuantity(order.getQuantity());
        dto.setRemainingQuantity(order.getRemainingQuantity());
        dto.setCreatedAt(LocalDateTime.ofInstant(order.getCreatedAt(),
                java.time.ZoneOffset.UTC));
        return dto;
    }

    private OrderBookStatisticsDTO toStatisticsDTO(OrderBookSnapshot.OrderBookStatistics stats) {
        OrderBookStatisticsDTO dto = new OrderBookStatisticsDTO();
        dto.setTotalBuyOrders(stats.getTotalBuyOrders());
        dto.setTotalSellOrders(stats.getTotalSellOrders());
        dto.setTotalBuyVolume(stats.getTotalBuyVolume());
        dto.setTotalSellVolume(stats.getTotalSellVolume());

        if (stats.getBestBid() != null) {
            dto.setBestBidPrice(stats.getBestBid().getAmount());
            dto.setPriceCurrency(stats.getBestBid().getCurrency());
        }

        if (stats.getBestAsk() != null) {
            dto.setBestAskPrice(stats.getBestAsk().getAmount());
        }

        if (stats.getSpread() != null) {
            dto.setSpread(stats.getSpread().getAmount());
        }

        return dto;
    }
}