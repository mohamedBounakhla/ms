package core.ms.order.application.services;

import core.ms.order.domain.entities.ITransaction;
import core.ms.order.domain.ports.inbound.TransactionService;
import core.ms.order.domain.ports.outbound.TransactionRepository;
import core.ms.shared.money.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TransactionQueryService implements TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionQueryService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    public Optional<ITransaction> findTransactionById(String transactionId) {
        logger.debug("Finding transaction by ID: {}", transactionId);
        return transactionRepository.findById(transactionId);
    }

    @Override
    public List<ITransaction> findTransactionsByOrderId(String orderId) {
        logger.debug("Finding transactions for order: {}", orderId);
        return transactionRepository.findByOrderId(orderId);
    }

    @Override
    public List<ITransaction> findTransactionsByPortfolioId(String portfolioId) {
        logger.debug("Finding transactions for portfolio: {}", portfolioId);
        List<ITransaction> allTransactions = transactionRepository.findAll();
        return allTransactions.stream()
                .filter(t -> portfolioId.equals(t.getBuyOrder().getPortfolioId()) ||
                        portfolioId.equals(t.getSellOrder().getPortfolioId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ITransaction> findTransactionsBySymbol(Symbol symbol) {
        logger.debug("Finding transactions for symbol: {}", symbol.getCode());
        return transactionRepository.findBySymbol(symbol);
    }

    @Override
    public List<ITransaction> findTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("Finding transactions between {} and {}", startDate, endDate);
        return transactionRepository.findByDateRange(startDate, endDate);
    }

    @Override
    public long getTotalTransactionCount() {
        logger.debug("Getting total transaction count");
        return transactionRepository.count();
    }

    @Override
    public BigDecimal getTransactionVolume(Symbol symbol) {
        logger.debug("Getting transaction volume for symbol: {}", symbol.getCode());
        List<ITransaction> transactions = transactionRepository.findBySymbol(symbol);
        return transactions.stream()
                .map(ITransaction::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}