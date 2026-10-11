package io.github.luigichopper.palimpsest.level;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum LevelKind implements StringRepresentable {
    ANNEX("annex", false),
    ECHO("echo", false),
    MIRROR("mirror", true),
    BACKUP("backup", true);

    public static final Codec<LevelKind> CODEC = StringRepresentable.fromEnum(LevelKind::values);

    private final String name;
    private final boolean persistent;

    LevelKind(String name, boolean persistent) {
        this.name = name;
        this.persistent = persistent;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public boolean isPersistent() {
        return this.persistent;
    }
}
