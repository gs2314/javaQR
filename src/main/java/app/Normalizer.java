package app;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class that normalizes raw scan input prior to parsing.
 */
public final class Normalizer {
    private static final Pattern AIM_PREFIX_PATTERN = Pattern.compile("^](\\w.)");
    private static final char GS = 0x1D;
    private static final Pattern OCTAL_PATTERN = Pattern.compile("\\\\0*(3[0-7]{2})");

    private Normalizer() {
    }

    public static NormalizationResult normalizeRaw(String raw, NormalizationOptions options) {
        Objects.requireNonNull(raw, "raw");
        Objects.requireNonNull(options, "options");

        String working = raw;
        List<String> warnings = new ArrayList<>();
        String symbologyId = null;

        Matcher aimMatcher = AIM_PREFIX_PATTERN.matcher(working);
        if (aimMatcher.find()) {
            symbologyId = "]" + aimMatcher.group(1);
            if (options.stripAimId()) {
                working = working.substring(aimMatcher.end());
                warnings.add("Removed AIM Symbology ID " + symbologyId);
            }
        }

        working = working.replace('\u00A0', ' ');
        working = stripControlSuffixes(working);
        working = working.replace("\t", "");
        working = replaceConfiguredPlaceholders(working, options.gsPlaceholders());
        working = replaceBuiltInPlaceholders(working);
        working = trimAsciiSpaces(working);

        if (options.collapseMultipleGs() && working.indexOf(GS) >= 0) {
            String collapsed = working.replaceAll("" + GS + "+", String.valueOf(GS));
            if (!collapsed.equals(working)) {
                warnings.add("Collapsed repeated GS characters");
                working = collapsed;
            }
        }

        while (!working.isEmpty() && working.charAt(0) == GS) {
            working = working.substring(1);
        }

        return new NormalizationResult(raw, working, symbologyId, warnings);
    }

    private static String trimAsciiSpaces(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isTrimChar(value.charAt(start))) {
            start++;
        }
        while (end > start && isTrimChar(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(start, end);
    }

    private static boolean isTrimChar(char c) {
        return c == ' ' || c == '\u00A0' || c == '\r' || c == '\n';
    }

    private static String stripControlSuffixes(String value) {
        int end = value.length();
        while (end > 0) {
            char c = value.charAt(end - 1);
            if (c == '\r' || c == '\n' || c == 0x00 || c == 0x03) {
                end--;
            } else {
                break;
            }
        }
        return value.substring(0, end);
    }

    private static String replaceConfiguredPlaceholders(String value, Set<String> placeholders) {
        String replaced = value;
        for (String placeholder : placeholders) {
            if (placeholder == null || placeholder.isBlank()) {
                continue;
            }
            String literal = Pattern.quote(placeholder);
            Pattern pattern = Pattern.compile(literal, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            Matcher matcher = pattern.matcher(replaced);
            if (matcher.find()) {
                replaced = matcher.replaceAll(String.valueOf(GS));
            }
        }
        return replaced;
    }

    private static String replaceBuiltInPlaceholders(String value) {
        String replaced = value;
        replaced = replaced.replace("\\u001d", String.valueOf(GS));
        replaced = replaced.replace("\\u001D", String.valueOf(GS));
        replaced = replaced.replace("\\x1d", String.valueOf(GS));
        replaced = replaced.replace("\\x1D", String.valueOf(GS));
        replaced = replaced.replace("%1d", String.valueOf(GS));
        replaced = replaced.replace("%1D", String.valueOf(GS));
        replaced = replaceOctalPlaceholders(replaced);
        return replaced;
    }

    private static String replaceOctalPlaceholders(String value) {
        Matcher matcher = OCTAL_PATTERN.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String octal = matcher.group(1);
            int parsed = Integer.parseInt(octal, 8);
            if (parsed == GS) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(String.valueOf(GS)));
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
