package core.ms.symbol.resolver;

import core.ms.shared.money.Symbol;
import core.ms.symbol.service.SymbolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SymbolResolver {

    private static SymbolService symbolService;
    private static boolean initialized = false;

    @Autowired
    public void setSymbolService(SymbolService service) {
        SymbolResolver.symbolService = service;
        SymbolResolver.initialized = true;
    }

    public static Symbol resolve(String code) {
        if (!initialized || symbolService == null) {
            throw new IllegalStateException("SymbolResolver not initialized");
        }

        return symbolService.getByCodeOrThrow(code);
    }

    public static boolean isInitialized() {
        return initialized && symbolService != null;
    }
}