package core.ms.symbol.config;

import core.ms.shared.money.AssetType;
import core.ms.shared.money.Currency;
import core.ms.symbol.domain.SymbolEntity;
import core.ms.symbol.dao.SymbolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;

@Component
@Order(1)
public class SymbolDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SymbolDataInitializer.class);

    @Autowired
    private SymbolRepository symbolRepository;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Checking symbol data...");

        // Always ensure these symbols exist
        ensureSymbolExists("BTC", "Bitcoin", "Bitcoin cryptocurrency",
                AssetType.CRYPTO, Currency.BTC, Currency.USD,
                new BigDecimal("0.0001"), new BigDecimal("100"),
                new BigDecimal("0.01"), new BigDecimal("0.0001"));

        ensureSymbolExists("ETH", "Ethereum", "Ethereum cryptocurrency",
                AssetType.CRYPTO, Currency.ETH, Currency.USD,
                new BigDecimal("0.001"), new BigDecimal("1000"),
                new BigDecimal("0.01"), new BigDecimal("0.001"));

        ensureSymbolExists("EURUSD", "Euro/US Dollar", "EUR/USD forex pair",
                AssetType.FOREX, Currency.EUR, Currency.USD,
                new BigDecimal("1000"), new BigDecimal("1000000"),
                new BigDecimal("0.0001"), new BigDecimal("1000"));

        ensureSymbolExists("GBPUSD", "British Pound/US Dollar", "GBP/USD forex pair",
                AssetType.FOREX, Currency.GBP, Currency.USD,
                new BigDecimal("1000"), new BigDecimal("1000000"),
                new BigDecimal("0.0001"), new BigDecimal("1000"));

        logger.info("Symbol data initialization completed. {} symbols available.",
                symbolRepository.count());
    }
    private void ensureSymbolExists(String code, String name, String description,
                                    AssetType assetType, Currency baseCurrency,
                                    Currency quoteCurrency, BigDecimal minOrderSize,
                                    BigDecimal maxOrderSize, BigDecimal tickSize,
                                    BigDecimal lotSize) {
        if (!symbolRepository.existsById(code)) {
            SymbolEntity symbol = new SymbolEntity(
                    code, name, description, assetType,
                    baseCurrency, quoteCurrency, minOrderSize,
                    maxOrderSize, tickSize, lotSize
            );
            symbolRepository.save(symbol);
            logger.info("Created symbol: {}", code);
        } else {
            logger.debug("Symbol already exists: {}", code);
        }
    }
}