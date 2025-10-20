package app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GS1Parser {
    private static final Map<String, Integer> FIXED_LENGTH_AIS = new HashMap<>();
    private static final Set<String> VARIABLE_LENGTH_AIS = new HashSet<>();

    static {
        FIXED_LENGTH_AIS.put("00", 18);
        FIXED_LENGTH_AIS.put("01", 14);
        FIXED_LENGTH_AIS.put("11", 6);
        FIXED_LENGTH_AIS.put("13", 6);
        FIXED_LENGTH_AIS.put("15", 6);
        FIXED_LENGTH_AIS.put("17", 6);

        VARIABLE_LENGTH_AIS.add("10");
        VARIABLE_LENGTH_AIS.add("21");
        VARIABLE_LENGTH_AIS.add("240");
        VARIABLE_LENGTH_AIS.add("241");
        VARIABLE_LENGTH_AIS.add("242");
        VARIABLE_LENGTH_AIS.add("243");
        VARIABLE_LENGTH_AIS.add("250");
        VARIABLE_LENGTH_AIS.add("251");
        VARIABLE_LENGTH_AIS.add("252");
        VARIABLE_LENGTH_AIS.add("253");
        VARIABLE_LENGTH_AIS.add("254");
    }

    private GS1Parser() {
    }

    public static List<GS1Element> parseGs1(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        String normalized = stripPrefix(raw).replace("\r", "").replace("\n", "");
        List<GS1Element> elements = new ArrayList<>();
        int index = 0;
        while (index < normalized.length()) {
            char current = normalized.charAt(index);
            if (current == '\u001D') {
                index++;
                continue;
            }
            if (!Character.isDigit(current)) {
                throw new IllegalArgumentException("Unexpected character '" + current + "' at position " + index);
            }
            String ai = null;
            int aiLength = 0;
            for (int candidateLength = Math.min(4, normalized.length() - index); candidateLength >= 2; candidateLength--) {
                String candidate = normalized.substring(index, index + candidateLength);
                if (isKnownAi(candidate)) {
                    ai = candidate;
                    aiLength = candidateLength;
                    break;
                }
            }
            if (ai == null) {
                throw new IllegalArgumentException("Unknown AI at position " + index);
            }
            index += aiLength;
            if (isFixedLengthAi(ai)) {
                int valueLength = getFixedLength(ai);
                if (index + valueLength > normalized.length()) {
                    throw new IllegalArgumentException("Value for AI " + ai + " is shorter than expected");
                }
                String value = normalized.substring(index, index + valueLength);
                elements.add(new GS1Element(ai, value));
                index += valueLength;
            } else {
                StringBuilder value = new StringBuilder();
                while (index < normalized.length() && normalized.charAt(index) != '\u001D') {
                    value.append(normalized.charAt(index));
                    index++;
                }
                if (value.length() == 0) {
                    throw new IllegalArgumentException("Value for AI " + ai + " cannot be empty");
                }
                elements.add(new GS1Element(ai, value.toString()));
            }
        }
        return elements;
    }

    public static String composeGs1(List<GS1Element> elements) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < elements.size(); i++) {
            GS1Element element = elements.get(i);
            builder.append(element.ai());
            builder.append(element.value());
            boolean needsGs = isVariableLengthAi(element.ai()) && i < elements.size() - 1;
            if (needsGs) {
                builder.append('\u001D');
            }
        }
        return builder.toString();
    }

    public static String withVisibleGs(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\u001D", "<GS>");
    }

    public static String stripPrefix(String raw) {
        if (raw.startsWith("]d2") || raw.startsWith("]D2")) {
            return raw.substring(3);
        }
        return raw;
    }

    private static boolean isKnownAi(String ai) {
        return isFixedLengthAi(ai) || isVariableLengthAi(ai);
    }

    private static boolean isFixedLengthAi(String ai) {
        if (FIXED_LENGTH_AIS.containsKey(ai)) {
            return true;
        }
        return ai.matches("31[0-9]{2}") || ai.matches("32[0-9]{2}") || ai.matches("33[0-9]{2}") || ai.matches("34[0-9]{2}");
    }

    private static boolean isVariableLengthAi(String ai) {
        if (VARIABLE_LENGTH_AIS.contains(ai)) {
            return true;
        }
        if (ai.length() == 3 && ai.chars().allMatch(Character::isDigit)) {
            int value = Integer.parseInt(ai);
            if ((value >= 240 && value <= 243) || (value >= 250 && value <= 254) || (value >= 400 && value <= 426)) {
                return true;
            }
        }
        return false;
    }

    private static int getFixedLength(String ai) {
        Integer length = FIXED_LENGTH_AIS.get(ai);
        if (length != null) {
            return length;
        }
        if (ai.matches("3[1-4][0-9]{2}")) {
            return 6;
        }
        throw new IllegalArgumentException("AI " + ai + " is not fixed-length");
    }
}
