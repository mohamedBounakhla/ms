package core.ms.order_book.application.dto.query;

import java.time.LocalDateTime;
import java.util.List;

public class OrderBookSnapshotDTO {
    private String id;
    private String symbolCode;
    private List<OrderSnapshotDTO> buyOrders;
    private List<OrderSnapshotDTO> sellOrders;
    private OrderBookStatisticsDTO statistics;
    private LocalDateTime timestamp;

    // Constructors
    public OrderBookSnapshotDTO() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSymbolCode() { return symbolCode; }
    public void setSymbolCode(String symbolCode) { this.symbolCode = symbolCode; }
    public List<OrderSnapshotDTO> getBuyOrders() { return buyOrders; }
    public void setBuyOrders(List<OrderSnapshotDTO> buyOrders) { this.buyOrders = buyOrders; }
    public List<OrderSnapshotDTO> getSellOrders() { return sellOrders; }
    public void setSellOrders(List<OrderSnapshotDTO> sellOrders) { this.sellOrders = sellOrders; }
    public OrderBookStatisticsDTO getStatistics() { return statistics; }
    public void setStatistics(OrderBookStatisticsDTO statistics) { this.statistics = statistics; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
