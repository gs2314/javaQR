package app;

public enum CharacterSet {
    NUMERIC,
    GS1_ALPHANUMERIC;

    public boolean isAllowed(char c, boolean allowLowercase) {
        return switch (this) {
            case NUMERIC -> Character.isDigit(c);
            case GS1_ALPHANUMERIC -> isGs1Alphanumeric(c, allowLowercase);
        };
    }

    private boolean isGs1Alphanumeric(char c, boolean allowLowercase) {
        if (Character.isDigit(c)) {
            return true;
        }
        if (Character.isUpperCase(c)) {
            return true;
        }
        if (allowLowercase && Character.isLowerCase(c)) {
            return true;
        }
        return " !\"%&'()*+,-./:;<=>?_".indexOf(c) >= 0;
    }
}
