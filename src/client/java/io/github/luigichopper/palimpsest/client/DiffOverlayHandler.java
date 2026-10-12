package io.github.luigichopper.palimpsest.client;

import io.github.luigichopper.palimpsest.diff.DiffEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

import java.util.ArrayList;
import java.util.List;

public class DiffOverlayHandler {
    private static final List<DiffEntry> ACTIVE_DIFFS = new ArrayList<>();
    private static int remainingTicks = 0;

    public static void setDiffs(List<DiffEntry> diffs) {
        ACTIVE_DIFFS.clear();
        ACTIVE_DIFFS.addAll(diffs);
        remainingTicks = 300; // 15 seconds
    }

    public static void tick(Minecraft client) {
        if (remainingTicks <= 0 || ACTIVE_DIFFS.isEmpty()) {
            return;
        }

        remainingTicks--;
        ClientLevel level = client.level;
        LocalPlayer player = client.player;
        if (level == null || player == null) {
            return;
        }

        // Emit particles around diff blocks every 4 ticks
        if (remainingTicks % 4 != 0) {
            return;
        }

        BlockPos playerPos = player.blockPosition();

        for (DiffEntry entry : ACTIVE_DIFFS) {
            BlockPos pos = entry.pos();
            if (pos.distSqr(playerPos) > 48 * 48) {
                continue;
            }

            ParticleOptions particle;
            switch (entry.category()) {
                case ADDED -> particle = ParticleTypes.HAPPY_VILLAGER;
                case REMOVED -> particle = ParticleTypes.FLAME;
                case CHANGED -> particle = ParticleTypes.ELECTRIC_SPARK;
                default -> particle = ParticleTypes.GLOW;
            }

            double px = pos.getX() + 0.5;
            double py = pos.getY() + 0.5;
            double pz = pos.getZ() + 0.5;

            level.addParticle(particle, px, py + 0.5, pz, 0.0, 0.02, 0.0);
            level.addParticle(particle, px + 0.4, py, pz, 0.0, 0.01, 0.0);
            level.addParticle(particle, px - 0.4, py, pz, 0.0, 0.01, 0.0);
            level.addParticle(particle, px, py, pz + 0.4, 0.0, 0.01, 0.0);
            level.addParticle(particle, px, py, pz - 0.4, 0.0, 0.01, 0.0);
        }
    }
}
