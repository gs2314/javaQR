package app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ParsedElement {
    private final GS1Element element;
    private final AiDefinition definition;
    private final int valueStart;
    private final int valueEnd;
    private final List<String> warnings = new ArrayList<>();
    private boolean valid = true;

    public ParsedElement(GS1Element element, AiDefinition definition, int valueStart, int valueEnd) {
        this.element = element;
        this.definition = definition;
        this.valueStart = valueStart;
        this.valueEnd = valueEnd;
    }

    public GS1Element element() {
        return element;
    }

    public AiDefinition definition() {
        return definition;
    }

    public int valueStart() {
        return valueStart;
    }

    public int valueEnd() {
        return valueEnd;
    }

    public List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }

    public void addWarning(String warning) {
        if (warning != null && !warning.isBlank()) {
            warnings.add(warning);
        }
    }

    public void markInvalid() {
        this.valid = false;
    }

    public boolean valid() {
        return valid;
    }
}
