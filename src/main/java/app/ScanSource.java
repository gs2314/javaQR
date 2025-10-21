package app;

public enum ScanSource {
    HID("HID"),
    SERIAL("Serial"),
    IMPORT("Import"),
    MANUAL("Manual");

    private final String display;

    ScanSource(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }
}
