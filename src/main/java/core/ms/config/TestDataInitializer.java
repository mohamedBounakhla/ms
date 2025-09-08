package core.ms.config;

import core.ms.portfolio.application.dto.command.CreatePortfolioCommand;
import core.ms.portfolio.application.dto.command.DepositCashCommand;
import core.ms.portfolio.application.dto.command.DepositAssetCommand;
import core.ms.portfolio.application.services.PortfolioApplicationService;
import core.ms.security.domain.MSUserRole;
import core.ms.security.exception.UsernameAlreadyExistException;
import core.ms.security.service.MSUserService;
import core.ms.shared.money.Currency;
import core.ms.shared.money.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@Order(10) // Run after symbols
public class TestDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TestDataInitializer.class);

    @Autowired
    private MSUserService userService;

    @Autowired
    private PortfolioApplicationService portfolioService;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("=== Starting Minimal Test Data Initialization ===");

        // Initialize only 2 test users with portfolios
        initializeUsers();

        log.info("=== Test Data Initialization Complete ===");
    }

    private void initializeUsers() {
        log.info("Initializing test users...");

        // Create only 2 test accounts with different asset allocations
        createUser("trader1", "password", "100000", "10", "portfolio-trader1");
        createUser("trader2", "password", "100000", "5", "portfolio-trader2");

        log.info("Test users initialized: trader1 and trader2");
    }

    private void createUser(String username, String password, String initialBalance,
                            String btcAmount, String portfolioId) {
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

            // Add initial USD funds
            portfolioService.depositCash(new DepositCashCommand(
                    portfolioId,
                    new BigDecimal(initialBalance),
                    Currency.USD
            ));

            // Add initial BTC assets
            DepositAssetCommand assetCommand = new DepositAssetCommand(
                    portfolioId,
                    Symbol.btcUsd(),  // Using the BTC/USD symbol
                    new BigDecimal(btcAmount)
            );
            portfolioService.depositAsset(assetCommand);

            log.info("Created user {} with ${} balance and {} BTC",
                    username, initialBalance, btcAmount);

        } catch (Exception e) {
            log.error("Error setting up user {}: {}", username, e.getMessage());
        }
    }
}