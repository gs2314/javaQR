package app;

public final class ParseOptions {
    private final boolean allowLowercase;
    private final boolean heuristicRepair;

    private ParseOptions(Builder builder) {
        this.allowLowercase = builder.allowLowercase;
        this.heuristicRepair = builder.heuristicRepair;
    }

    public boolean allowLowercase() {
        return allowLowercase;
    }

    public boolean heuristicRepair() {
        return heuristicRepair;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ParseOptions defaults() {
        return builder().allowLowercase(false).heuristicRepair(false).build();
    }

    public static final class Builder {
        private boolean allowLowercase;
        private boolean heuristicRepair;

        private Builder() {
        }

        public Builder allowLowercase(boolean allowLowercase) {
            this.allowLowercase = allowLowercase;
            return this;
        }

        public Builder heuristicRepair(boolean heuristicRepair) {
            this.heuristicRepair = heuristicRepair;
            return this;
        }

        public ParseOptions build() {
            return new ParseOptions(this);
        }
    }
}
