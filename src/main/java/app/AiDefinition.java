package app;

public final class AiDefinition {
    private final String code;
    private final String description;
    private final boolean fixedLength;
    private final int minLength;
    private final int maxLength;
    private final CharacterSet characterSet;

    public AiDefinition(String code, String description, int length, CharacterSet characterSet) {
        this(code, description, true, length, length, characterSet);
    }

    public AiDefinition(String code, String description, int minLength, int maxLength, CharacterSet characterSet) {
        this(code, description, false, minLength, maxLength, characterSet);
    }

    private AiDefinition(String code, String description, boolean fixedLength, int minLength, int maxLength, CharacterSet characterSet) {
        this.code = code;
        this.description = description;
        this.fixedLength = fixedLength;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.characterSet = characterSet;
    }

    public String code() {
        return code;
    }

    public String description() {
        return description;
    }

    public boolean fixedLength() {
        return fixedLength;
    }

    public int minLength() {
        return minLength;
    }

    public int maxLength() {
        return maxLength;
    }

    public CharacterSet characterSet() {
        return characterSet;
    }
}
