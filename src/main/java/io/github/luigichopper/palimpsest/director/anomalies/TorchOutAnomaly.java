package io.github.luigichopper.palimpsest.director.anomalies;

import io.github.luigichopper.palimpsest.director.Anomaly;
import io.github.luigichopper.palimpsest.director.AnomalyContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class TorchOutAnomaly implements Anomaly {
    @Override
    public String id() {
        return "torch_out";
    }

    @Override
    public int tier() {
        return 1;
    }

    @Override
    public float cost() {
        return 18.0f;
    }

    @Override
    public long cooldownSeconds() {
        return 240;
    }

    @Override
    public boolean canFire(AnomalyContext ctx) {
        return findNearbyTorch(ctx.level(), ctx.player().blockPosition()) != null;
    }

    @Override
    public boolean fire(AnomalyContext ctx) {
        ServerLevel level = ctx.level();
        BlockPos torchPos = findNearbyTorch(level, ctx.player().blockPosition());
        if (torchPos == null) {
            return false;
        }

        level.removeBlock(torchPos, false);
        level.sendParticles(
            ParticleTypes.SMOKE,
            torchPos.getX() + 0.5,
            torchPos.getY() + 0.5,
            torchPos.getZ() + 0.5,
            10,
            0.1, 0.1, 0.1,
            0.02
        );
        level.playSound(null, torchPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4f, 1.3f);
        return true;
    }

    private BlockPos findNearbyTorch(ServerLevel level, BlockPos origin) {
        int radius = 8;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 3; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos p = origin.offset(x, y, z);
                    BlockState state = level.getBlockState(p);
                    if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)) {
                        return p;
                    }
                }
            }
        }
        return null;
    }
}
