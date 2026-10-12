package io.github.luigichopper.palimpsest.reflection;

public record ReplayMode(
    Type type,
    int parameter
) {
    public enum Type {
        FAITHFUL,
        DELAYED,
        DIVERGE
    }

    public static ReplayMode faithful() {
        return new ReplayMode(Type.FAITHFUL, 0);
    }

    public static ReplayMode delayed(int delayTicks) {
        return new ReplayMode(Type.DELAYED, Math.max(1, delayTicks));
    }

    public static ReplayMode diverge(int divergeTick) {
        return new ReplayMode(Type.DIVERGE, Math.max(0, divergeTick));
    }
}
