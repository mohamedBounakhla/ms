package core.ms.portfolio.domain;

import core.ms.portfolio.domain.value.PortfolioSummary;
import core.ms.shared.domain.Currency;
import core.ms.shared.domain.Money;
import core.ms.shared.domain.Symbol;
import core.ms.utils.IdGenerator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PortfolioManager {
    private final Map<String, Portfolio> portfolios;
    private final Map<String, Set<String>> userPortfolios; // userId -> Set of portfolioIds
    private final IdGenerator idGenerator;

    public PortfolioManager() {
        this.portfolios = new ConcurrentHashMap<>();
        this.userPortfolios = new ConcurrentHashMap<>();
        this.idGenerator = new IdGenerator();
    }

    /**
     * Creates a new portfolio for a user.
     */
    public Portfolio createPortfolio(String name, String userId, Currency baseCurrency) {
        Objects.requireNonNull(name, "Portfolio name cannot be null");
        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(baseCurrency, "Base currency cannot be null");

        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Portfolio name cannot be empty");
        }

        String portfolioId = idGenerator.generateTransactionId(); // Reuse for portfolio IDs
        Money initialCash = Money.zero(baseCurrency);

        Portfolio portfolio = new Portfolio(portfolioId, name.trim(), userId, initialCash);

        portfolios.put(portfolioId, portfolio);
        userPortfolios.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(portfolioId);

        return portfolio;
    }

    /**
     * Creates a new portfolio with initial cash deposit.
     */
    public Portfolio createPortfolioWithInitialDeposit(String name, String userId, Money initialCash) {
        Objects.requireNonNull(initialCash, "Initial cash cannot be null");

        if (initialCash.isNegative()) {
            throw new IllegalArgumentException("Initial cash cannot be negative");
        }

        Portfolio portfolio = createPortfolio(name, userId, initialCash.getCurrency());

        if (initialCash.isPositive()) {
            portfolio.depositCash(initialCash);
        }

        return portfolio;
    }

    /**
     * Retrieves a portfolio by ID.
     */
    public Optional<Portfolio> getPortfolio(String portfolioId) {
        Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null");
        return Optional.ofNullable(portfolios.get(portfolioId));
    }

    /**
     * Retrieves all portfolios for a specific user.
     */
    public List<Portfolio> getUserPortfolios(String userId) {
        Objects.requireNonNull(userId, "User ID cannot be null");

        Set<String> portfolioIds = userPortfolios.getOrDefault(userId, Collections.emptySet());

        return portfolioIds.stream()
                .map(portfolios::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Deletes a portfolio (only if it belongs to the specified user and has no positions).
     */
    public boolean deletePortfolio(String portfolioId, String userId) {
        Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null");
        Objects.requireNonNull(userId, "User ID cannot be null");

        Portfolio portfolio = portfolios.get(portfolioId);
        if (portfolio == null) {
            return false;
        }

        // Verify ownership
        if (!portfolio.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Portfolio does not belong to the specified user");
        }

        // Check if portfolio can be deleted (no positions)
        if (!portfolio.getPositions().isEmpty()) {
            throw new IllegalStateException("Cannot delete portfolio with active positions");
        }

        // Remove from both maps
        portfolios.remove(portfolioId);
        Set<String> userPortfolioSet = userPortfolios.get(userId);
        if (userPortfolioSet != null) {
            userPortfolioSet.remove(portfolioId);
            if (userPortfolioSet.isEmpty()) {
                userPortfolios.remove(userId);
            }
        }

        return true;
    }

    /**
     * Calculates the total value of a portfolio using current market prices.
     */
    public Money calculateTotalValue(Portfolio portfolio, Map<Symbol, Money> currentPrices) {
        Objects.requireNonNull(portfolio, "Portfolio cannot be null");
        Objects.requireNonNull(currentPrices, "Current prices cannot be null");

        return portfolio.getTotalValue(currentPrices);
    }

    /**
     * Generates a comprehensive summary of a portfolio.
     */
    public PortfolioSummary generatePortfolioSummary(Portfolio portfolio, Map<Symbol, Money> currentPrices) {
        Objects.requireNonNull(portfolio, "Portfolio cannot be null");
        Objects.requireNonNull(currentPrices, "Current prices cannot be null");

        Money totalValue = portfolio.getTotalValue(currentPrices);
        Money cashBalance = portfolio.getCashBalance();
        Money totalInvested = portfolio.getTotalInvested();
        Money totalProfitLoss = portfolio.getTotalProfitLoss(currentPrices);

        BigDecimal profitLossPercentage = calculateProfitLossPercentage(totalInvested, totalProfitLoss);

        int positionCount = portfolio.getPositions().size();
        int transactionCount = portfolio.getTransactions().size();

        return new PortfolioSummary(
                portfolio.getId(),
                portfolio.getName(),
                totalValue,
                cashBalance,
                totalInvested,
                totalProfitLoss,
                profitLossPercentage,
                positionCount,
                transactionCount
        );
    }

    /**
     * Gets aggregated statistics across all portfolios for a user.
     */
    public Map<String, Object> getUserPortfolioStatistics(String userId, Map<Symbol, Money> currentPrices) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(currentPrices, "Current prices cannot be null");

        List<Portfolio> userPortfolioList = getUserPortfolios(userId);

        if (userPortfolioList.isEmpty()) {
            return Collections.emptyMap();
        }

        Currency baseCurrency = userPortfolioList.get(0).getBaseCurrency(); // Assume same currency

        Money totalValue = Money.zero(baseCurrency);
        Money totalCash = Money.zero(baseCurrency);
        Money totalInvested = Money.zero(baseCurrency);
        Money totalProfitLoss = Money.zero(baseCurrency);
        int totalPositions = 0;
        int totalTransactions = 0;

        for (Portfolio portfolio : userPortfolioList) {
            totalValue = totalValue.add(portfolio.getTotalValue(currentPrices));
            totalCash = totalCash.add(portfolio.getCashBalance());
            totalInvested = totalInvested.add(portfolio.getTotalInvested());
            totalProfitLoss = totalProfitLoss.add(portfolio.getTotalProfitLoss(currentPrices));
            totalPositions += portfolio.getPositions().size();
            totalTransactions += portfolio.getTransactions().size();
        }

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("userId", userId);
        statistics.put("portfolioCount", userPortfolioList.size());
        statistics.put("totalValue", totalValue);
        statistics.put("totalCash", totalCash);
        statistics.put("totalInvested", totalInvested);
        statistics.put("totalProfitLoss", totalProfitLoss);
        statistics.put("profitLossPercentage", calculateProfitLossPercentage(totalInvested, totalProfitLoss));
        statistics.put("totalPositions", totalPositions);
        statistics.put("totalTransactions", totalTransactions);

        return statistics;
    }

    /**
     * Finds portfolios that contain a specific symbol.
     */
    public List<Portfolio> getPortfoliosWithSymbol(String userId, Symbol symbol) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(symbol, "Symbol cannot be null");

        return getUserPortfolios(userId).stream()
                .filter(portfolio -> portfolio.getPosition(symbol).isPresent())
                .collect(Collectors.toList());
    }

    /**
     * Gets all portfolios (admin function).
     */
    public Collection<Portfolio> getAllPortfolios() {
        return new ArrayList<>(portfolios.values());
    }

    /**
     * Gets portfolio count statistics.
     */
    public Map<String, Integer> getPortfolioStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalPortfolios", portfolios.size());
        stats.put("totalUsers", userPortfolios.size());

        int totalPositions = portfolios.values().stream()
                .mapToInt(p -> p.getPositions().size())
                .sum();
        stats.put("totalPositions", totalPositions);

        int totalTransactions = portfolios.values().stream()
                .mapToInt(p -> p.getTransactions().size())
                .sum();
        stats.put("totalTransactions", totalTransactions);

        return stats;
    }

    private BigDecimal calculateProfitLossPercentage(Money totalInvested, Money totalProfitLoss) {
        if (totalInvested.isZero()) {
            return BigDecimal.ZERO;
        }

        return totalProfitLoss.getAmount()
                .divide(totalInvested.getAmount(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}
