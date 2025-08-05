package core.ms.order.application.services;

import core.ms.order.application.dto.command.CreateTransactionCommand;
import core.ms.order.application.dto.query.TransactionDTO;
import core.ms.order.application.dto.query.TransactionResultDTO;
import core.ms.order.application.dto.query.TransactionStatisticsDTO;
import core.ms.order.domain.entities.*;
import core.ms.order.domain.factories.TransactionFactory;
import core.ms.order.domain.ports.inbound.TransactionService;
import core.ms.order.domain.ports.outbound.OrderRepository;
import core.ms.order.domain.ports.outbound.TransactionRepository;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
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
    private TransactionRepository transactionRepository;

    @Autowired
    private OrderRepository orderRepository;

    // ===== TRANSACTION CREATION =====

    @Override
    public TransactionResultDTO createTransaction(IBuyOrder buyOrder, ISellOrder sellOrder,
                                                  Money executionPrice, BigDecimal quantity) {
        try {
            // Create transaction using factory (includes validation)
            Transaction transaction = TransactionFactory.create(buyOrder, sellOrder, quantity);

            // Save transaction (Using Infrastructure Service)
            ITransaction savedTransaction = transactionRepository.save(transaction);

            // Update orders after transaction (Using Infrastructure Service)
            orderRepository.save(buyOrder);
            orderRepository.save(sellOrder);

            return new TransactionResultDTO(true, savedTransaction.getId(), "Transaction created successfully",
                    LocalDateTime.now(), null);

        } catch (TransactionFactory.TransactionCreationException e) {
            return new TransactionResultDTO(false, null, "Transaction creation failed: " + e.getMessage(),
                    LocalDateTime.now(), e.hasValidationErrors() ? e.getErrors() : List.of(e.getMessage()));
        } catch (Exception e) {
            return new TransactionResultDTO(false, null, "Failed to create transaction: " + e.getMessage(),
                    LocalDateTime.now(), List.of(e.getMessage()));
        }
    }

    @Override
    public TransactionResultDTO createTransactionByOrderIds(String buyOrderId, String sellOrderId,
                                                            Money executionPrice, BigDecimal quantity) {
        try {
            // Fetch orders (Using Infrastructure Service)
            Optional<IOrder> buyOrderOpt = orderRepository.findById(buyOrderId);
            Optional<IOrder> sellOrderOpt = orderRepository.findById(sellOrderId);

            if (buyOrderOpt.isEmpty()) {
                return new TransactionResultDTO(false, null, "Buy order not found",
                        LocalDateTime.now(), List.of("Buy order not found"));
            }
            if (sellOrderOpt.isEmpty()) {
                return new TransactionResultDTO(false, null, "Sell order not found",
                        LocalDateTime.now(), List.of("Sell order not found"));
            }

            // Validate order types
            if (!(buyOrderOpt.get() instanceof IBuyOrder)) {
                return new TransactionResultDTO(false, null, "Invalid buy order type",
                        LocalDateTime.now(), List.of("Invalid buy order type"));
            }
            if (!(sellOrderOpt.get() instanceof ISellOrder)) {
                return new TransactionResultDTO(false, null, "Invalid sell order type",
                        LocalDateTime.now(), List.of("Invalid sell order type"));
            }

            IBuyOrder buyOrder = (IBuyOrder) buyOrderOpt.get();
            ISellOrder sellOrder = (ISellOrder) sellOrderOpt.get();

            // Delegate to the main creation method
            return createTransaction(buyOrder, sellOrder, executionPrice, quantity);

        } catch (Exception e) {
            return new TransactionResultDTO(false, null, "Failed to create transaction: " + e.getMessage(),
                    LocalDateTime.now(), List.of(e.getMessage()));
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
        return transactionRepository.findAll().stream()
                .filter(t -> t.getPrice().isGreaterThanOrEqual(minPrice) &&
                        t.getPrice().isLessThanOrEqual(maxPrice))
                .collect(Collectors.toList());
    }

    // ===== TRANSACTION ANALYTICS =====

    @Override
    public TransactionStatisticsDTO getTransactionStatistics(Symbol symbol) {
        List<ITransaction> transactions = transactionRepository.findBySymbol(symbol);

        if (transactions.isEmpty()) {
            return new TransactionStatisticsDTO(
                    symbol.getCode(),
                    0L,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    symbol.getQuoteCurrency(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
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

        BigDecimal averagePrice = totalValue.divide(totalVolume, 8, RoundingMode.HALF_UP);

        BigDecimal highestPrice = transactions.stream()
                .map(t -> t.getPrice().getAmount())
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal lowestPrice = transactions.stream()
                .map(t -> t.getPrice().getAmount())
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        LocalDateTime periodStart = transactions.stream()
                .map(ITransaction::getCreatedAt)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        LocalDateTime periodEnd = transactions.stream()
                .map(ITransaction::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        return new TransactionStatisticsDTO(
                symbol.getCode(),
                totalTransactions,
                totalVolume,
                averagePrice,
                symbol.getQuoteCurrency(),
                highestPrice,
                lowestPrice,
                periodStart,
                periodEnd
        );
    }

    // ===== DTO ORCHESTRATION METHODS =====

    public TransactionResultDTO createTransaction(CreateTransactionCommand command) {
        Money executionPrice = Money.of(command.getExecutionPrice(), command.getCurrency());

        return createTransactionByOrderIds(
                command.getBuyOrderId(),
                command.getSellOrderId(),
                executionPrice,
                command.getQuantity()
        );
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
        return getTransactionStatistics(symbol);
    }

    // ===== PRIVATE HELPER METHODS =====

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