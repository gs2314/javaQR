package app;

import java.util.Collections;
import java.util.List;

public class Scan {
    private final long id;
    private final String createdAt;
    private final String raw;
    private final String normalized;
    private final String source;
    private final String port;
    private final String symbologyId;
    private final boolean parseOk;
    private final String error;
    private final List<ScanElement> elements;

    public Scan(long id,
                String createdAt,
                String raw,
                String normalized,
                String source,
                String port,
                String symbologyId,
                boolean parseOk,
                String error,
                List<ScanElement> elements) {
        this.id = id;
        this.createdAt = createdAt;
        this.raw = raw;
        this.normalized = normalized;
        this.source = source;
        this.port = port;
        this.symbologyId = symbologyId;
        this.parseOk = parseOk;
        this.error = error;
        this.elements = elements == null ? List.of() : List.copyOf(elements);
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

    public String getNormalized() {
        return normalized;
    }

    public String getSource() {
        return source;
    }

    public String getPort() {
        return port;
    }

    public String getSymbologyId() {
        return symbologyId;
    }

    public boolean isParseOk() {
        return parseOk;
    }

    public String getError() {
        return error;
    }

    public List<ScanElement> getElements() {
        return elements;
    }
}
