package core.ms.order.web.controllers;

import core.ms.order.application.services.TransactionQueryService;
import core.ms.shared.money.Symbol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/internal/transactions")
public class TransactionController {

    @Autowired
    private TransactionQueryService transactionService;

    @GetMapping("/history")
    public List<TransactionDataDTO> getTransactionHistory(
            @RequestParam String symbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        Symbol domainSymbol = Symbol.createFromCode(symbol);

        return transactionService.findTransactionsByDateRange(from, to).stream()
                .filter(tx -> tx.getSymbol().equals(domainSymbol))
                .map(tx -> new TransactionDataDTO(
                        tx.getId(),
                        tx.getPrice().getAmount(),
                        tx.getQuantity(),
                        tx.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    public static class TransactionDataDTO {
        private String id;
        private BigDecimal price;
        private BigDecimal quantity;
        private LocalDateTime timestamp;

        public TransactionDataDTO() {}

        public TransactionDataDTO(String id, BigDecimal price, BigDecimal quantity, LocalDateTime timestamp) {
            this.id = id;
            this.price = price;
            this.quantity = quantity;
            this.timestamp = timestamp;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
