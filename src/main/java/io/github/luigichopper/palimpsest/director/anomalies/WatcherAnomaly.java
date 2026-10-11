package io.github.luigichopper.palimpsest.director.anomalies;

import io.github.luigichopper.palimpsest.core.TaskScheduler;
import io.github.luigichopper.palimpsest.director.Anomaly;
import io.github.luigichopper.palimpsest.director.AnomalyContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class WatcherAnomaly implements Anomaly {
    @Override
    public String id() {
        return "watcher";
    }

    @Override
    public int tier() {
        return 2;
    }

    @Override
    public float cost() {
        return 35.0f;
    }

    @Override
    public long cooldownSeconds() {
        return 400;
    }

    @Override
    public boolean canFire(AnomalyContext ctx) {
        return ctx.player().level() instanceof ServerLevel;
    }

    @Override
    public boolean fire(AnomalyContext ctx) {
        ServerPlayer player = ctx.player();
        ServerLevel level = ctx.level();

        Vec3 look = player.getLookAngle();
        // Place watcher roughly 26 blocks forward-left or forward-right
        double angleOffset = (ctx.random().nextBoolean() ? 0.35 : -0.35) * Math.PI;
        double cos = Math.cos(angleOffset);
        double sin = Math.sin(angleOffset);
        double dirX = look.x * cos - look.z * sin;
        double dirZ = look.x * sin + look.z * cos;

        int targetX = (int) (player.getX() + dirX * 26.0);
        int targetZ = (int) (player.getZ() + dirZ * 26.0);
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(targetX, 0, targetZ));

        Mannequin watcher = new Mannequin(EntityTypes.MANNEQUIN, level);
        watcher.setPos(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5);
        watcher.setCustomName(Component.literal(" "));
        watcher.setCustomNameVisible(false);
        watcher.setPermanentlyInvulnerable(true);

        // Face the player
        double dx = player.getX() - watcher.getX();
        double dz = player.getZ() - watcher.getZ();
        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        watcher.setYRot(yaw);
        watcher.setYHeadRot(yaw);

        level.addFreshEntity(watcher);

        // Schedule disappearance logic
        TaskScheduler.get().submit(new io.github.luigichopper.palimpsest.core.ScheduledTask() {
            int ticksLived = 0;
            int eyeContactTicks = 0;
            boolean cancelled = false;

            @Override
            public String name() {
                return "watcher_behavior";
            }

            @Override
            public int priority() {
                return 5;
            }

            @Override
            public boolean tick(net.minecraft.server.MinecraftServer server) {
                if (cancelled) return true;
                ticksLived += 4;
                if (!watcher.isAlive()) {
                    return true;
                }

                if (ticksLived >= 600) { // 30 seconds
                    vanish();
                    return true;
                }

                // Check distance
                double distSq = watcher.distanceToSqr(player);
                if (distSq < 12.0 * 12.0) {
                    vanish();
                    return true;
                }

                // Check eye contact
                Vec3 playerLook = player.getLookAngle().normalize();
                Vec3 toWatcher = watcher.position().subtract(player.getEyePosition()).normalize();
                double dot = playerLook.dot(toWatcher);
                if (dot > 0.97) {
                    eyeContactTicks += 4;
                    if (eyeContactTicks >= 16) { // ~0.8s of sustained eye contact
                        vanish();
                        return true;
                    }
                } else {
                    eyeContactTicks = Math.max(0, eyeContactTicks - 2);
                }

                return false;
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
                return Math.min(1.0f, (float) ticksLived / 600.0f);
            }

            private void vanish() {
                level.sendParticles(
                    ParticleTypes.POOF,
                    watcher.getX(),
                    watcher.getY() + 1.0,
                    watcher.getZ(),
                    15,
                    0.2, 0.4, 0.2,
                    0.02
                );
                level.playSound(null, watcher.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 0.35f, 1.4f);
                watcher.discard();
            }
        });

        return true;
    }
}
