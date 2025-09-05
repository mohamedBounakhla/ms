package core.ms.config;

import core.ms.order.domain.entities.IBuyOrder;
import core.ms.order.domain.entities.ISellOrder;
import core.ms.order.domain.factories.OrderFactory;
import core.ms.order.domain.ports.outbound.OrderRepository;
import core.ms.order_book.application.services.OrderBookApplicationService;
import core.ms.portfolio.application.dto.command.CreatePortfolioCommand;
import core.ms.portfolio.application.dto.command.DepositCashCommand;
import core.ms.portfolio.application.services.PortfolioApplicationService;
import core.ms.security.domain.MSUserRole;
import core.ms.security.exception.UsernameAlreadyExistException;
import core.ms.security.service.MSUserService;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Money;
import core.ms.shared.money.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@Order(10) // Run after symbols
public class TestDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TestDataInitializer.class);

    @Autowired
    private MSUserService userService;

    @Autowired
    private PortfolioApplicationService portfolioService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderBookApplicationService orderBookService;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("=== Starting Test Data Initialization ===");

        // Initialize users with portfolios
        initializeUsers();

        // Initialize BTC order book with pending orders
        initializeBTCOrderBook();

        log.info("=== Test Data Initialization Complete ===");
    }

    private void initializeUsers() {
        log.info("Initializing test users...");

        createUser("trader1", "password", "500000", "portfolio-trader1");
        createUser("trader2", "password", "200000", "portfolio-trader2");
        createUser("trader3", "password", "100000", "portfolio-trader3");
        createUser("cnam1", "password", "100000", "portfolio-cnam1");
        createUser("cnam2", "password", "50000", "portfolio-cnam2");

        log.info("Test users initialized");
    }

    private void initializeBTCOrderBook() {
        log.info("Initializing BTC order book with pending orders...");

        Symbol btc = Symbol.btcUsd();

        // Create buy orders (bids) - descending prices
        createAndAddBuyOrder("portfolio-trader1", btc, "44950", "0.5");
        createAndAddBuyOrder("portfolio-trader2", btc, "44900", "0.3");
        createAndAddBuyOrder("portfolio-trader3", btc, "44850", "0.2");
        createAndAddBuyOrder("portfolio-trader1", btc, "44800", "0.4");
        createAndAddBuyOrder("portfolio-cnam1", btc, "44750", "0.1");
        createAndAddBuyOrder("portfolio-trader2", btc, "44700", "0.6");
        createAndAddBuyOrder("portfolio-trader3", btc, "44650", "0.25");
        createAndAddBuyOrder("portfolio-trader1", btc, "44600", "0.35");
        createAndAddBuyOrder("portfolio-trader2", btc, "44550", "0.15");
        createAndAddBuyOrder("portfolio-trader3", btc, "44500", "0.45");

        // Create sell orders (asks) - ascending prices
        createAndAddSellOrder("portfolio-trader1", btc, "45050", "0.2");
        createAndAddSellOrder("portfolio-trader2", btc, "45100", "0.3");
        createAndAddSellOrder("portfolio-trader3", btc, "45150", "0.15");
        createAndAddSellOrder("portfolio-trader1", btc, "45200", "0.4");
        createAndAddSellOrder("portfolio-trader2", btc, "45250", "0.25");
        createAndAddSellOrder("portfolio-trader3", btc, "45300", "0.5");
        createAndAddSellOrder("portfolio-trader1", btc, "45350", "0.35");
        createAndAddSellOrder("portfolio-trader2", btc, "45400", "0.6");
        createAndAddSellOrder("portfolio-trader3", btc, "45450", "0.45");
        createAndAddSellOrder("portfolio-trader1", btc, "45500", "0.3");

        log.info("BTC order book initialized with 20 pending orders");
    }

    private void createAndAddBuyOrder(String portfolioId, Symbol symbol, String price, String quantity) {
        try {
            String reservationId = "res-buy-" + System.nanoTime();

            IBuyOrder buyOrder = OrderFactory.createBuyOrder(
                    portfolioId,
                    reservationId,
                    symbol,
                    Money.of(new BigDecimal(price), Currency.USD),
                    new BigDecimal(quantity)
            );

            // Save to database
            orderRepository.save(buyOrder);

            // Add to order book (critical step!)
            orderBookService.addOrderToBook(buyOrder);

            log.debug("Added buy order: {} {} @ ${}", quantity, symbol.getCode(), price);

        } catch (Exception e) {
            log.error("Failed to create buy order: {}", e.getMessage());
        }
    }

    private void createAndAddSellOrder(String portfolioId, Symbol symbol, String price, String quantity) {
        try {
            String reservationId = "res-sell-" + System.nanoTime();

            ISellOrder sellOrder = OrderFactory.createSellOrder(
                    portfolioId,
                    reservationId,
                    symbol,
                    Money.of(new BigDecimal(price), Currency.USD),
                    new BigDecimal(quantity)
            );

            // Save to database
            orderRepository.save(sellOrder);

            // Add to order book (critical step!)
            orderBookService.addOrderToBook(sellOrder);

            log.debug("Added sell order: {} {} @ ${}", quantity, symbol.getCode(), price);

        } catch (Exception e) {
            log.error("Failed to create sell order: {}", e.getMessage());
        }
    }

    private void createUser(String username, String password, String initialBalance, String portfolioId) {
        try {
            // Check if portfolio already exists
            if (portfolioService.findPortfolioById(portfolioId).isPresent()) {
                log.debug("User {} already exists, skipping", username);
                return;
            }

            // Create user
            try {
                userService.createUser(username, password, MSUserRole.CUSTOMER);
            } catch (UsernameAlreadyExistException e) {
                log.debug("User {} already exists in auth system", username);
            }

            // Create portfolio
            portfolioService.createPortfolio(new CreatePortfolioCommand(portfolioId, username));

            // Add initial funds
            portfolioService.depositCash(new DepositCashCommand(
                    portfolioId,
                    new BigDecimal(initialBalance),
                    Currency.USD
            ));

            log.info("Created user {} with ${} balance", username, initialBalance);

        } catch (Exception e) {
            log.error("Error setting up user {}: {}", username, e.getMessage());
        }
    }
}