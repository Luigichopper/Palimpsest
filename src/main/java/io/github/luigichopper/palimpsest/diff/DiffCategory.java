package io.github.luigichopper.palimpsest.diff;

public enum DiffCategory {
    ADDED((byte) 0),
    REMOVED((byte) 1),
    CHANGED((byte) 2);

    private final byte id;

    DiffCategory(byte id) {
        this.id = id;
    }

    public byte getId() {
        return id;
    }

    public static DiffCategory fromId(byte id) {
        return switch (id) {
            case 0 -> ADDED;
            case 1 -> REMOVED;
            default -> CHANGED;
        };
    }
}
