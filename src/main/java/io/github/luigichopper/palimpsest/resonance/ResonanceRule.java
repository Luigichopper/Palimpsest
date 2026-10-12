package io.github.luigichopper.palimpsest.resonance;

public record ResonanceRule(
    String type,
    String from,
    String to,
    int minTicks,
    int maxTicks
) {
    public boolean isInvert() {
        return "INVERT".equalsIgnoreCase(type);
    }

    public boolean isDelay() {
        return "DELAY".equalsIgnoreCase(type);
    }
}
