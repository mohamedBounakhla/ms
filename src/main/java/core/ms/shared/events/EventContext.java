package core.ms.shared.events;

import core.ms.utils.idgenerator.IdGen;

public class EventContext {
    private static final ThreadLocal<String> currentCorrelation = new ThreadLocal<>();

    public static String getCurrentCorrelationId() {
        String id = currentCorrelation.get();
        if (id == null) {
            id = IdGen.generate("saga");
            currentCorrelation.set(id);
        }
        return id;
    }

    public static void setCorrelationId(String correlationId) {
        currentCorrelation.set(correlationId);
    }

    public static void clear() {
        currentCorrelation.remove();
    }

    public static void startNewSaga() {
        currentCorrelation.set(IdGen.generate("saga"));
    }
}