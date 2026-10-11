package io.github.luigichopper.palimpsest.director.anomalies;

import io.github.luigichopper.palimpsest.config.PalimpsestConfig;
import io.github.luigichopper.palimpsest.director.Anomaly;
import io.github.luigichopper.palimpsest.director.AnomalyContext;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class WrongHourAnomaly implements Anomaly {
    @Override
    public String id() {
        return "wrong_hour";
    }

    @Override
    public int tier() {
        return 2;
    }

    @Override
    public float cost() {
        return 30.0f;
    }

    @Override
    public long cooldownSeconds() {
        return 360;
    }

    @Override
    public boolean canFire(AnomalyContext ctx) {
        return true;
    }

    @Override
    public boolean fire(AnomalyContext ctx) {
        ServerPlayer player = ctx.player();
        ServerLevel level = ctx.level();
        boolean reduceFlashes = PalimpsestConfig.get().accessibility.reduceFlashes;

        // Play gentle, unsettling ambient audio
        level.playSound(
            null,
            player.blockPosition(),
            SoundEvents.AMBIENT_CAVE.value(),
            SoundSource.AMBIENT,
            0.5f,
            0.75f
        );

        if (!reduceFlashes) {
            // Particle visual cue if flashes/strobing aren't restricted
            level.sendParticles(
                ParticleTypes.WARPED_SPORE,
                player.getX(),
                player.getY() + 2.0,
                player.getZ(),
                25,
                4.0, 2.0, 4.0,
                0.01
            );
        } else {
            // Subtle gentle ash drift respecting reduceFlashes
            level.sendParticles(
                ParticleTypes.WHITE_ASH,
                player.getX(),
                player.getY() + 2.0,
                player.getZ(),
                12,
                3.0, 1.5, 3.0,
                0.005
            );
        }

        return true;
    }
}
