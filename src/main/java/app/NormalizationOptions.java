package app;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link Normalizer} behaviour.
 */
public final class NormalizationOptions {
    private final Set<String> gsPlaceholders;
    private final boolean stripAimId;
    private final boolean collapseMultipleGs;

    private NormalizationOptions(Builder builder) {
        this.gsPlaceholders = Set.copyOf(builder.gsPlaceholders);
        this.stripAimId = builder.stripAimId;
        this.collapseMultipleGs = builder.collapseMultipleGs;
    }

    public Set<String> gsPlaceholders() {
        return gsPlaceholders;
    }

    public boolean stripAimId() {
        return stripAimId;
    }

    public boolean collapseMultipleGs() {
        return collapseMultipleGs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder defaultsBuilder() {
        return builder()
                .stripAimId(true)
                .collapseMultipleGs(false)
                .gsPlaceholders(DefaultPlaceholders.VALUES);
    }

    public static NormalizationOptions defaults() {
        return defaultsBuilder().build();
    }

    public static final class Builder {
        private final LinkedHashSet<String> gsPlaceholders = new LinkedHashSet<>();
        private boolean stripAimId;
        private boolean collapseMultipleGs;

        private Builder() {
        }

        public Builder gsPlaceholders(Set<String> placeholders) {
            Objects.requireNonNull(placeholders, "placeholders");
            this.gsPlaceholders.clear();
            this.gsPlaceholders.addAll(placeholders);
            return this;
        }

        public Builder addGsPlaceholders(String... placeholders) {
            this.gsPlaceholders.addAll(Arrays.asList(placeholders));
            return this;
        }

        public Builder stripAimId(boolean stripAimId) {
            this.stripAimId = stripAimId;
            return this;
        }

        public Builder collapseMultipleGs(boolean collapseMultipleGs) {
            this.collapseMultipleGs = collapseMultipleGs;
            return this;
        }

        public NormalizationOptions build() {
            return new NormalizationOptions(this);
        }
    }

    /**
     * Default placeholder definitions.
     */
    public static final class DefaultPlaceholders {
        public static final Set<String> VALUES = Set.of(
                "<GS>",
                "[GS]",
                "{GS}",
                "(GS)",
                "\\u001D",
                "\\x1D",
                "\\035",
                "&#29;",
                "%1D"
        );

        private DefaultPlaceholders() {
        }
    }
}
