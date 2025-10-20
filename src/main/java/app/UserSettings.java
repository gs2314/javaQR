package app;

import com.fazecast.jSerialComm.SerialPort;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.prefs.Preferences;

public final class UserSettings {
    private static final String KEY_AUTO_SAVE = "autoSave";
    private static final String KEY_FOCUS_LOCK = "focusLock";
    private static final String KEY_PLACEHOLDERS = "placeholders";
    private static final String KEY_STRIP_AIM = "stripAim";
    private static final String KEY_COLLAPSE_GS = "collapseGs";
    private static final String KEY_ALLOW_LOWER = "allowLower";
    private static final String KEY_HEURISTIC = "heuristic";
    private static final String KEY_DUPLICATE_MS = "duplicateMs";
    private static final String KEY_MAX_LENGTH = "maxLength";
    private static final String KEY_SERIAL_BAUD = "serialBaud";
    private static final String KEY_SERIAL_DATABITS = "serialDataBits";
    private static final String KEY_SERIAL_STOPBITS = "serialStopBits";
    private static final String KEY_SERIAL_PARITY = "serialParity";
    private static final String KEY_SERIAL_IDLE = "serialIdle";
    private static final String KEY_BARCODE_SIZE = "barcodeSize";
    private static final String KEY_BARCODE_MARGIN = "barcodeMargin";
    private static final String KEY_PRINT_SIZE = "printSize";
    private static final String KEY_PRINT_DPI = "printDpi";

    private final Preferences preferences = Preferences.userNodeForPackage(UserSettings.class);

    private boolean autoSaveOnEnter = true;
    private boolean focusLock = false;
    private boolean stripAimId = true;
    private boolean collapseGs = false;
    private boolean allowLowercase = false;
    private boolean heuristicRepair = false;
    private int duplicateSuppressionMs = 1000;
    private int maxScanLength = 256;
    private int serialBaudRate = 9600;
    private int serialDataBits = 8;
    private int serialStopBits = SerialPort.ONE_STOP_BIT;
    private int serialParity = SerialPort.NO_PARITY;
    private int serialIdleTimeoutMs = 80;
    private int barcodePixels = 300;
    private int barcodeMargin = 4;
    private double printSizeMillimetres = 40.0;
    private int printDpi = 300;
    private final List<String> gsPlaceholders = new ArrayList<>();

    private UserSettings() {
        gsPlaceholders.addAll(NormalizationOptions.DefaultPlaceholders.VALUES);
    }

    public static UserSettings load() {
        UserSettings settings = new UserSettings();
        settings.autoSaveOnEnter = settings.preferences.getBoolean(KEY_AUTO_SAVE, true);
        settings.focusLock = settings.preferences.getBoolean(KEY_FOCUS_LOCK, false);
        settings.stripAimId = settings.preferences.getBoolean(KEY_STRIP_AIM, true);
        settings.collapseGs = settings.preferences.getBoolean(KEY_COLLAPSE_GS, false);
        settings.allowLowercase = settings.preferences.getBoolean(KEY_ALLOW_LOWER, false);
        settings.heuristicRepair = settings.preferences.getBoolean(KEY_HEURISTIC, false);
        settings.duplicateSuppressionMs = settings.preferences.getInt(KEY_DUPLICATE_MS, 1000);
        settings.maxScanLength = settings.preferences.getInt(KEY_MAX_LENGTH, 256);
        settings.serialBaudRate = settings.preferences.getInt(KEY_SERIAL_BAUD, 9600);
        settings.serialDataBits = settings.preferences.getInt(KEY_SERIAL_DATABITS, 8);
        settings.serialStopBits = settings.preferences.getInt(KEY_SERIAL_STOPBITS, SerialPort.ONE_STOP_BIT);
        settings.serialParity = settings.preferences.getInt(KEY_SERIAL_PARITY, SerialPort.NO_PARITY);
        settings.serialIdleTimeoutMs = settings.preferences.getInt(KEY_SERIAL_IDLE, 80);
        settings.barcodePixels = settings.preferences.getInt(KEY_BARCODE_SIZE, 300);
        settings.barcodeMargin = settings.preferences.getInt(KEY_BARCODE_MARGIN, 4);
        settings.printSizeMillimetres = settings.preferences.getDouble(KEY_PRINT_SIZE, 40.0);
        settings.printDpi = settings.preferences.getInt(KEY_PRINT_DPI, 300);
        String placeholderBlob = settings.preferences.get(KEY_PLACEHOLDERS, null);
        if (placeholderBlob != null && !placeholderBlob.isBlank()) {
            settings.gsPlaceholders.clear();
            for (String line : placeholderBlob.split("\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    settings.gsPlaceholders.add(trimmed);
                }
            }
        }
        if (settings.gsPlaceholders.isEmpty()) {
            settings.gsPlaceholders.addAll(NormalizationOptions.DefaultPlaceholders.VALUES);
        }
        return settings;
    }

