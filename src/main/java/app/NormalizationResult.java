package app;

import java.util.List;

public record NormalizationResult(String raw,
                                  String normalized,
                                  String symbologyId,
                                  List<String> warnings) {
    public NormalizationResult {
        warnings = List.copyOf(warnings);
    }
}
