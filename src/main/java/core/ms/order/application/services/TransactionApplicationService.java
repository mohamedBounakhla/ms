package core.ms.order.application.services;

import core.ms.order.application.dto.command.CreateTransactionCommand;
import core.ms.order.application.dto.query.TransactionDTO;
import core.ms.order.application.dto.query.TransactionResultDTO;
import core.ms.order.application.dto.query.TransactionStatisticsDTO;
import core.ms.order.domain.entities.*;
import core.ms.order.domain.ports.inbound.*;
import core.ms.order.domain.ports.outbound.OrderRepository;
import core.ms.order.domain.ports.outbound.TransactionRepository;
import core.ms.order.domain.validators.ValidationErrorMessage;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransactionApplicationService implements TransactionService {

    // Inject infrastructure services (outbound port implementations)
    @Autowired
    private TransactionRepository transactionRepository; // This will be TransactionRepositoryService

    @Autowired
    private OrderRepository orderRepository; // This will be OrderRepositoryService

    @Autowired
    private OrderValidationService orderValidationService; // This will be OrderValidationApplicationService

    // ===== TRANSACTION CREATION =====

    @Override
    public TransactionResult createTransaction(IBuyOrder buyOrder, ISellOrder sellOrder,
                                               Money executionPrice, BigDecimal quantity) {
        try {
            // Validate transaction creation
            List<ValidationErrorMessage> errors = orderValidationService.validateTransactionCreation(
                    buyOrder, sellOrder, executionPrice, quantity);

            if (!errors.isEmpty()) {
                List<String> errorMessages = errors.stream()
                        .map(ValidationErrorMessage::getMessage)
                        .toList();
                return TransactionResult.failure(null, "Transaction validation failed", errorMessages);
            }

            // Generate transaction ID
            String transactionId = generateTransactionId();

            // Create transaction (Domain Entity)
            Transaction transaction = new Transaction(
                    transactionId,
                    buyOrder.getSymbol(),
                    buyOrder,
                    sellOrder,
                    executionPrice,
                    quantity
            );

            // Save transaction (Using Infrastructure Service)
            ITransaction savedTransaction = transactionRepository.save(transaction);

            // Update orders after transaction (Using Infrastructure Service)
            orderRepository.save(buyOrder);
            orderRepository.save(sellOrder);

            return TransactionResult.success(savedTransaction.getId(), "Transaction created successfully");

        } catch (Exception e) {
            return TransactionResult.failure(null, "Failed to create transaction: " + e.getMessage(),
                    List.of(e.getMessage()));
        }
    }

    @Override
    public TransactionResult createTransactionByOrderIds(String buyOrderId, String sellOrderId,
                                                         Money executionPrice, BigDecimal quantity) {
        try {
            // Fetch orders (Using Infrastructure Service)
            Optional<IOrder> buyOrderOpt = orderRepository.findById(buyOrderId);
            Optional<IOrder> sellOrderOpt = orderRepository.findById(sellOrderId);

            if (buyOrderOpt.isEmpty()) {
                return TransactionResult.failure(null, "Buy order not found", List.of("Buy order not found"));
            }
            if (sellOrderOpt.isEmpty()) {
                return TransactionResult.failure(null, "Sell order not found", List.of("Sell order not found"));
            }

            // Validate order types
            if (!(buyOrderOpt.get() instanceof IBuyOrder)) {
                return TransactionResult.failure(null, "Invalid buy order type", List.of("Invalid buy order type"));
            }
            if (!(sellOrderOpt.get() instanceof ISellOrder)) {
                return TransactionResult.failure(null, "Invalid sell order type", List.of("Invalid sell order type"));
            }

            IBuyOrder buyOrder = (IBuyOrder) buyOrderOpt.get();
            ISellOrder sellOrder = (ISellOrder) sellOrderOpt.get();

            // Delegate to the main creation method
            return createTransaction(buyOrder, sellOrder, executionPrice, quantity);

        } catch (Exception e) {
            return TransactionResult.failure(null, "Failed to create transaction: " + e.getMessage(),
                    List.of(e.getMessage()));
        }
    }

    // ===== TRANSACTION QUERIES =====

    @Override
    public Optional<ITransaction> findTransactionById(String transactionId) {
        return transactionRepository.findById(transactionId);
    }

    @Override
    public List<ITransaction> findTransactionsByOrderId(String orderId) {
        return transactionRepository.findByOrderId(orderId);
    }

    @Override
    public List<ITransaction> findTransactionsBySymbol(Symbol symbol) {
        return transactionRepository.findBySymbol(symbol);
    }

    @Override
    public List<ITransaction> findTransactionsByUserId(String userId) {
        // This domain doesn't handle users directly
        return new ArrayList<>();
    }

    @Override
    public List<ITransaction> findTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByDateRange(startDate, endDate);
    }

    @Override
    public List<ITransaction> findTransactionsByPriceRange(Money minPrice, Money maxPrice) {
        // FIXED: Use correct Money method names
        return transactionRepository.findAll().stream()
                .filter(t -> t.getPrice().isGreaterThanOrEqual(minPrice) &&
                        t.getPrice().isLessThanOrEqual(maxPrice))
                .collect(Collectors.toList());
    }

    // ===== TRANSACTION ANALYTICS =====

    @Override
    public TransactionStatistics getTransactionStatistics(Symbol symbol) {
        List<ITransaction> transactions = transactionRepository.findBySymbol(symbol);

        if (transactions.isEmpty()) {
            return new TransactionStatistics(
                    symbol,
                    0L,
                    BigDecimal.ZERO,
                    Money.of(BigDecimal.ZERO, symbol.getQuoteCurrency()),
                    Money.of(BigDecimal.ZERO, symbol.getQuoteCurrency()),
                    Money.of(BigDecimal.ZERO, symbol.getQuoteCurrency()),
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
        }

        long totalTransactions = transactions.size();

        BigDecimal totalVolume = transactions.stream()
                .map(ITransaction::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalValue = transactions.stream()
                .map(t -> t.getPrice().getAmount().multiply(t.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Money averagePrice = Money.of(
                totalValue.divide(totalVolume, 8, RoundingMode.HALF_UP),
                symbol.getQuoteCurrency()
        );

        // FIXED: Use proper Money comparison with custom comparator
        Money highestPrice = transactions.stream()
                .map(ITransaction::getPrice)
                .max(Comparator.comparing(Money::getAmount))
                .orElse(Money.of(BigDecimal.ZERO, symbol.getQuoteCurrency()));

        Money lowestPrice = transactions.stream()
                .map(ITransaction::getPrice)
                .min(Comparator.comparing(Money::getAmount))
                .orElse(Money.of(BigDecimal.ZERO, symbol.getQuoteCurrency()));

        LocalDateTime periodStart = transactions.stream()
                .map(ITransaction::getCreatedAt)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        LocalDateTime periodEnd = transactions.stream()
                .map(ITransaction::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        return new TransactionStatistics(
                symbol,
                totalTransactions,
                totalVolume,
                averagePrice,
                highestPrice,
                lowestPrice,
                periodStart,
                periodEnd
        );
    }

    @Override
    public TransactionVolume getTransactionVolume(Symbol symbol, LocalDateTime startDate, LocalDateTime endDate) {
        List<ITransaction> transactions = transactionRepository.findByDateRange(startDate, endDate)
                .stream()
                .filter(t -> t.getSymbol().equals(symbol))
                .collect(Collectors.toList());

        BigDecimal totalVolume = transactions.stream()
                .map(ITransaction::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TransactionVolume(symbol, totalVolume, startDate, endDate);
    }

    // ===== DTO ORCHESTRATION METHODS =====

    public TransactionResultDTO createTransaction(CreateTransactionCommand command) {
        Money executionPrice = Money.of(command.getExecutionPrice(), command.getCurrency());

        TransactionResult result = createTransactionByOrderIds(
                command.getBuyOrderId(),
                command.getSellOrderId(),
                executionPrice,
                command.getQuantity()
        );

        return mapToTransactionResultDTO(result);
    }

    // DTO Query Methods
    public Optional<TransactionDTO> findTransactionByIdAsDTO(String transactionId) {
        Optional<ITransaction> transaction = findTransactionById(transactionId);
        return transaction.map(this::mapToTransactionDTO);
    }

    public List<TransactionDTO> findTransactionsByOrderIdAsDTO(String orderId) {
        List<ITransaction> transactions = findTransactionsByOrderId(orderId);
        return transactions.stream()
                .map(this::mapToTransactionDTO)
                .collect(Collectors.toList());
    }

    public List<TransactionDTO> findTransactionsBySymbolAsDTO(String symbolCode) {
        Symbol symbol = createSymbol(symbolCode);
        List<ITransaction> transactions = findTransactionsBySymbol(symbol);
        return transactions.stream()
                .map(this::mapToTransactionDTO)
                .collect(Collectors.toList());
    }

    public List<TransactionDTO> findTransactionsByDateRangeAsDTO(LocalDateTime startDate, LocalDateTime endDate) {
        List<ITransaction> transactions = findTransactionsByDateRange(startDate, endDate);
        return transactions.stream()
                .map(this::mapToTransactionDTO)
                .collect(Collectors.toList());
    }

    public TransactionStatisticsDTO getTransactionStatisticsAsDTO(String symbolCode) {
        Symbol symbol = createSymbol(symbolCode);
        TransactionStatistics stats = getTransactionStatistics(symbol);
        return mapToTransactionStatisticsDTO(stats);
    }

    // ===== PRIVATE HELPER METHODS =====

    private String generateTransactionId() {
        return "TXN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private TransactionResultDTO mapToTransactionResultDTO(TransactionResult result) {
        return new TransactionResultDTO(
                result.isSuccess(),
                result.getTransactionId(),
                result.getMessage(),
                result.getTimestamp(),
                result.getErrors()
        );
    }

    private TransactionDTO mapToTransactionDTO(ITransaction transaction) {
        return new TransactionDTO(
                transaction.getId(),
                transaction.getSymbol().getCode(),
                transaction.getSymbol().getName(),
                transaction.getBuyOrder().getId(),
                transaction.getSellOrder().getId(),
                transaction.getPrice().getAmount(),
                transaction.getPrice().getCurrency(),
                transaction.getQuantity(),
                transaction.getTotalValue().getAmount(),
                transaction.getCreatedAt()
        );
    }

    private TransactionStatisticsDTO mapToTransactionStatisticsDTO(TransactionStatistics stats) {
        return new TransactionStatisticsDTO(
                stats.getSymbol().getCode(),
                stats.getTotalTransactions(),
                stats.getTotalVolume(),
                stats.getAveragePrice().getAmount(),
                stats.getAveragePrice().getCurrency(),
                stats.getHighestPrice().getAmount(),
                stats.getLowestPrice().getAmount(),
                stats.getPeriodStart(),
                stats.getPeriodEnd()
        );
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