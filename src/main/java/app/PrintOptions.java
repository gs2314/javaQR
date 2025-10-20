package app;

public record PrintOptions(double sizeMillimetres, int dpi, int copies, boolean includeHri, String title, String hriText) {
}
