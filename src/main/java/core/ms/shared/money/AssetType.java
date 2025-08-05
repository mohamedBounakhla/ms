package core.ms.shared.money;

public enum AssetType {
    CRYPTO("Cryptocurrency"),
    STOCK("Stock"),
    FOREX("Foreign Exchange"),
    COMMODITY("Commodity");

    private final String displayName;

    AssetType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}