package io.github.luigichopper.palimpsest.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public record AnchorPos(
    Identifier dimensionId,
    double x,
    double y,
    double z,
    float yaw,
    float pitch
) {
    public static final Codec<AnchorPos> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Identifier.CODEC.fieldOf("dimension").forGetter(AnchorPos::dimensionId),
            Codec.DOUBLE.fieldOf("x").forGetter(AnchorPos::x),
            Codec.DOUBLE.fieldOf("y").forGetter(AnchorPos::y),
            Codec.DOUBLE.fieldOf("z").forGetter(AnchorPos::z),
            Codec.FLOAT.fieldOf("yaw").forGetter(AnchorPos::yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(AnchorPos::pitch)
        ).apply(instance, AnchorPos::new)
    );

    public Vec3 pos() {
        return new Vec3(x, y, z);
    }
}
