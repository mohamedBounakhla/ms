package core.ms.utils.idgenerator;

public abstract class BaseIdGenerator {

    protected abstract String getPrefix();

    public String generate() {
        return getPrefix() + createUniqueId();
    }

    private String createUniqueId() {
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 10000);
        return timestamp + "-" + String.format("%04d", random);
    }
}