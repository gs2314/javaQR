package app;

public record GS1Element(String ai, String value) {
    @Override
    public String toString() {
        return "(" + ai + ") " + value;
    }
}
