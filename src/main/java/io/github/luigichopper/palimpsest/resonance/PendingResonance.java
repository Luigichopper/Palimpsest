package io.github.luigichopper.palimpsest.resonance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

public record PendingResonance(
    Identifier dimensionId,
    BlockPos pos,
    BlockState state,
    long executeGameTime
) {
    public static final Codec<PendingResonance> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Identifier.CODEC.fieldOf("dimension").forGetter(PendingResonance::dimensionId),
            BlockPos.CODEC.fieldOf("pos").forGetter(PendingResonance::pos),
            BlockState.CODEC.fieldOf("state").forGetter(PendingResonance::state),
            Codec.LONG.fieldOf("execute_tick").forGetter(PendingResonance::executeGameTime)
        ).apply(instance, PendingResonance::new)
    );
}
