package core.ms.order.application.services;

import core.ms.order.application.dto.query.TransactionDTO;
import core.ms.order.application.dto.query.TransactionResultDTO;
import core.ms.order.application.dto.query.TransactionStatisticsDTO;
import core.ms.order.domain.entities.*;
import core.ms.order.domain.factories.TransactionFactory;
import core.ms.order.domain.ports.inbound.TransactionService;
import core.ms.order.domain.ports.outbound.OrderRepository;
import core.ms.order.domain.ports.outbound.TransactionRepository;
import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionApplicationService implements TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionApplicationService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderEventPublisher eventPublisher;

    // ===== CORE TRANSACTION OPERATIONS (Called by Event Handlers) =====

    @Override
    public TransactionResultDTO createTransaction(IBuyOrder buyOrder, ISellOrder sellOrder,
                                                  Money executionPrice, BigDecimal quantity) {
        try {
            logger.info("Creating transaction - Buy Order: {}, Sell Order: {}, Quantity: {}, Price: {}",
                    buyOrder.getId(), sellOrder.getId(), quantity, executionPrice);

            // Store pre-transaction state
            BigDecimal buyOrderPreExecuted = buyOrder.getExecutedQuantity();
            BigDecimal sellOrderPreExecuted = sellOrder.getExecutedQuantity();

            // Create transaction using factory (includes validation)
            Transaction transaction = TransactionFactory.create(buyOrder, sellOrder, quantity);

            // Save transaction
            ITransaction savedTransaction = transactionRepository.save(transaction);

            // Publish transaction created event to Market Engine
            eventPublisher.publishTransactionCreated(savedTransaction);

            // Check and publish order fill events
            publishOrderFillEvents(buyOrder, buyOrderPreExecuted, quantity, executionPrice, "BUY");
            publishOrderFillEvents(sellOrder, sellOrderPreExecuted, quantity, executionPrice, "SELL");

            // Update orders after transaction
            orderRepository.save(buyOrder);
            orderRepository.save(sellOrder);

            logger.info("Transaction created successfully - Transaction ID: {}", savedTransaction.getId());
            return new TransactionResultDTO(true, savedTransaction.getId(),
                    "Transaction created successfully", LocalDateTime.now(), null);

        } catch (TransactionFactory.TransactionCreationException e) {
            logger.error("Transaction creation failed: {}", e.getMessage());
            return new TransactionResultDTO(false, null,
                    "Transaction creation failed: " + e.getMessage(), LocalDateTime.now(),
                    e.hasValidationErrors() ? e.getErrors() : List.of(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error creating transaction", e);
            return new TransactionResultDTO(false, null,
                    "Failed to create transaction: " + e.getMessage(), LocalDateTime.now(),
                    List.of(e.getMessage()));
        }
    }

    @Override
    public TransactionResultDTO createTransactionByOrderIds(String buyOrderId, String sellOrderId,
                                                            Money executionPrice, BigDecimal quantity) {
        try {
            logger.info("Creating transaction by order IDs - Buy: {}, Sell: {}", buyOrderId, sellOrderId);

            // Fetch orders
            Optional<IOrder> buyOrderOpt = orderRepository.findById(buyOrderId);
            Optional<IOrder> sellOrderOpt = orderRepository.findById(sellOrderId);

            if (buyOrderOpt.isEmpty()) {
                logger.warn("Buy order not found - Order ID: {}", buyOrderId);
                return new TransactionResultDTO(false, null,
                        "Buy order not found", LocalDateTime.now(), List.of("Buy order not found"));
            }
            if (sellOrderOpt.isEmpty()) {
                logger.warn("Sell order not found - Order ID: {}", sellOrderId);
                return new TransactionResultDTO(false, null,
                        "Sell order not found", LocalDateTime.now(), List.of("Sell order not found"));
            }

            // Validate order types
            if (!(buyOrderOpt.get() instanceof IBuyOrder)) {
                logger.error("Invalid buy order type - Order ID: {}", buyOrderId);
                return new TransactionResultDTO(false, null,
                        "Invalid buy order type", LocalDateTime.now(), List.of("Invalid buy order type"));
            }
            if (!(sellOrderOpt.get() instanceof ISellOrder)) {
                logger.error("Invalid sell order type - Order ID: {}", sellOrderId);
                return new TransactionResultDTO(false, null,
                        "Invalid sell order type", LocalDateTime.now(), List.of("Invalid sell order type"));
            }

            IBuyOrder buyOrder = (IBuyOrder) buyOrderOpt.get();
            ISellOrder sellOrder = (ISellOrder) sellOrderOpt.get();

            // Delegate to the main creation method
            return createTransaction(buyOrder, sellOrder, executionPrice, quantity);

        } catch (Exception e) {
            logger.error("Failed to create transaction by order IDs", e);
            return new TransactionResultDTO(false, null,
                    "Failed to create transaction: " + e.getMessage(), LocalDateTime.now(),
                    List.of(e.getMessage()));
        }
    }

    private void publishOrderFillEvents(IOrder order, BigDecimal preExecutedQty,
                                        BigDecimal executedQty, Money executionPrice, String orderType) {
        // Check if order is now partially filled
        if (order.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0 &&
                order.getExecutedQuantity().compareTo(preExecutedQty) > 0) {
            eventPublisher.publishOrderPartiallyFilled(order, orderType, executedQty, executionPrice);
        }

        // Check if order is now completely filled
        if (order.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0 &&
                order.getStatus().getStatus() == OrderStatusEnum.FILLED) {
            eventPublisher.publishOrderFilled(order, orderType, executionPrice);
        }
    }

    // ===== QUERY OPERATIONS (For Monitoring/Internal Use) =====

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

    // ===== ANALYTICS =====

    @Override
    public TransactionStatisticsDTO getTransactionStatistics(Symbol symbol) {
        List<ITransaction> transactions = transactionRepository.findBySymbol(symbol);

        if (transactions.isEmpty()) {
            return new TransactionStatisticsDTO(
                    symbol.getCode(), 0L, BigDecimal.ZERO, BigDecimal.ZERO,
                    symbol.getQuoteCurrency(), BigDecimal.ZERO, BigDecimal.ZERO,
                    LocalDateTime.now(), LocalDateTime.now()
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
                symbol.getCode(), totalTransactions, totalVolume, averagePrice,
                symbol.getQuoteCurrency(), highestPrice, lowestPrice,
                periodStart, periodEnd
        );
    }

    // ===== DTO QUERY METHODS (For Monitoring/Internal Use) =====

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