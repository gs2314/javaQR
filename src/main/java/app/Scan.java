package app;

public class Scan {
    private final long id;
    private final String createdAt;
    private final String raw;

    public Scan(long id, String createdAt, String raw) {
        this.id = id;
        this.createdAt = createdAt;
        this.raw = raw;
    }

    public long getId() {
        return id;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getRaw() {
        return raw;
    }
}
