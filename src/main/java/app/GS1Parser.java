package app;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class GS1Parser {
    private static final char GS = 0x1D;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd", Locale.ROOT);

    private GS1Parser() {
    }

    public static ParseResult parse(String normalized, ParseOptions options) {
        Objects.requireNonNull(normalized, "normalized");
        ParseOptions parseOptions = options == null ? ParseOptions.defaults() : options;

        ParseResult result = new ParseResult();
        int index = 0;
        while (index < normalized.length()) {
            char current = normalized.charAt(index);
            if (current == GS) {
                index++;
                continue;
            }
            if (!Character.isDigit(current)) {
                result.addError(new ParseMessage("Unexpected character '" + printable(current) + "'", index));
                break;
            }
            AiMatch match = AiDictionary.match(normalized, index);
            if (match == null) {
                result.addError(new ParseMessage("Unknown AI at position " + index, index));
                break;
            }
            String ai = match.ai();
            AiDefinition definition = match.definition();
            int aiLength = match.length();
            int valueStart = index + aiLength;
            if (definition.fixedLength()) {
                int valueEnd = valueStart + definition.maxLength();
                if (valueEnd > normalized.length()) {
                    result.addError(new ParseMessage("Value for AI (" + ai + ") is shorter than expected", valueStart));
                    break;
                }
                String value = normalized.substring(valueStart, valueEnd);
                ParsedElement element = new ParsedElement(new GS1Element(ai, value), definition, valueStart, valueEnd);
                enforceCharacterSet(element, parseOptions);
                result.addElement(element);
                applyValidation(element);
                index = valueEnd;
            } else {
                int cursor = valueStart;
                while (cursor < normalized.length() && normalized.charAt(cursor) != GS) {
                    cursor++;
                }
                String value = normalized.substring(valueStart, cursor);
                MissingAiInfo missingAi = detectEmbeddedAi(value);
                boolean heuristicsApplied = false;
                if (missingAi != null) {
                    String embeddedAi = missingAi.match().ai();
                    if (parseOptions.heuristicRepair() && ai.equals("10") && embeddedAi.equals("21")) {
                        int candidateValueEnd = missingAi.position();
                        String candidateBatch = value.substring(0, candidateValueEnd);
                        String candidateSerial = value.substring(candidateValueEnd + missingAi.match().length());
                        if (!candidateBatch.isEmpty() && candidateBatch.length() <= definition.maxLength()
                                && candidateSerial.length() >= 1 && candidateSerial.length() <= 20
                                && Character.isLetterOrDigit(candidateSerial.charAt(0))) {
                            value = candidateBatch;
                            index = valueStart + candidateValueEnd;
                            heuristicsApplied = true;
                            result.setHeuristicsApplied();
                            result.addWarning(new ParseMessage("Applied heuristic split between (10) and (21)", index));
                        }
                    }
                    if (!heuristicsApplied) {
                        result.addError(new ParseMessage("Expected GS after variable-length AI (" + ai + ") before AI (" + embeddedAi + ")", valueStart + missingAi.position()));
                    }
                }

                if (value.isEmpty()) {
                    result.addError(new ParseMessage("Value for AI (" + ai + ") cannot be empty", valueStart));
                    break;
                }
                int valueEnd = valueStart + value.length();
                ParsedElement element = new ParsedElement(new GS1Element(ai, value), definition, valueStart, valueEnd);
                if (value.length() < definition.minLength()) {
                    element.addWarning("Value shorter than minimum length " + definition.minLength());
                    element.markInvalid();
                }
                if (value.length() > definition.maxLength()) {
                    element.addWarning("Value longer than maximum length " + definition.maxLength());
                }
                enforceCharacterSet(element, parseOptions);
                result.addElement(element);
                applyValidation(element);
                if (!heuristicsApplied) {
                    index = cursor;
                }
                if (index < normalized.length() && normalized.charAt(index) == GS) {
                    index++;
                }
            }
        }
        return result;
    }

    private static void enforceCharacterSet(ParsedElement element, ParseOptions options) {
        String value = element.element().value();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!element.definition().characterSet().isAllowed(c, options.allowLowercase())) {
                element.addWarning("Character '" + printable(c) + "' not allowed for AI (" + element.element().ai() + ")");
                element.markInvalid();
            }
        }
        if (!options.allowLowercase() && containsLowercase(value)) {
            element.addWarning("Lowercase characters detected");
        }
    }

    private static boolean containsLowercase(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isLowerCase(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static void applyValidation(ParsedElement element) {
        String ai = element.element().ai();
        String value = element.element().value();
        switch (ai) {
            case "01" -> validateGtin(element);
            case "10" -> validateRange(element, 1, 20, "Batch/Lot");
            case "21" -> validateRange(element, 1, 20, "Serial");
            case "11", "12", "13", "15", "16", "17" -> validateDate(element);
            default -> {
                if (ai.startsWith("31") || ai.startsWith("32") || ai.startsWith("33") || ai.startsWith("34")) {
                    element.addWarning("Quantity: " + scaledValue(value, ai.charAt(3)));
                } else if (ai.startsWith("392") || ai.startsWith("393")) {
                    validatePrice(element);
                }
            }
        }
    }

    private static void validatePrice(ParsedElement element) {
        String value = element.element().value();
        if (!value.chars().allMatch(Character::isDigit)) {
            element.addWarning("Price contains non-numeric characters");
            element.markInvalid();
            return;
        }
        int modifier = element.element().ai().charAt(3) - '0';
        if (value.length() > modifier) {
            String integral = value.substring(0, value.length() - modifier);
            String fractional = modifier > 0 ? value.substring(value.length() - modifier) : "";
            element.addWarning("Price preview: " + integral + (modifier > 0 ? ("." + fractional) : ""));
        }
    }

    private static void validateRange(ParsedElement element, int min, int max, String label) {
        int length = element.element().value().length();
        if (length < min || length > max) {
            element.addWarning(label + " length must be between " + min + " and " + max);
            element.markInvalid();
        }
    }

    private static void validateDate(ParsedElement element) {
        String value = element.element().value();
        if (!value.chars().allMatch(Character::isDigit)) {
            element.addWarning("Date contains non-numeric characters");
            element.markInvalid();
            return;
        }
        int month = Integer.parseInt(value.substring(2, 4));
        int day = Integer.parseInt(value.substring(4, 6));
        if (month < 1 || month > 12) {
            element.addWarning("Month out of range: " + month);
            element.markInvalid();
            return;
        }
        if (day < 0 || day > 31) {
            element.addWarning("Day out of range: " + day);
            element.markInvalid();
            return;
        }
        try {
            if (day == 0) {
                YearMonth yearMonth = YearMonth.parse(value.substring(0, 4), DateTimeFormatter.ofPattern("yyMM"));
                LocalDate lastDay = yearMonth.atEndOfMonth();
                element.addWarning("Date: " + lastDay);
            } else {
                LocalDate date = LocalDate.parse(value, DATE_FORMAT);
                element.addWarning("Date: " + date);
            }
        } catch (DateTimeParseException ex) {
            element.addWarning("Invalid date: " + value);
            element.markInvalid();
        }
    }

    private static void validateGtin(ParsedElement element) {
        String value = element.element().value();
        if (!value.chars().allMatch(Character::isDigit)) {
            element.addWarning("GTIN must be numeric");
            element.markInvalid();
            return;
        }
        int expectedCheck = Character.digit(value.charAt(value.length() - 1), 10);
        int calculated = calculateMod10(value.substring(0, value.length() - 1));
        if (expectedCheck == calculated) {
            element.addWarning("GTIN check digit OK");
        } else {
            element.addWarning("GTIN check digit mismatch: expected " + expectedCheck + " calculated " + calculated);
            element.markInvalid();
        }
    }

    private static int calculateMod10(String body) {
        int sum = 0;
        boolean even = true;
        for (int i = body.length() - 1; i >= 0; i--) {
            int digit = body.charAt(i) - '0';
            sum += even ? digit * 3 : digit;
            even = !even;
        }
        int mod = sum % 10;
        return (10 - mod) % 10;
    }

    private static MissingAiInfo detectEmbeddedAi(String value) {
        for (int i = 1; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                continue;
            }
            AiMatch match = AiDictionary.match(value, i);
            if (match != null) {
                return new MissingAiInfo(i, match);
            }
        }
        return null;
    }

    private static String scaledValue(String raw, char modifier) {
        if (!raw.chars().allMatch(Character::isDigit)) {
            return raw;
        }
        int decimals = modifier - '0';
        if (decimals <= 0) {
            return raw;
        }
        if (raw.length() <= decimals) {
            String fraction = "0".repeat(decimals - raw.length()) + raw;
            return "0." + fraction;
        }
        String integral = raw.substring(0, raw.length() - decimals);
        String fraction = raw.substring(raw.length() - decimals);
        return integral + "." + fraction;
    }

    public static String composeGs1(List<GS1Element> elements) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < elements.size(); i++) {
            GS1Element element = elements.get(i);
            builder.append(element.ai());
            builder.append(element.value());
            AiDefinition definition = AiDictionary.lookup(element.ai());
            if (definition != null && !definition.fixedLength() && i < elements.size() - 1) {
                builder.append(GS);
            }
        }
        return builder.toString();
    }

    public static String composeHri(List<GS1Element> elements) {
        StringBuilder builder = new StringBuilder();
        for (GS1Element element : elements) {
            builder.append('(').append(element.ai()).append(')').append(element.value());
        }
        return builder.toString();
    }

    public static String withVisibleControlChars(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(raw.length() * 2);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case GS -> builder.append("<GS>");
                case '\r' -> builder.append("<CR>");
                case '\n' -> builder.append("<LF>");
                case 0x00 -> builder.append("<NUL>");
                case 0x03 -> builder.append("<ETX>");
                default -> builder.append(c);
            }
        }
        return builder.toString();
    }

    public static AiDefinition lookupAi(String ai) {
        return AiDictionary.lookup(ai);
    }

    private static String printable(char c) {
        if (Character.isISOControl(c)) {
            return String.format("0x%02X", (int) c);
        }
        return String.valueOf(c);
    }

    private record MissingAiInfo(int position, AiMatch match) {
    }

    private record AiMatch(String ai, AiDefinition definition, int length) {
    }

    private static final class AiDictionary {
        private static final Map<String, AiDefinition> DEFINITIONS = new HashMap<>();

        static {
            register(new AiDefinition("00", "SSCC", 18, CharacterSet.NUMERIC));
            register(new AiDefinition("01", "GTIN", 14, CharacterSet.NUMERIC));
            register(new AiDefinition("02", "Content GTIN", 14, CharacterSet.NUMERIC));
            register(new AiDefinition("11", "Production date", 6, CharacterSet.NUMERIC));
            register(new AiDefinition("12", "Due date", 6, CharacterSet.NUMERIC));
            register(new AiDefinition("13", "Packaging date", 6, CharacterSet.NUMERIC));
            register(new AiDefinition("15", "Best before", 6, CharacterSet.NUMERIC));
            register(new AiDefinition("16", "Sell by", 6, CharacterSet.NUMERIC));
            register(new AiDefinition("17", "Expiration date", 6, CharacterSet.NUMERIC));
            register(new AiDefinition("20", "Variant", 2, CharacterSet.NUMERIC));
            register(new AiDefinition("30", "Count", 1, 8, CharacterSet.NUMERIC));
            register(new AiDefinition("37", "Units contained", 1, 8, CharacterSet.NUMERIC));
            registerVariableRange(240, 243, "Additional ID", 1, 30, CharacterSet.GS1_ALPHANUMERIC);
            register(new AiDefinition("242", "Made-to-order variation", 1, 6, CharacterSet.NUMERIC));
            registerVariableRange(250, 254, "Reference", 1, 30, CharacterSet.GS1_ALPHANUMERIC);
            registerQuantityFamily("310", "Net weight (kg)");
            registerQuantityFamily("320", "Net weight (lb)");
            registerQuantityFamily("330", "Length (m)");
            registerQuantityFamily("340", "Length (in)");
            registerPriceFamily("392", "Price");
            registerPriceFamily("393", "Price with ISO currency");
            registerRange(400, 426, "Customer data", 1, 30, CharacterSet.GS1_ALPHANUMERIC);
            register(new AiDefinition("8001", "Roll products", 14, CharacterSet.NUMERIC));
            register(new AiDefinition("8002", "Serial within batch", 1, 20, CharacterSet.GS1_ALPHANUMERIC));
            register(new AiDefinition("8003", "GRAI", 14, CharacterSet.NUMERIC));
            register(new AiDefinition("8004", "GIAI", 1, 30, CharacterSet.GS1_ALPHANUMERIC));
        }

        private static void register(AiDefinition definition) {
            DEFINITIONS.put(definition.code(), definition);
        }

        private static void registerVariableRange(int start, int end, String description, int min, int max, CharacterSet set) {
            for (int code = start; code <= end; code++) {
                register(new AiDefinition(String.valueOf(code), description + " (" + code + ")", min, max, set));
            }
        }

        private static void registerRange(int start, int end, String description, int min, int max, CharacterSet set) {
            for (int code = start; code <= end; code++) {
                register(new AiDefinition(String.valueOf(code), description + " (" + code + ")", min, max, set));
            }
        }

        private static void registerQuantityFamily(String prefix, String description) {
            for (int i = 0; i <= 9; i++) {
                String code = prefix + i;
                register(new AiDefinition(code, description + " (10^-" + i + ")", 6, CharacterSet.NUMERIC));
            }
        }

        private static void registerPriceFamily(String prefix, String description) {
            for (int i = 0; i <= 9; i++) {
                String code = prefix + i;
                register(new AiDefinition(code, description + " (10^-" + i + ")", 1, 15, CharacterSet.NUMERIC));
            }
        }

        private static AiDefinition lookup(String ai) {
            return DEFINITIONS.get(ai);
        }

        private static AiMatch match(String value, int index) {
            int remaining = value.length() - index;
            int max = Math.min(4, remaining);
            for (int len = max; len >= 2; len--) {
                String candidate = value.substring(index, index + len);
                AiDefinition definition = DEFINITIONS.get(candidate);
                if (definition != null) {
                    return new AiMatch(candidate, definition, len);
                }
            }
            return null;
        }
    }
}
