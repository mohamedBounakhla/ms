package core.ms.shared.money;

import java.util.Objects;

public class Symbol {
    private final String code;
    private final String name;
    private final AssetType type;
    private final Currency baseCurrency;
    private final Currency quoteCurrency;

    public Symbol(String code, String name, AssetType type, Currency baseCurrency, Currency quoteCurrency) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Code cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("AssetType cannot be null");
        }
        if (baseCurrency == null) {
            throw new IllegalArgumentException("Base currency cannot be null");
        }
        if (quoteCurrency == null) {
            throw new IllegalArgumentException("Quote currency cannot be null");
        }

        this.code = code.toUpperCase().trim();
        this.name = name.trim();
        this.type = type;
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
    }

    // ===== GETTERS =====

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public AssetType getType() {
        return type;
    }

    public Currency getBaseCurrency() {
        return baseCurrency;
    }

    public Currency getQuoteCurrency() {
        return quoteCurrency;
    }

    // ===== BUSINESS METHODS =====

    public String getFullSymbol() {
        return code + "/" + quoteCurrency.name();
    }

    public String getDisplayName() {
        return String.format("%s (%s)", name, code);
    }

    public boolean isCrypto() {
        return type == AssetType.CRYPTO;
    }

    public boolean isStock() {
        return type == AssetType.STOCK;
    }

    public boolean isForex() {
        return type == AssetType.FOREX;
    }

    public boolean isCommodity() {
        return type == AssetType.COMMODITY;
    }

    public boolean isCrossCurrency() {
        return baseCurrency != quoteCurrency;
    }

    // ===== FACTORY METHODS =====

    public static Symbol btcUsd() {
        return new Symbol("BTC", "Bitcoin", AssetType.CRYPTO, Currency.BTC, Currency.USD);
    }

    public static Symbol btcEur() {
        return new Symbol("BTC", "Bitcoin", AssetType.CRYPTO, Currency.BTC, Currency.EUR);
    }

    public static Symbol ethUsd() {
        return new Symbol("ETH", "Ethereum", AssetType.CRYPTO, Currency.ETH, Currency.USD);
    }

    public static Symbol eurUsd() {
        return new Symbol("EURUSD", "Euro US Dollar", AssetType.FOREX, Currency.EUR, Currency.USD);
    }

    public static Symbol gbpUsd() {
        return new Symbol("GBPUSD", "British Pound US Dollar", AssetType.FOREX, Currency.GBP, Currency.USD);
    }

    public static Symbol createFromCode(String symbolCode) {
        return switch (symbolCode.toUpperCase()) {
            case "BTC" -> btcUsd();
            case "ETH" -> ethUsd();
            case "EURUSD" -> eurUsd();
            case "GBPUSD" -> gbpUsd();
            default -> throw new IllegalArgumentException("Unsupported symbol: " + symbolCode);
        };
    }
    // ===== OBJECT METHODS =====

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Symbol symbol = (Symbol) obj;
        return Objects.equals(code, symbol.code) &&
                Objects.equals(name, symbol.name) &&
                type == symbol.type &&
                baseCurrency == symbol.baseCurrency &&
                quoteCurrency == symbol.quoteCurrency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name, type, baseCurrency, quoteCurrency);
    }

    @Override
    public String toString() {
        return getFullSymbol();
    }
}