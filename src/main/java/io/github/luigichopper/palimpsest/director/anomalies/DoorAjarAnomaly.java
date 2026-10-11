package io.github.luigichopper.palimpsest.director.anomalies;

import io.github.luigichopper.palimpsest.director.Anomaly;
import io.github.luigichopper.palimpsest.director.AnomalyContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;

public class DoorAjarAnomaly implements Anomaly {
    @Override
    public String id() {
        return "door_ajar";
    }

    @Override
    public int tier() {
        return 1;
    }

    @Override
    public float cost() {
        return 15.0f;
    }

    @Override
    public long cooldownSeconds() {
        return 180;
    }

    @Override
    public boolean canFire(AnomalyContext ctx) {
        BlockPos playerPos = ctx.player().blockPosition();
        ServerLevel level = ctx.level();
        return findClosedDoor(level, playerPos) != null;
    }

    @Override
    public boolean fire(AnomalyContext ctx) {
        BlockPos playerPos = ctx.player().blockPosition();
        ServerLevel level = ctx.level();
        BlockPos doorPos = findClosedDoor(level, playerPos);
        if (doorPos == null) {
            return false;
        }

        BlockState state = level.getBlockState(doorPos);
        if (state.getBlock() instanceof DoorBlock doorBlock) {
            doorBlock.setOpen(null, level, state, doorPos, true);
            level.playSound(null, doorPos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 0.35f, 0.8f);
            return true;
        }
        return false;
    }

    private BlockPos findClosedDoor(ServerLevel level, BlockPos origin) {
        int radius = 8;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 4; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos p = origin.offset(x, y, z);
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof DoorBlock) {
                        if (!s.getValue(DoorBlock.OPEN)) {
                            return p;
                        }
                    }
                }
            }
        }
        return null;
    }
}
