package io.github.luigichopper.palimpsest.level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record LevelRegistryEntry(
    Identifier id,
    LevelKind kind,
    Identifier dimensionTypeId,
    long seed,
    String difficulty,
    boolean mirrorClocks,
    String generatorKind
) {
    public static final Codec<LevelRegistryEntry> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(LevelRegistryEntry::id),
            LevelKind.CODEC.fieldOf("kind").forGetter(LevelRegistryEntry::kind),
            Identifier.CODEC.fieldOf("dimension_type").forGetter(LevelRegistryEntry::dimensionTypeId),
            Codec.LONG.fieldOf("seed").forGetter(LevelRegistryEntry::seed),
            Codec.STRING.fieldOf("difficulty").forGetter(LevelRegistryEntry::difficulty),
            Codec.BOOL.fieldOf("mirror_clocks").forGetter(LevelRegistryEntry::mirrorClocks),
            Codec.STRING.fieldOf("generator_kind").forGetter(LevelRegistryEntry::generatorKind)
        ).apply(instance, LevelRegistryEntry::new)
    );
}
