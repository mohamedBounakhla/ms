package core.ms.order_book.application.dto.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class MarketOverviewDTO {
    private List<String> activeSymbols;
    private int totalOrderBooks;
    private int totalOrders;
    private Map<String, BigDecimal> totalVolumeBySymbol;
    private LocalDateTime timestamp;

    public MarketOverviewDTO() {}

    public MarketOverviewDTO(List<String> activeSymbols, int totalOrderBooks, int totalOrders,
                             Map<String, BigDecimal> totalVolumeBySymbol, LocalDateTime timestamp) {
        this.activeSymbols = activeSymbols;
        this.totalOrderBooks = totalOrderBooks;
        this.totalOrders = totalOrders;
        this.totalVolumeBySymbol = totalVolumeBySymbol;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public List<String> getActiveSymbols() { return activeSymbols; }
    public void setActiveSymbols(List<String> activeSymbols) { this.activeSymbols = activeSymbols; }
    public int getTotalOrderBooks() { return totalOrderBooks; }
    public void setTotalOrderBooks(int totalOrderBooks) { this.totalOrderBooks = totalOrderBooks; }
    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }
    public Map<String, BigDecimal> getTotalVolumeBySymbol() { return totalVolumeBySymbol; }
    public void setTotalVolumeBySymbol(Map<String, BigDecimal> totalVolumeBySymbol) {
        this.totalVolumeBySymbol = totalVolumeBySymbol;
    }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}