package io.github.luigichopper.palimpsest.director.anomalies;

import io.github.luigichopper.palimpsest.core.TaskScheduler;
import io.github.luigichopper.palimpsest.director.Anomaly;
import io.github.luigichopper.palimpsest.director.AnomalyContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class FootstepsBehindAnomaly implements Anomaly {
    @Override
    public String id() {
        return "footsteps_behind";
    }

    @Override
    public int tier() {
        return 1;
    }

    @Override
    public float cost() {
        return 20.0f;
    }

    @Override
    public long cooldownSeconds() {
        return 200;
    }

    @Override
    public boolean canFire(AnomalyContext ctx) {
        return ctx.player().onGround();
    }

    @Override
    public boolean fire(AnomalyContext ctx) {
        ServerPlayer player = ctx.player();
        ServerLevel level = ctx.level();

        Vec3 initialLook = player.getLookAngle();
        Vec3 initialPos = player.position();

        // Position 3.5 blocks behind the player
        Vec3 behind = initialPos.subtract(initialLook.x * 3.5, 0, initialLook.z * 3.5);
        BlockPos groundPos = BlockPos.containing(behind).below();
        BlockState groundState = level.getBlockState(groundPos);
        SoundEvent stepSound = groundState.getSoundType().getStepSound();
        if (stepSound == null) {
            stepSound = SoundEvents.WOOD_STEP;
        }

        // First footstep
        level.playSound(null, behind.x, behind.y, behind.z, stepSound, SoundSource.PLAYERS, 0.45f, 0.9f);

        // Schedule next footsteps that abort if player turns
        final SoundEvent finalStepSound = stepSound;
        TaskScheduler.get().submit(new io.github.luigichopper.palimpsest.core.ScheduledTask() {
            int step = 1;
            int ticksUntilNext = 8;
            boolean cancelled = false;

            @Override
            public String name() {
                return "footsteps_behind_sequence";
            }

            @Override
            public int priority() {
                return 10;
            }

            @Override
            public boolean tick(net.minecraft.server.MinecraftServer server) {
                if (cancelled) return true;
                if (ticksUntilNext > 0) {
                    ticksUntilNext--;
                    return false;
                }

                // Check if player turned (> 40 degrees difference)
                Vec3 currentLook = player.getLookAngle();
                double dot = initialLook.x * currentLook.x + initialLook.z * currentLook.z;
                if (dot < 0.75) {
                    // Player turned! Stop sequence immediately
                    return true;
                }

                Vec3 nextBehind = player.position().subtract(currentLook.x * (3.5 - step * 0.5), 0, currentLook.z * (3.5 - step * 0.5));
                level.playSound(null, nextBehind.x, nextBehind.y, nextBehind.z, finalStepSound, SoundSource.PLAYERS, 0.5f, 0.95f);

                step++;
                ticksUntilNext = 8;
                return step >= 3;
            }

            @Override
            public void cancel() {
                this.cancelled = true;
            }

            @Override
            public boolean isCancelled() {
                return this.cancelled;
            }

            @Override
            public float getProgress() {
                return (float) step / 3.0f;
            }
        });

        return true;
    }
}