    public void save() {
        preferences.putBoolean(KEY_AUTO_SAVE, autoSaveOnEnter);
        preferences.putBoolean(KEY_FOCUS_LOCK, focusLock);
        preferences.putBoolean(KEY_STRIP_AIM, stripAimId);
        preferences.putBoolean(KEY_COLLAPSE_GS, collapseGs);
        preferences.putBoolean(KEY_ALLOW_LOWER, allowLowercase);
        preferences.putBoolean(KEY_HEURISTIC, heuristicRepair);
        preferences.putInt(KEY_DUPLICATE_MS, duplicateSuppressionMs);
        preferences.putInt(KEY_MAX_LENGTH, maxScanLength);
        preferences.putInt(KEY_SERIAL_BAUD, serialBaudRate);
        preferences.putInt(KEY_SERIAL_DATABITS, serialDataBits);
        preferences.putInt(KEY_SERIAL_STOPBITS, serialStopBits);
        preferences.putInt(KEY_SERIAL_PARITY, serialParity);
        preferences.putInt(KEY_SERIAL_IDLE, serialIdleTimeoutMs);
        preferences.putInt(KEY_BARCODE_SIZE, barcodePixels);
        preferences.putInt(KEY_BARCODE_MARGIN, barcodeMargin);
        preferences.putDouble(KEY_PRINT_SIZE, printSizeMillimetres);
        preferences.putInt(KEY_PRINT_DPI, printDpi);
        preferences.put(KEY_PLACEHOLDERS, String.join("\n", gsPlaceholders));
    }

    public NormalizationOptions toNormalizationOptions() {
        Set<String> placeholders = new LinkedHashSet<>(gsPlaceholders);
        return NormalizationOptions.builder()
                .stripAimId(stripAimId)
                .collapseMultipleGs(collapseGs)
                .gsPlaceholders(placeholders)
                .build();
    }

    public ParseOptions toParseOptions() {
        return ParseOptions.builder()
                .allowLowercase(allowLowercase)
                .heuristicRepair(heuristicRepair)
                .build();
    }

    public boolean isAutoSaveOnEnter() {
        return autoSaveOnEnter;
    }

    public void setAutoSaveOnEnter(boolean autoSaveOnEnter) {
        this.autoSaveOnEnter = autoSaveOnEnter;
    }

    public boolean isFocusLock() {
        return focusLock;
    }

    public void setFocusLock(boolean focusLock) {
        this.focusLock = focusLock;
    }

    public boolean isStripAimId() {
        return stripAimId;
    }

    public void setStripAimId(boolean stripAimId) {
        this.stripAimId = stripAimId;
    }

    public boolean isCollapseGs() {
        return collapseGs;
    }

    public void setCollapseGs(boolean collapseGs) {
        this.collapseGs = collapseGs;
    }

    public boolean isAllowLowercase() {
        return allowLowercase;
    }

    public void setAllowLowercase(boolean allowLowercase) {
        this.allowLowercase = allowLowercase;
    }

    public boolean isHeuristicRepair() {
        return heuristicRepair;
    }

    public void setHeuristicRepair(boolean heuristicRepair) {
        this.heuristicRepair = heuristicRepair;
    }

    public int getDuplicateSuppressionMs() {
        return duplicateSuppressionMs;
    }

    public void setDuplicateSuppressionMs(int duplicateSuppressionMs) {
        this.duplicateSuppressionMs = Math.max(0, duplicateSuppressionMs);
    }

    public int getMaxScanLength() {
        return maxScanLength;
    }

    public void setMaxScanLength(int maxScanLength) {
        this.maxScanLength = Math.max(1, maxScanLength);
    }

    public int getSerialBaudRate() {
        return serialBaudRate;
    }

    public void setSerialBaudRate(int serialBaudRate) {
        this.serialBaudRate = serialBaudRate;
    }

    public int getSerialDataBits() {
        return serialDataBits;
    }

    public void setSerialDataBits(int serialDataBits) {
        this.serialDataBits = serialDataBits;
    }

    public int getSerialStopBits() {
        return serialStopBits;
    }

    public void setSerialStopBits(int serialStopBits) {
        this.serialStopBits = serialStopBits;
    }

    public int getSerialParity() {
        return serialParity;
    }

    public void setSerialParity(int serialParity) {
        this.serialParity = serialParity;
    }

    public int getSerialIdleTimeoutMs() {
        return serialIdleTimeoutMs;
    }

    public void setSerialIdleTimeoutMs(int serialIdleTimeoutMs) {
        this.serialIdleTimeoutMs = Math.max(10, serialIdleTimeoutMs);
    }

    public int getBarcodePixels() {
        return barcodePixels;
    }

    public void setBarcodePixels(int barcodePixels) {
        this.barcodePixels = Math.max(40, barcodePixels);
    }

    public int getBarcodeMargin() {
        return barcodeMargin;
    }

    public void setBarcodeMargin(int barcodeMargin) {
        this.barcodeMargin = Math.max(0, barcodeMargin);
    }

    public double getPrintSizeMillimetres() {
        return printSizeMillimetres;
    }

    public void setPrintSizeMillimetres(double printSizeMillimetres) {
        this.printSizeMillimetres = Math.max(10.0, printSizeMillimetres);
    }

    public int getPrintDpi() {
        return printDpi;
    }

    public void setPrintDpi(int printDpi) {
        this.printDpi = Math.max(150, printDpi);
    }

    public List<String> getGsPlaceholders() {
        return List.copyOf(gsPlaceholders);
    }

    public void setGsPlaceholders(List<String> placeholders) {
        Objects.requireNonNull(placeholders, "placeholders");
        gsPlaceholders.clear();
        for (String placeholder : placeholders) {
            String trimmed = placeholder == null ? "" : placeholder.trim();
            if (!trimmed.isEmpty()) {
                gsPlaceholders.add(trimmed);
            }
        }
        if (gsPlaceholders.isEmpty()) {
            gsPlaceholders.addAll(NormalizationOptions.DefaultPlaceholders.VALUES);
        }
    }
}
