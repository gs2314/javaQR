package app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ParseResult {
    private final List<ParsedElement> elements = new ArrayList<>();
    private final List<ParseMessage> errors = new ArrayList<>();
    private final List<ParseMessage> warnings = new ArrayList<>();
    private boolean heuristicsApplied;

    public void addElement(ParsedElement element) {
        elements.add(element);
    }

    public void addError(ParseMessage message) {
        errors.add(message);
    }

    public void addWarning(ParseMessage message) {
        warnings.add(message);
    }

    public List<ParsedElement> elements() {
        return Collections.unmodifiableList(elements);
    }

    public List<ParseMessage> errors() {
        return Collections.unmodifiableList(errors);
    }

    public List<ParseMessage> warnings() {
        return Collections.unmodifiableList(warnings);
    }

    public boolean success() {
        return errors.isEmpty();
    }

    public void setHeuristicsApplied() {
        this.heuristicsApplied = true;
    }

    public boolean heuristicsApplied() {
        return heuristicsApplied;
    }
}
