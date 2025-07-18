package core.ms.order.domain.ports.inbound;

import core.ms.shared.domain.Symbol;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Value object for transaction volume information
 */
public class TransactionVolume {
    private final Symbol symbol;
    private final BigDecimal volume;
    private final LocalDateTime periodStart;
    private final LocalDateTime periodEnd;

    public TransactionVolume(Symbol symbol, BigDecimal volume,
                             LocalDateTime periodStart, LocalDateTime periodEnd) {
        this.symbol = symbol;
        this.volume = volume;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    // Getters
    public Symbol getSymbol() { return symbol; }
    public BigDecimal getVolume() { return volume; }
    public LocalDateTime getPeriodStart() { return periodStart; }
    public LocalDateTime getPeriodEnd() { return periodEnd; }
}