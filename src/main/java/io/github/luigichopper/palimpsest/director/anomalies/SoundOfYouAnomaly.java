package io.github.luigichopper.palimpsest.director.anomalies;

import io.github.luigichopper.palimpsest.director.Anomaly;
import io.github.luigichopper.palimpsest.director.AnomalyContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public class SoundOfYouAnomaly implements Anomaly {
    @Override
    public String id() {
        return "sound_of_you";
    }

    @Override
    public int tier() {
        return 2;
    }

    @Override
    public float cost() {
        return 25.0f;
    }

    @Override
    public long cooldownSeconds() {
        return 300;
    }

    @Override
    public boolean canFire(AnomalyContext ctx) {
        return ctx.tracker().getLastPlayerSound() != null;
    }

    @Override
    public boolean fire(AnomalyContext ctx) {
        ServerPlayer player = ctx.player();
        ServerLevel level = ctx.level();
        SoundEvent sound = ctx.tracker().getLastPlayerSound();
        if (sound == null) {
            sound = SoundEvents.WOOD_BREAK;
        }

        // Calculate a location 10 blocks away at an angle
        Vec3 look = player.getLookAngle();
        double angle = ctx.random().nextBoolean() ? Math.PI / 2 : -Math.PI / 2;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double offsetX = (look.x * cos - look.z * sin) * 10.0;
        double offsetZ = (look.x * sin + look.z * cos) * 10.0;

        Vec3 soundPos = player.position().add(offsetX, 0, offsetZ);

        level.playSound(
            null,
            soundPos.x,
            soundPos.y,
            soundPos.z,
            sound,
            SoundSource.BLOCKS,
            0.6f,
            0.9f + ctx.random().nextFloat() * 0.2f
        );

        return true;
    }
}
